package org.example.orbit.Service;

import lombok.NoArgsConstructor;
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
            //это парсинг данных
            TLE tle = new TLE(sat.getTleLine1(), sat.getTleLine2());
            TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
            /// Точка наблюдения на поверхности земли
            GeodeticPoint geo = new GeodeticPoint(
                    Math.toRadians(lat), Math.toRadians(lon), 0.0);
            //это хуйня кароче как наш взгляд чтобы код мог считать угол возвышения аля на сколько сильно он высоко от нас
            TopocentricFrame topoFrame = new TopocentricFrame(getEarth(), geo, "observer");
            /// временной интервал поиска
            AbsoluteDate startDate = new AbsoluteDate(Date.from(Instant.now()),
                    TimeScalesFactory.getUTC());
            AbsoluteDate endDate = startDate.shiftedBy(hoursAhead * 3600.0);
            /// Минимальный угол возвышения для видимостти 10 секундд
            double minElevation = Math.toRadians(10.0);
            /// шаг сканирования 30 секунд
            double stepSec = 30.0;
            //это кароче переменные состояния
            boolean inPass = false;
            AbsoluteDate riseTime = null;
            double maxElevation = 0;
            AbsoluteDate maxElevTime = null;
            //тут начинается цикл который идет по времени каждые 30 секунд
            AbsoluteDate current = startDate;
            while (current.compareTo(endDate) <= 0) {
                // этот спейс спрашивает SGP4 где спутник в по current
                SpacecraftState state = propagator.propagate(current);
                //хуйня берет позицию (позиция + скорость) , берем только позицию
                Vector3D satPos = state.getPVCoordinates(getEarthFrame()).getPosition();
                //считает угол возвышения
                double elevation = topoFrame.getElevation(satPos, getEarthFrame(), current);
                //тут начинается логика трех состояний, сейчас когда мы не в пролете спутника
                if (!inPass && elevation >= minElevation) {
                    /// Спутник появился над горизонтом!11
                    inPass = true;
                    riseTime = current;
                    maxElevation = elevation;
                    maxElevTime = current;
                    //тут спутник появился над горизонтов
                } else if (inPass && elevation >= minElevation) {
                    /// Отслеживаем максимум
                    if (elevation > maxElevation) {
                        maxElevation = elevation;
                        maxElevTime = current;
                    }
                    //тут спутник ушел за горизонт
                } else if (inPass && elevation < minElevation) {
                    /// Спутник зашел за горизонт
                    inPass = false;
                    AbsoluteDate setTime = current;
                    double durationSec = setTime.durationFrom(riseTime);
                    //тут сохраняем пролет
                    passes.add(PassPredictionDto.builder()
                            .noradId(sat.getNoradId())
                            .name(sat.getName())
                            .riseTime(riseTime.toString())
                            .maxElevationTime(maxElevTime.toString())
                            .setTime(setTime.toString())
                            .maxEvelationDeg(Math.toDegrees(maxElevation))
                            .durationMinutes(durationSec / 60.0)
                            .build());
                }
                current = current.shiftedBy(stepSec);
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
                // Если база все еще огромная, для демо можно раскомментировать лимит:
                 .limit(100)
                .toList();

        long startTime = System.currentTimeMillis();

        //  Считаем параллельно
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
