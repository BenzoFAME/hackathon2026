package org.example.orbit.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orbit.EnumsTags.OrbitType;
import org.example.orbit.Model.Satellite;
import org.example.orbit.repository.SatelliteRepository;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TleLoaderService {
    private final SatelliteRepository satelliteRepository;
    private final RestClient celestrakRestClient; // ← было WebClient

    public int loadActiveSatellites() {
        log.info("loading");
        String rawTle;
        try {
            rawTle = celestrakRestClient.get()
                    .uri("/NORAD/elements/gp.php?GROUP=active&FORMAT=tle")
                    .retrieve()
                    .body(String.class);
            log.info("complete {} ", rawTle.length());
        } catch (Exception e) {
            log.error("error {}", e.getMessage());
            throw new RuntimeException("error", e);
        }

        List<Satellite> satellites = parseTleText(rawTle);

        int saved = 0;
        for (Satellite sat : satellites) {
            try {
                satelliteRepository.findByNoradId(sat.getNoradId())
                        .ifPresentOrElse(
                                existing -> {
                                    existing.setTleLine1(sat.getTleLine1());
                                    existing.setTleLine2(sat.getTleLine2());
                                    existing.setEpochTime(sat.getEpochTime());
                                    existing.setMeanMotion(sat.getMeanMotion());
                                    satelliteRepository.save(existing);
                                },
                                () -> satelliteRepository.save(sat)
                        );
                saved++;
            }catch (Exception e){
                log.warn("error {}: {}" , sat.getNoradId(), e.getMessage());
            }
        }
        log.info("save/update {} satellites" , saved);
        return saved;
    }

    private List<Satellite> parseTleText(String rawText){
        List<Satellite> result = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return result;
        }
        String[] lines = rawText.split("\r?\n");
        for (int i = 0; i + 2 < lines.length; i+=3) {
            String name = lines[i].trim();
            String line1 = lines[i+1].trim();
            String line2 = lines[i+2].trim();
            if (!line1.startsWith("1 ") || !line2.startsWith("2 ")) {
                log.warn("Невалидные TLE строки для: {}", name);
                continue;
            }
            try {
                TLE tle = new TLE(line1, line2);
                Satellite sat =  Satellite.builder()
                        .name(name)
                        .noradId(tle.getSatelliteNumber())
                        .tleLine1(line1)
                        .tleLine2(line2)
                        .inclination(Math.toDegrees(tle.getI()))
                        .eccentricity(tle.getE())
                        .meanMotion(tle.getMeanMotion() * 86400/ (2*Math.PI))
                        .epochTime(toLocalDateTime(tle.getDate()))
                        .periodMinutes(computePeriod(tle.getMeanMotion()))
                        .apogeeKm(computeApogee(tle))
                        .perigeeKm(computePerigee(tle))
                        .orbitType(detectOrbitType(computeApogee(tle), computePerigee(tle), tle.getE()))
                        .build();
                result.add(sat);
            }catch (Exception e){
                log.warn("Ошибка парсинга TLE для {}: {}", name, e.getMessage());
            }
        }
        return result;
    }

    /// Период обращения в минутах из meanMotion
    private double computePeriod(double meanMotionRadPerSec){
        return (2 * Math.PI / meanMotionRadPerSec) / 60.0;
    }
    /// Высота апогея  a*(1+e) - R_earth, где a — большая полуось
    private double computeApogee(TLE tle){
        double mu = 398600.4418;
        double n = tle.getMeanMotion();
        double a = Math.cbrt(mu / (n*n));
        return a * (1 +tle.getE()) - 6371.0;
    }

    private double computePerigee(TLE tle) {
        double mu = 398600.4418;
        double n = tle.getMeanMotion();
        double a = Math.cbrt(mu / (n * n));
        return a * (1 - tle.getE()) - 6371.0;
    }

    private OrbitType detectOrbitType(double apogee, double perigee, double eccentricity) {
        double avgAlt = (apogee + perigee) / 2;
        if (eccentricity > 0.25) return OrbitType.HEO_HIGHLY_ELLIPTICAL_ORBIT;
        if (avgAlt < 2000)       return OrbitType.LEO_LOW_EARTH_ORBIT;
        if (avgAlt < 35000)      return OrbitType.MEO_MEDIUM_EARTH_ORBIT;
        return OrbitType.GEO_GEOSTATIONARY_EARTH_ORBIT;
    }


    private LocalDateTime toLocalDateTime(AbsoluteDate date) {
        DateTimeComponents dtc = date.getComponents(TimeScalesFactory.getUTC());
        return LocalDateTime.of(
                dtc.getDate().getYear(),
                dtc.getDate().getMonth(),
                dtc.getDate().getDay(),
                dtc.getTime().getHour(),
                dtc.getTime().getMinute(),
                (int) dtc.getTime().getSecond()
        );
    }
}
