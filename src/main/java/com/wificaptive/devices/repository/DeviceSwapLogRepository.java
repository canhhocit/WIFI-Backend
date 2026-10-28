package com.wificaptive.devices.repository;

import com.wificaptive.devices.entity.DeviceSwapLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceSwapLogRepository extends JpaRepository<DeviceSwapLog, Long> {
    List<DeviceSwapLog> findByLocationIdOrderBySwappedAtDesc(Long locationId);
    Page<DeviceSwapLog> findAllByOrderBySwappedAtDesc(Pageable pageable);
}

