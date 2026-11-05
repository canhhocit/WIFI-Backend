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
    Optional<WifiLocation> findBySlugAndDeletedFalse(String slug);
    Optional<WifiLocation> findByLocationCodeAndDeletedFalse(String locationCode);
    boolean existsBySlug(String slug);
    boolean existsByLocationCode(String locationCode);
    boolean existsBySlugAndDeletedFalse(String slug);
    boolean existsByLocationCodeAndDeletedFalse(String locationCode);
    List<WifiLocation> findByActiveTrue();
    List<WifiLocation> findByActiveTrueAndDeletedFalse();
    long countByActiveTrue();
    long countByActiveTrueAndDeletedFalse();
    long countByDeletedFalse();
    Page<WifiLocation> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<WifiLocation> findByDeletedFalse(Pageable pageable);
    Page<WifiLocation> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);
    @Query("SELECT l FROM WifiLocation l LEFT JOIN FETCH l.adminUnit u LEFT JOIN FETCH u.parent")
    List<WifiLocation> findAllWithAdminUnit();
    @Query("SELECT l FROM WifiLocation l LEFT JOIN FETCH l.adminUnit u LEFT JOIN FETCH u.parent WHERE l.deleted = false")
    List<WifiLocation> findAllWithAdminUnitAndDeletedFalse();
    @Query("SELECT l FROM WifiLocation l WHERE l.adminUnit.id = :adminUnitId")
    List<WifiLocation> findByAdminUnitId(@Param("adminUnitId") Long adminUnitId);
    @Query("SELECT l FROM WifiLocation l WHERE l.adminUnit.id = :adminUnitId AND l.deleted = false")
    List<WifiLocation> findByAdminUnitIdAndDeletedFalse(@Param("adminUnitId") Long adminUnitId);

    @Modifying
    @Query("UPDATE WifiLocation l SET l.adminUnit.id = :targetId WHERE l.adminUnit.id = :sourceId")
    void reassignLocationsAdminUnit(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
