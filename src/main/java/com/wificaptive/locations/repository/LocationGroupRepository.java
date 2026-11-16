package com.wificaptive.locations.repository;

import com.wificaptive.locations.entity.LocationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationGroupRepository extends JpaRepository<LocationGroup, Long> {

    Optional<LocationGroup> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    @Query("SELECT g FROM LocationGroup g JOIN g.locations l WHERE l.id = :locationId")
    List<LocationGroup> findByLocationId(@Param("locationId") Long locationId);
}
