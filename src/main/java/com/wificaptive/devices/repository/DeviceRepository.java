package com.wificaptive.devices.repository;

import com.wificaptive.devices.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByMacAddressIgnoreCase(String macAddress);
    Page<Device> findByMacAddressContainingIgnoreCaseOrIpAddressContaining(String mac, String ip, Pageable pageable);
}
