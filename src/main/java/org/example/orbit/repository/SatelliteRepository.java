package org.example.orbit.repository;

import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;
import org.example.orbit.Model.Satellite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SatelliteRepository extends JpaRepository<Satellite, Long>,
        JpaSpecificationExecutor<Satellite> {
    Optional<Satellite> findByNoradId(Integer noradId);
    List<Satellite> findByNameContainingIgnoreCase(String name);
    List<Satellite> findByCountryAndOrbitType(Country country, OrbitType orbitType);
    List<Satellite> findByOrbitType(OrbitType orbitType);
    /// Для более сложных фильтров — JPQL запрос
    @Query("SELECT s FROM Satellite s WHERE " +
            "(:country IS NULL OR s.country = :country) AND " +
            "(:orbitType IS NULL OR s.orbitType = :orbitType) AND " +
            "(:objectType IS NULL OR s.objectType = :objectType)")
    List<Satellite> findByFilters(
            @Param("country") Country country,
            @Param("orbitType") OrbitType orbitType,
            @Param("objectType") ObjectType objectType
    );
}
