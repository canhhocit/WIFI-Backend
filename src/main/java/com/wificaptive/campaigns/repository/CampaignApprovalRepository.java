package com.wificaptive.campaigns.repository;

import com.wificaptive.campaigns.entity.CampaignApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignApprovalRepository extends JpaRepository<CampaignApproval, Long> {
    List<CampaignApproval> findByCampaignIdOrderByCreatedAtDesc(Long campaignId);

    @Modifying
    @Query("DELETE FROM CampaignApproval a WHERE a.campaignId = :campaignId")
    void deleteByCampaignId(@Param("campaignId") Long campaignId);
}

