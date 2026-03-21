package org.example.orbit.Mapper;

import org.example.orbit.Model.Satellite;
import org.example.orbit.ModelDto.SatelliteDto;
import org.springframework.stereotype.Component;

@Component
public class SatelliteMapper {
    public SatelliteDto toDto(Satellite satellite) {
        return SatelliteDto.builder()
                .noradId(satellite.getNoradId())
                .name(satellite.getName())
                .country(satellite.getCountry())
                .orbitType(satellite.getOrbitType())
                .objectType(satellite.getObjectType())
                .inclination(satellite.getInclination())
                .eccentricity(satellite.getEccentricity())
                .apogeeKm(satellite.getApogeeKm())
                .perigeeKm(satellite.getPerigeeKm())
                .periodMinutes(satellite.getPeriodMinutes())
                .epochTime(satellite.getEpochTime())
                .build();
    }
}
