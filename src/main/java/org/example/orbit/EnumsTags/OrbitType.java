package org.example.orbit.EnumsTags;

public enum OrbitType {
    LEO_LOW_EARTH_ORBIT("LEO"),
    MEO_MEDIUM_EARTH_ORBIT("MEO"),
    GEO_GEOSTATIONARY_EARTH_ORBIT("GEO"),
    HEO_HIGHLY_ELLIPTICAL_ORBIT("HEO"),
    UNKNOWN("UNKNOWN");

    private final String code;
    OrbitType(String code) { this.code = code; }
    public String getCode() { return code; }

    public static OrbitType fromCode(String code) {
        if (code == null) return UNKNOWN;
        for (OrbitType o : values()) {
            if (o.code.equalsIgnoreCase(code.trim())) return o;
        }
        return UNKNOWN;
    }
}
