package com.wificaptive.campaigns.repository;

import com.wificaptive.campaigns.entity.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Page<Campaign> findByStatus(String status, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'APPROVED' AND :now BETWEEN c.startTime AND c.endTime ORDER BY c.priority DESC")
    List<Campaign> findActiveApprovedCampaigns(@Param("now") Instant now);
}

