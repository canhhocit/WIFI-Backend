package com.wificaptive.network.repository;

import com.wificaptive.network.entity.AccessLogRetention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AccessLogRetentionRepository extends JpaRepository<AccessLogRetention, Long> {

    Page<AccessLogRetention> findByClientMacContainingIgnoreCase(String clientMac, Pageable pageable);

    @Query("SELECT l FROM AccessLogRetention l WHERE " +
           "(:clientMac IS NULL OR LOWER(l.clientMac) LIKE LOWER(CONCAT('%', :clientMac, '%'))) AND " +
           "(:locationId IS NULL OR l.locationId = :locationId) AND " +
           "(:startDate IS NULL OR l.authenticatedAt >= :startDate) AND " +
           "(:endDate IS NULL OR l.authenticatedAt <= :endDate)")
    Page<AccessLogRetention> searchLogs(
            @Param("clientMac") String clientMac,
            @Param("locationId") Long locationId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    List<AccessLogRetention> findByRetainedUntilBefore(Instant cutoff);

    long countByLocationId(Long locationId);
}

