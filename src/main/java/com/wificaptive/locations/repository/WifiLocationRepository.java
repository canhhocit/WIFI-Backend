package com.wificaptive.locations.repository;

import com.wificaptive.locations.entity.WifiLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WifiLocationRepository extends JpaRepository<WifiLocation, Long> {
    Optional<WifiLocation> findBySlug(String slug);
    Optional<WifiLocation> findByLocationCode(String locationCode);

    /** Cache số cổng LAN ON (= số AP thực) của điểm, do scheduler nền ghi; tránh gọi GWN khi hiển thị danh sách. */
    @Modifying
    @Query("UPDATE WifiLocation l SET l.activeApCount = :cnt WHERE l.id = :id")
    void updateActiveApCount(@Param("id") Long id, @Param("cnt") Integer cnt);
    Optional<WifiLocation> findBySlugAndDeletedFalse(String slug);
    Optional<WifiLocation> findByLocationCodeAndDeletedFalse(String locationCode);
    boolean existsBySlug(String slug);
    boolean existsByLocationCode(String locationCode);
    boolean existsBySlugAndDeletedFalse(String slug);
    boolean existsByLocationCodeAndDeletedFalse(String locationCode);
    List<WifiLocation> findByActiveTrue();
    List<WifiLocation> findByActiveTrueAndDeletedFalse();
    List<WifiLocation> findByAdminUnit_IdInAndActiveTrueAndDeletedFalse(Collection<Long> adminUnitIds);
    List<WifiLocation> findByAdminUnit_IdInAndDeletedFalse(Collection<Long> adminUnitIds);
    long countByActiveTrue();
    long countByActiveTrueAndDeletedFalse();
    long countByDeletedFalse();
    Page<WifiLocation> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<WifiLocation> findByDeletedFalse(Pageable pageable);
    Page<WifiLocation> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);
    Page<WifiLocation> findByAdminUnit_IdInAndDeletedFalse(Collection<Long> adminUnitIds, Pageable pageable);
    Page<WifiLocation> findByNameContainingIgnoreCaseAndAdminUnit_IdInAndDeletedFalse(String name, Collection<Long> adminUnitIds, Pageable pageable);
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
