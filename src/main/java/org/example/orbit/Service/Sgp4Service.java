package org.example.orbit.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class Sgp4Service {

    private Frame earthFrame;
    private BodyShape earth;

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

    public double[] getLatLonAlt(String line1, String line2, Instant instant) {
        try {
            TLE tle = new TLE(line1, line2);
            TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
            AbsoluteDate date = new AbsoluteDate(Date.from(instant), TimeScalesFactory.getUTC());
            SpacecraftState state = propagator.propagate(date);
            Vector3D position = state.getPVCoordinates(getEarthFrame()).getPosition();
            GeodeticPoint geo = getEarth().transform(position, getEarthFrame(), date);
            return new double[]{
                    Math.toDegrees(geo.getLatitude()),
                    Math.toDegrees(geo.getLongitude()),
                    geo.getAltitude() / 1000.0
            };
        } catch (Exception e) {
            log.error("Ошибка SGP4: {}", e.getMessage());
            throw new RuntimeException("Не удалось рассчитать позицию", e);
        }
    }

    public List<double[]> getOrbitTrack(String line1, String line2,
                                        Instant startTime, int periodMinutes, int stepSeconds) {
        List<double[]> track = new ArrayList<>();
        TLE tle = new TLE(line1, line2);
        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        Instant current = startTime;
        Instant end = startTime.plusSeconds((long) periodMinutes * 60);
        while (current.isBefore(end)) {
            AbsoluteDate date = new AbsoluteDate(Date.from(current), TimeScalesFactory.getUTC());
            try {
                SpacecraftState state = propagator.propagate(date);
                Vector3D position = state.getPVCoordinates(getEarthFrame()).getPosition();
                GeodeticPoint geo = getEarth().transform(position, getEarthFrame(), date);
                track.add(new double[]{
                        Math.toDegrees(geo.getLatitude()),
                        Math.toDegrees(geo.getLongitude()),
                        geo.getAltitude() / 1000.0
                });
            } catch (Exception e) {
                log.warn("Ошибка в точке трека: {}", e.getMessage());
            }
            current = current.plusSeconds(stepSeconds);
        }
        return track;
    }
}
