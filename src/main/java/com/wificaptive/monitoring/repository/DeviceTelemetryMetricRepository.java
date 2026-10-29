package com.wificaptive.monitoring.repository;

import com.wificaptive.monitoring.entity.DeviceTelemetryMetric;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceTelemetryMetricRepository extends JpaRepository<DeviceTelemetryMetric, Long> {

    @Query("SELECT m FROM DeviceTelemetryMetric m WHERE m.locationId = :locationId ORDER BY m.recordedAt DESC")
    List<DeviceTelemetryMetric> findLatestByLocationId(@Param("locationId") Long locationId, Pageable pageable);

    @Query("SELECT m FROM DeviceTelemetryMetric m WHERE m.deviceId = :deviceId ORDER BY m.recordedAt DESC")
    List<DeviceTelemetryMetric> findLatestByDeviceId(@Param("deviceId") Long deviceId, Pageable pageable);
}

