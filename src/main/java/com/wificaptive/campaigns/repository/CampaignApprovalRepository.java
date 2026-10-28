package com.wificaptive.campaigns.repository;

import com.wificaptive.campaigns.entity.CampaignApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignApprovalRepository extends JpaRepository<CampaignApproval, Long> {
    List<CampaignApproval> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);
}

