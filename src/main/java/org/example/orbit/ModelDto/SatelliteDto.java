package org.example.orbit.ModelDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteDto {
    public Integer noradId;
    private String name;
    public String tleLine1;
    public String tleLine2;
    private Country country;
    private OrbitType orbitType;
    private ObjectType objectType;
    private Double inclination;
    private Double eccentricity;
    private Double apogeeKm;
    private Double perigeeKm;
    private Double periodMinutes;
    private LocalDateTime epochTime;
}
