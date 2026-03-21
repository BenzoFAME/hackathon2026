package org.example.orbit.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orbit.Model.Satellite;
import org.example.orbit.ModelDto.SatPositionDto;
import org.example.orbit.repository.SatelliteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SatelliteBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SatelliteRepository satelliteRepository;
    private final Sgp4Service sgp4Service;

    // Запускаем расчет каждые 3 секунды
    @Scheduled(fixedRate = 3000)
    public void broadcastPositions() {
        // Берем 100 спутников, чтобы уложиться в требования кейса и не убить CPU
        List<Satellite> satellites = satelliteRepository.findAll(PageRequest.of(0, 100)).getContent();
        if (satellites.isEmpty()) return;

        Instant now = Instant.now();

        // Считаем текущую позицию SGP4 для каждого
        List<SatPositionDto> positions = satellites.stream().map(sat -> {
            try {
                double[] latLonAlt = sgp4Service.getLatLonAlt(sat.getTleLine1(), sat.getTleLine2(), now);
                return SatPositionDto.builder()
                        .noradId(sat.getNoradId())
                        .name(sat.getName())
                        .latitude(latLonAlt[0])
                        .longitude(latLonAlt[1])
                        .altitudeKm(latLonAlt[2])
                        .timestamp(now.toString())
                        .build();
            } catch (Exception e) {
                return null;
            }
        }).filter(p -> p != null).toList();

        // Отправляем JSON-массив позиций всем подписчикам топика
        messagingTemplate.convertAndSend("/topic/positions", positions);
    }
}