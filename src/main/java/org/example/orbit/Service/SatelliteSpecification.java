package org.example.orbit.Service;

import org.example.orbit.EnumsTags.Country;
import org.example.orbit.EnumsTags.ObjectType;
import org.example.orbit.EnumsTags.OrbitType;
import org.example.orbit.Model.Satellite;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class SatelliteSpecification {
    public static Specification<Satellite> filter(Country country,
                                                  OrbitType orbitType,
                                                  ObjectType objectType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (country != null)    predicates.add(cb.equal(root.get("country"), country));
            if (orbitType != null)  predicates.add(cb.equal(root.get("orbitType"), orbitType));
            if (objectType != null) predicates.add(cb.equal(root.get("objectType"), objectType));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
