package com.wificaptive.locations.repository;

import com.wificaptive.locations.entity.WifiLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WifiLocationRepository extends JpaRepository<WifiLocation, Long> {
    Optional<WifiLocation> findBySlug(String slug);
    Optional<WifiLocation> findByLocationCode(String locationCode);
    boolean existsBySlug(String slug);
    boolean existsByLocationCode(String locationCode);
    List<WifiLocation> findByActiveTrue();
    Page<WifiLocation> findByNameContainingIgnoreCase(String name, Pageable pageable);
    @Query("SELECT l FROM WifiLocation l WHERE l.adminUnit.id = :adminUnitId")
    List<WifiLocation> findByAdminUnitId(@Param("adminUnitId") Long adminUnitId);

    @Modifying
    @Query("UPDATE WifiLocation l SET l.adminUnit.id = :targetId WHERE l.adminUnit.id = :sourceId")
    void reassignLocationsAdminUnit(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
