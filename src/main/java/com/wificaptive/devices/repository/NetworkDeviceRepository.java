package com.wificaptive.devices.repository;

import com.wificaptive.devices.entity.NetworkDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, Long> {

    Optional<NetworkDevice> findByMacAddress(String macAddress);

    Optional<NetworkDevice> findBySerialNumber(String serialNumber);

    boolean existsByMacAddress(String macAddress);

    boolean existsBySerialNumber(String serialNumber);

    @Query("SELECT d FROM NetworkDevice d WHERE d.location.id = :locationId")
    List<NetworkDevice> findByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT d FROM NetworkDevice d WHERE d.location.id IN :locationIds")
    List<NetworkDevice> findByLocationIdIn(@Param("locationIds") Collection<Long> locationIds);

    long countByDeviceTypeIgnoreCaseAndStatusIgnoreCase(String deviceType, String status);

    long countByDeviceTypeIgnoreCase(String deviceType);

    @Query("SELECT DISTINCT d.location.id FROM NetworkDevice d WHERE UPPER(d.deviceType) = 'ROUTER' AND UPPER(d.status) = 'ONLINE' AND d.location.id IS NOT NULL")
    java.util.Set<Long> findOnlineRouterLocationIds();

    @Query("SELECT DISTINCT d.location.id FROM NetworkDevice d WHERE UPPER(d.deviceType) = 'ROUTER' AND d.location.id IS NOT NULL")
    java.util.Set<Long> findRouterLocationIds();

    @Query("SELECT d FROM NetworkDevice d WHERE UPPER(d.deviceType) IN ('ACCESS_POINT', 'AP')")
    List<NetworkDevice> findAccessPoints();

    @Query("SELECT d FROM NetworkDevice d WHERE UPPER(d.deviceType) = 'ROUTER'")
    List<NetworkDevice> findRouters();

    @Query("SELECT d FROM NetworkDevice d WHERE d.location.id = :locationId AND UPPER(d.deviceType) = UPPER(:deviceType)")
    List<NetworkDevice> findByLocationIdAndDeviceType(@Param("locationId") Long locationId, @Param("deviceType") String deviceType);

    List<NetworkDevice> findByLocationIsNullOrderByUpdatedAtDesc();

    Page<NetworkDevice> findByLocationId(Long locationId, Pageable pageable);

    Page<NetworkDevice> findByLocationIsNull(Pageable pageable);

    Page<NetworkDevice> findByStatus(String status, Pageable pageable);

    Page<NetworkDevice> findByDeviceType(String deviceType, Pageable pageable);

    @Query("SELECT d FROM NetworkDevice d WHERE d.location.adminUnit.id = :adminUnitId")
    Page<NetworkDevice> findByLocationAdminUnitId(@Param("adminUnitId") Long adminUnitId, Pageable pageable);

    @Query("SELECT d FROM NetworkDevice d WHERE d.location.adminUnit.id IN :adminUnitIds")
    Page<NetworkDevice> findByLocationAdminUnitIdIn(@Param("adminUnitIds") Collection<Long> adminUnitIds, Pageable pageable);

    @Query("SELECT d FROM NetworkDevice d WHERE d.deviceType = :deviceType AND d.location.adminUnit.id IN :adminUnitIds")
    Page<NetworkDevice> findByDeviceTypeAndLocationAdminUnitIdIn(@Param("deviceType") String deviceType, @Param("adminUnitIds") Collection<Long> adminUnitIds, Pageable pageable);

    @Query("SELECT d FROM NetworkDevice d WHERE d.status = :status AND d.location.adminUnit.id IN :adminUnitIds")
    Page<NetworkDevice> findByStatusAndLocationAdminUnitIdIn(@Param("status") String status, @Param("adminUnitIds") Collection<Long> adminUnitIds, Pageable pageable);

    @Query("SELECT d.macAddress FROM NetworkDevice d WHERE UPPER(d.deviceType) IN :types")
    List<String> findMacAddressesByDeviceTypeIn(@Param("types") List<String> types);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM NetworkDevice d WHERE LOWER(REPLACE(REPLACE(d.macAddress, ':', ''), '-', '')) = :cleanMac AND UPPER(d.deviceType) IN :types")
    boolean existsByCleanMacAndDeviceTypeIn(@Param("cleanMac") String cleanMac, @Param("types") List<String> types);
}

