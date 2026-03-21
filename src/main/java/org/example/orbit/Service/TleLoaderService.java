package org.example.orbit.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;
import org.example.orbit.Model.Satellite;
import org.example.orbit.repository.SatelliteRepository;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.orekit.data.DataContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TleLoaderService {

    private final SatelliteRepository satelliteRepository;
    private final RestClient celestrakRestClient;


    private final DataContext dataContext;

    private record SatcatEntry(String owner, String objectType) {}

    @PostConstruct
    public void init() {
        log.info(">>> [INIT] Запуск первичной загрузки данных...");
        try {
            loadActiveSatellites();
        } catch (Exception e) {
            log.error(">>> [INIT] Ошибка: {}", e.getMessage());
        }
    }

    /**
     * ПУНКТ 6: Автоматическое обновление данных по расписанию
     * Cron "0 0 0 * * *" означает: каждую полночь в 00:00:00 (по времени сервера)
     * протестил , вроде работает
     */
    @Scheduled(cron = "0 0 0  * * *")
    public void scheduledUpdate() {
        log.info(">>> [SCHEDULED] Запуск планового обновления TLE...");
        int count = loadActiveSatellites();
        log.info(">>> [SCHEDULED] Плановое обновление завершено. Обработано спутников: {}", count);
    }

    public int loadActiveSatellites() {
        log.info("loading satcat...");
        Map<Integer, SatcatEntry> satcatMap = loadSatcat();

        log.info("loading TLE...");
        String rawTle;
        try {
            rawTle = celestrakRestClient.get()
                    .uri("/NORAD/elements/gp.php?GROUP=active&FORMAT=tle")
                    .retrieve()
                    .body(String.class);
            log.info("TLE loaded: {} chars", rawTle.length());
        } catch (Exception e) {
            log.error("error {}", e.getMessage());
            throw new RuntimeException("error", e);
        }

        List<Satellite> satellites = parseTleText(rawTle, satcatMap);

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
                                    existing.setCountry(sat.getCountry());
                                    existing.setObjectType(sat.getObjectType());
                                    satelliteRepository.save(existing);
                                },
                                () -> satelliteRepository.save(sat)
                        );
                saved++;
            } catch (Exception e) {
                log.warn("error {}: {}", sat.getNoradId(), e.getMessage());
            }
        }
        log.info("save/update {} satellites", saved);
        return saved;
    }

    private Map<Integer, SatcatEntry> loadSatcat() {
        Map<Integer, SatcatEntry> map = new HashMap<>();
        try {
            String csv = celestrakRestClient.get()
                    .uri("/pub/satcat.csv")
                    .retrieve()
                    .body(String.class);

            log.info("CSV length: {}", csv.length());
            String[] lines = csv.split("\r?\n");
            log.info("CSV lines count: {}", lines.length);
            log.info("HEADER: {}", lines[0]);
            if (lines.length > 1) log.info("ROW 1: {}", lines[1]);

            for (int i = 1; i < lines.length; i++) {
                String[] cols = lines[i].split(",", -1);
                if (cols.length < 6) continue;
                try {
                    int noradId = Integer.parseInt(cols[2].trim());
                    String objectType = cols[3].trim();
                    String owner = cols[5].trim();
                    if (noradId == 900) {
                        log.info("SATCAT 900: objectType='{}' owner='{}'", objectType, owner);
                    }
                    map.put(noradId, new SatcatEntry(owner, objectType));
                } catch (NumberFormatException ignored) {}
            }
            log.info("satcat loaded: {} entries", map.size());
        } catch (Exception e) {
            log.warn("Не удалось загрузить satcat: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return map;
    }

    private List<Satellite> parseTleText(String rawText, Map<Integer, SatcatEntry> satcatMap) {
        List<Satellite> result = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) return result;

        String[] lines = rawText.split("\r?\n");
        for (int i = 0; i + 2 < lines.length; i += 3) {
            String name = lines[i].trim();
            String line1 = lines[i + 1].trim();
            String line2 = lines[i + 2].trim();
            if (!line1.startsWith("1 ") || !line2.startsWith("2 ")) {
                log.warn("Невалидные TLE строки для: {}", name);
                continue;
            }
            try {
                TLE tle = new TLE(line1, line2);
                SatcatEntry entry = satcatMap.get(tle.getSatelliteNumber());

                Satellite sat = Satellite.builder()
                        .name(name)
                        .noradId(tle.getSatelliteNumber())
                        .tleLine1(line1)
                        .tleLine2(line2)
                        .inclination(Math.toDegrees(tle.getI()))
                        .eccentricity(tle.getE())
                        .meanMotion(tle.getMeanMotion() * 86400 / (2 * Math.PI))
                        .epochTime(toLocalDateTime(tle.getDate()))
                        .periodMinutes(computePeriod(tle.getMeanMotion()))
                        .apogeeKm(computeApogee(tle))
                        .perigeeKm(computePerigee(tle))
                        .orbitType(detectOrbitType(computeApogee(tle), computePerigee(tle), tle.getE()))
                        .country(entry != null ? Country.fromCode(entry.owner()) : Country.UNKNOWN)
                        .objectType(entry != null ? ObjectType.fromCode(entry.objectType()) : ObjectType.UNKNOWN)
                        .build();
                result.add(sat);
            } catch (Exception e) {
                log.warn("Ошибка парсинга TLE для {}: {}", name, e.getMessage());
            }
        }
        return result;
    }

    private double computePeriod(double meanMotionRadPerSec) {
        return (2 * Math.PI / meanMotionRadPerSec) / 60.0;
    }

    private double computeApogee(TLE tle) {
        double mu = 398600.4418;
        double n = tle.getMeanMotion();
        double a = Math.cbrt(mu / (n * n));
        return a * (1 + tle.getE()) - 6371.0;
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