package com.wificaptive.devices.repository;

import com.wificaptive.devices.entity.NetworkDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, Long> {

    Optional<NetworkDevice> findByMacAddress(String macAddress);

    Optional<NetworkDevice> findBySerialNumber(String serialNumber);

    boolean existsByMacAddress(String macAddress);

    boolean existsBySerialNumber(String serialNumber);

    List<NetworkDevice> findByLocationId(Long locationId);

    Page<NetworkDevice> findByStatus(String status, Pageable pageable);

    Page<NetworkDevice> findByDeviceType(String deviceType, Pageable pageable);

    Page<NetworkDevice> findByLocationAdminUnitId(Long adminUnitId, Pageable pageable);
}

