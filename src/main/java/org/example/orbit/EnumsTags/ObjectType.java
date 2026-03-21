package org.example.orbit.EnumsTags;

public enum ObjectType {
    PAYLOAD_PAYLOAD("PAY"),
    DEBRIS_SPACE_DEBRIS("DEB"),
    ROCKET_BODY_ROCKET_BODY("R/B"),
    UNKNOWN("UNKNOWN");

    private final String code;
    ObjectType(String code) { this.code = code; }
    public String getCode() { return code; }

    public static ObjectType fromCode(String code) {
        if (code == null) return UNKNOWN;
        for (ObjectType t : values()) {
            if (t.code.equalsIgnoreCase(code.trim())) return t;
        }
        return UNKNOWN;
    }
}