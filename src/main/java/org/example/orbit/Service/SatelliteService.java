package org.example.orbit.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;
import org.example.orbit.Mapper.SatelliteMapper;
import org.example.orbit.Model.Satellite;
import org.example.orbit.ModelDto.PassPredictionDto;
import org.example.orbit.ModelDto.SatPositionDto;
import org.example.orbit.ModelDto.SatelliteCardDto;
import org.example.orbit.ModelDto.SatelliteDto;
import org.example.orbit.repository.SatelliteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SatelliteService {
    private final SatelliteRepository satelliteRepository;
    private final Sgp4Service sgp4Service;
    private final SatelliteMapper satelliteMapper;
    private final PassPredictionService passPredictionService;

    /// Получаем все спутники
    public Page<SatelliteDto> getAll(Pageable pageable) {
        return satelliteRepository.findAll(pageable).map(satelliteMapper::toDto);
    }
    /// Ищем по имени либо по NORAD ID
    public List<SatelliteDto> search(String querry){
        try {
            /// Ищем по id
            int noradId = Integer.parseInt(querry.trim());
            return satelliteRepository.findByNoradId(noradId)
                    .map(s -> List.of(satelliteMapper.toDto(s))).orElse(List.of());
        }catch (NumberFormatException e){
            /// Ищем по name
            return satelliteRepository.findByNameContainingIgnoreCase(querry)
                    .stream()
                    .map(satelliteMapper::toDto)
                    .toList();
        }
    }
    /// фильтр
    public List<SatelliteDto> filter (Country country , OrbitType orbitType,
                                      ObjectType objectType){
        return satelliteRepository.findByFilters(country , orbitType , objectType)
                .stream().map(satelliteMapper::toDto).toList();
    }
    /// Текущая позиция
    public SatPositionDto getPosition(Integer noradId , Instant time){
        Satellite sat = satelliteRepository.findByNoradId(noradId)
                .orElseThrow(()-> new EntityNotFoundException("Спутник с NORAD ID" + noradId + "не найден"));
        double[] latLonAlt = sgp4Service.getLatLonAlt(sat.getTleLine1(),
                sat.getTleLine2(), time);

        return SatPositionDto.builder()
                .noradId(noradId)
                .name(sat.getName())
                .latitude(latLonAlt[0])
                .longitude(latLonAlt[1])
                .altitudeKm(latLonAlt[2])
                .timestamp(time.toString())
                .build();
    }
    /// Трек орбиты
    public List<SatPositionDto> getOrbitTrack(Integer noradId){
        Satellite sat = satelliteRepository.findByNoradId(noradId)
                .orElseThrow(() -> new EntityNotFoundException("Спутник не найден: " + noradId));
        int period = sat.getPeriodMinutes() != null ? sat.getPeriodMinutes().intValue() : 90;

        return sgp4Service.getOrbitTrack(sat.getTleLine1(),
                sat.getTleLine2(), Instant.now() , period , 30 )
                .stream()
                .map(point -> SatPositionDto.builder()
                        .noradId(sat.getNoradId())
                        .name(sat.getName())
                        .latitude(point[0])
                        .longitude(point[1])
                        .altitudeKm(point[2])
                        .build())
                .toList();
    }

    public SatelliteCardDto getCard(Integer noradId){
        Satellite sat = satelliteRepository.findByNoradId(noradId)
                .orElseThrow(() -> new
                        EntityNotFoundException
                        ("Спутник с NORAD ID " + noradId + " не найден"));
        /// считаем позицию обьекта через SGP4
        Instant now = Instant.now();
        double[] latLonAlt = sgp4Service.getLatLonAlt(sat.getTleLine1(),
                sat.getTleLine2(), now);
        /// Возвращаем
        return SatelliteCardDto.builder()
                .noradId(sat.getNoradId())
                .name(sat.getName())
                .country(sat.getCountry())
                .orbitType(sat.getOrbitType())
                .objectType(sat.getObjectType())
                .inclination(sat.getInclination())
                .eccentricity(sat.getEccentricity())
                .apogeeKm(sat.getApogeeKm())
                .perigeeKm(sat.getPerigeeKm())
                .periodMinutes(sat.getPeriodMinutes())
                .meanMotion(sat.getMeanMotion())
                .latitude(latLonAlt[0])
                .longitude(latLonAlt[1])
                .altitudeKm(latLonAlt[2])
                .positionTimestamp(now.toString())
                .epochTime(sat.getEpochTime())
                .updatedAt(sat.getUpdateAt())
                .build();
    }

    public List<PassPredictionDto> getPasses(Integer noradId ,
                                             double lat , double lon,int hoursAhead){
        Satellite sat = satelliteRepository.findByNoradId(noradId)
                .orElseThrow(() -> new EntityNotFoundException("Спутник не найден: " + noradId));
        return passPredictionService.predict(sat , lat, lon, hoursAhead);
    }
}
