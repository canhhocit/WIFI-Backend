package com.wificaptive.devices.repository;

import com.wificaptive.devices.entity.NetworkDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    Page<NetworkDevice> findByStatus(String status, Pageable pageable);

    Page<NetworkDevice> findByDeviceType(String deviceType, Pageable pageable);

    @Query("SELECT d FROM NetworkDevice d WHERE d.location.adminUnit.id = :adminUnitId")
    Page<NetworkDevice> findByLocationAdminUnitId(@Param("adminUnitId") Long adminUnitId, Pageable pageable);
}

