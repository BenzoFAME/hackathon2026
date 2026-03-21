package org.example.orbit.Model;

import lombok.Data;
import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.orekit.orbits.OrbitType;

@Data
public class SatelliteFilterRequest {
    private Country country;
    private OrbitType orbitType;
    private ObjectType objectType;
    private Double minInclination; /// минимальное наклонение
    private Double maxInclination; /// максимальное наклонение
}
