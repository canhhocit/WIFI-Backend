package com.wificaptive.campaigns;

import com.wificaptive.campaigns.dto.ApproveCampaignRequest;
import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.campaigns.dto.CreateCampaignRequest;
import com.wificaptive.campaigns.entity.Campaign;
import com.wificaptive.campaigns.repository.CampaignApprovalRepository;
import com.wificaptive.campaigns.repository.CampaignRepository;
import com.wificaptive.campaigns.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignApprovalRepository approvalRepository;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(campaignRepository, approvalRepository);
    }

    @Test
    void testCreateSubmitAndApproveWorkflow() {
        CreateCampaignRequest req = new CreateCampaignRequest();
        req.setTitle("Tuyên truyền Dịch vụ công trực tuyến");
        req.setStartTime(Instant.now());
        req.setEndTime(Instant.now().plus(7, ChronoUnit.DAYS));
        req.setPriority(2);

        Campaign c = new Campaign();
        c.setId(10L);
        c.setTitle(req.getTitle());
        c.setStatus("DRAFT");
        c.setStartTime(req.getStartTime());
        c.setEndTime(req.getEndTime());

        when(campaignRepository.save(any(Campaign.class))).thenReturn(c);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(c));

        CampaignDto created = campaignService.createCampaign(req, "editor1");
        assertNotNull(created);
        assertEquals("DRAFT", created.getStatus());

        // Submit
        CampaignDto submitted = campaignService.submitForApproval(10L, "editor1");
        assertEquals("PENDING_APPROVAL", submitted.getStatus());
        verify(approvalRepository, times(1)).save(any());

        // Approve
        ApproveCampaignRequest approveReq = new ApproveCampaignRequest();
        approveReq.setComment("Đạt chuẩn chính sách tuyên truyền số");
        CampaignDto approved = campaignService.approveCampaign(10L, approveReq, "lead_approver");
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("lead_approver", approved.getApprovedBy());
    }
}

