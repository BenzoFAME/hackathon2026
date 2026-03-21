package org.example.orbit.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;

import java.time.LocalDateTime;

@Entity
@Table(name = "satellites")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Satellite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private Integer noradId; /// Уникальный номер NORAD
    @Column(nullable = false)
    private String name; /// Имя спутника хд
    @Column(nullable = false , length = 70)
    private String tleLine1; /// Первая строка
    @Column(nullable = false , length = 70)
    private String tleLine2; /// Вторая строка
    @Enumerated(EnumType.STRING)
    private Country country;
    @Enumerated(EnumType.STRING)
    private OrbitType orbitType;
    @Enumerated(EnumType.STRING)
    private ObjectType objectType;
    private Double inclination; /// Наклонение орбиты в градусах
    private Double eccentricity; /// Эксцентриситет (0 = круговая)
    private Double meanMotion; /// обороты в сутки (из TLE2 будет)
    private Double apogeeKm; /// Высота апогея в км
    private Double perigeeKm; /// Высота перигея в км
    private Double periodMinutes; /// Период обращения в минутах
    private LocalDateTime epochTime; /// Когда данные были актуальны
    private LocalDateTime updateAt; /// Когда в последний раз обновлялись данные
    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updateAt = LocalDateTime.now();
    }
}
