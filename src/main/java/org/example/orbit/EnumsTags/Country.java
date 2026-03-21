package org.example.orbit.EnumsTags;

public enum Country {
    USA_UNITED_STATES("US"), RU_RUSSIA("CIS"),
    CN_CHINA("CN"), IN_INDIA("IN"),
    JP_JAPAN("JP"), FR_FRANCE("FR"),
    UK_UNITED_KINGDOM("UK"), DE_GERMANY("GER"),
    IT_ITALY("IT"), ES_SPAIN("ES"),
    CA_CANADA("CA"), BR_BRAZIL("BR"),
    AR_ARGENTINA("AR"), IL_ISRAEL("IL"),
    IR_IRAN("IR"), KP_NORTH_KOREA("KP"),
    KR_SOUTH_KOREA("KR"), PK_PAKISTAN("PK"),
    TR_TURKEY("TR"), AE_UNITED_ARAB_EMIRATES("UAE"),
    SA_SAUDI_ARABIA("SA"), ZA_SOUTH_AFRICA("ZA"),
    NG_NIGERIA("NG"), EG_EGYPT("EG"),
    AU_AUSTRALIA("AU"), NZ_NEW_ZEALAND("NZ"),
    SE_SWEDEN("SE"), NO_NORWAY("NO"),
    NL_NETHERLANDS("NL"), BE_BELGIUM("BE"),
    CH_SWITZERLAND("CH"), AT_AUSTRIA("AT"),
    PL_POLAND("PL"), CZ_CZECH_REPUBLIC("CZ"),
    HU_HUNGARY("HU"), RO_ROMANIA("RO"),
    UA_UKRAINE("UA"), BY_BELARUS("BY"),
    KZ_KAZAKHSTAN("KZ"), VN_VIETNAM("VN"),
    TH_THAILAND("TH"), MY_MALAYSIA("MY"),
    SG_SINGAPORE("SG"), ID_INDONESIA("ID"),
    PH_PHILIPPINES("PH"), MX_MEXICO("MX"),
    CL_CHILE("CL"), CO_COLOMBIA("CO"),
    PE_PERU("PE"), VE_VENEZUELA("VE"),
    DZ_ALGERIA("DZ"), MA_MOROCCO("MA"),
    TN_TUNISIA("TN"), ET_ETHIOPIA("ET"),
    KE_KENYA("KE"), UNKNOWN("UNKNOWN");

    private final String code;
    Country(String code) { this.code = code; }
    public String getCode() { return code; }

    public static Country fromCode(String code) {
        if (code == null) return UNKNOWN;
        for (Country c : values()) {
            if (c.code.equalsIgnoreCase(code.trim())) return c;
        }
        return UNKNOWN;
    }
}
