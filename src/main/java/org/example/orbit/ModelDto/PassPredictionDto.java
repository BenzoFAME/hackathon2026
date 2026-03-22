package org.example.orbit.ModelDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PassPredictionDto {
    private Integer noradId;
    private String name;
    private String riseTime; ///Время появления над горизонтов
    private String maxElevationTime;///Время максимальной высоты
    private String setTime;///Время ухода за горизонт
    private double maxElevationDeg;;///Максимальный угол возвышения(градусы)
    private Double durationMinutes;///Длительность пролета
}
