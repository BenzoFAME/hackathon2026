package org.example.orbit.ModelDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/// ДЛЯ SGP4
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SatPositionDto {
    private Integer noradId;
    private String name;
    private Double latitude; ///широта
    private Double longitude;///долгота
    private Double altitudeKm;///высота над землей в км
    private String timestamp;///момент времени расчета (ISO 8601)
}
