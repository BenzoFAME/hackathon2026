package org.example.orbit.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;
import org.example.orbit.Model.Satellite;
import org.example.orbit.ModelDto.PassPredictionDto;
import org.example.orbit.repository.SatelliteRepository;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.springframework.stereotype.Service;
import java.util.Comparator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PassPredictionService {

    //создает систему координат
    private Frame earthFrame;
    //математическая модель формы земли о
    private BodyShape earth;

    private final SatelliteRepository satelliteRepository;

    private Frame getEarthFrame() {
        if (earthFrame == null) {
            earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        }
        return earthFrame;
    }

    private BodyShape getEarth() {
        if (earth == null) {
            earth = new OneAxisEllipsoid(
                    Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    getEarthFrame()
            );
        }
        return earth;
    }

    //lat and lon жто координаты наблюдате(там где мы находимся например)
    public List<PassPredictionDto> predict(Satellite sat, double lat,
                                           double lon, int hoursAhead) {
        List<PassPredictionDto> passes = new ArrayList<>();
        try {
            TLE tle = new TLE(sat.getTleLine1(), sat.getTleLine2());
            TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);

            GeodeticPoint geo = new GeodeticPoint(Math.toRadians(lat), Math.toRadians(lon), 0.0);
            TopocentricFrame topoFrame = new TopocentricFrame(getEarth(), geo, "observer");

            AbsoluteDate startDate = new AbsoluteDate(Date.from(Instant.now()), TimeScalesFactory.getUTC());
            AbsoluteDate endDate = startDate.shiftedBy(hoursAhead * 3600.0);

            double minElevation = Math.toRadians(10.0);
            double stepSec = 30.0;

            boolean inPass = false;
            AbsoluteDate riseTime = null;
            double maxElevation = 0;
            AbsoluteDate maxElevTime = null;

            AbsoluteDate current = startDate;
            while (current.compareTo(endDate) <= 0) {
                SpacecraftState state = propagator.propagate(current);
                Vector3D satPos = state.getPVCoordinates(getEarthFrame()).getPosition();
                double elevation = topoFrame.getElevation(satPos, getEarthFrame(), current);

                if (!inPass && elevation >= minElevation) {
                    // Спутник появился над горизонтом
                    inPass = true;
                    riseTime = current;
                    maxElevation = elevation;
                    maxElevTime = current;
                } else if (inPass && elevation >= minElevation) {
                    // Отслеживаем максимум
                    if (elevation > maxElevation) {
                        maxElevation = elevation;
                        maxElevTime = current;
                    }
                } else if (inPass && elevation < minElevation) {
                    // Спутник зашел за горизонт
                    inPass = false;
                    AbsoluteDate setTime = current;
                    double durationSec = setTime.durationFrom(riseTime);

                    passes.add(PassPredictionDto.builder()
                            .noradId(sat.getNoradId())
                            .name(sat.getName())
                            .riseTime(riseTime.toString())
                            .maxElevationTime(maxElevTime.toString())
                            .setTime(setTime.toString())
                            .maxElevationDeg(Math.toDegrees(maxElevation)) // Опечатка исправлена
                            .durationMinutes(durationSec / 60.0)
                            .build());
                }
                current = current.shiftedBy(stepSec);
            }

            if (inPass) {
                double durationSec = endDate.durationFrom(riseTime);
                passes.add(PassPredictionDto.builder()
                        .noradId(sat.getNoradId())
                        .name(sat.getName())
                        .riseTime(riseTime.toString())
                        .maxElevationTime(maxElevTime.toString())
                        .setTime(endDate.toString())
                        .maxElevationDeg(Math.toDegrees(maxElevation))
                        .durationMinutes(durationSec / 60.0)
                        .build());
            }

        }  catch (Exception e) {
            log.warn("Ошибка расчета пролетов для {}: {}", sat.getName(), e.getMessage());
            return new ArrayList<>();
        }
        return passes;
    }


    public List<PassPredictionDto> predictBatch(double lat, double lon, int hoursAhead) {
        //  Берем только LEO орбиту и ТОЛЬКО полезную нагрузку (игнорируем мусор)
        List<Satellite> leoSatellites = satelliteRepository.findByOrbitType(OrbitType.LEO_LOW_EARTH_ORBIT)
                .stream()
                .filter(sat -> sat.getObjectType() == ObjectType.PAYLOAD_PAYLOAD)
                 .limit(100)
                .toList();

        long startTime = System.currentTimeMillis();

        //  Считаем параллельно))
        List<PassPredictionDto> allPasses = leoSatellites.parallelStream()
                .map(sat -> predict(sat, lat, lon, hoursAhead))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(PassPredictionDto::getRiseTime)) // Сортируем по времени появления
                .limit(100)
                .toList();

        long endTime = System.currentTimeMillis();
        log.info("Рассчитано и отфильтровано {} ближайших пролетов над [{}, {}] за {} мс",
                allPasses.size(), lat, lon, (endTime - startTime));

        return allPasses;
    }
}
