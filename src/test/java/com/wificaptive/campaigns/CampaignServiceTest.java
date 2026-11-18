package com.wificaptive.campaigns;

import com.wificaptive.campaigns.dto.ApproveCampaignRequest;
import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.campaigns.dto.CreateCampaignRequest;
import com.wificaptive.campaigns.entity.Campaign;
import com.wificaptive.campaigns.repository.CampaignApprovalRepository;
import com.wificaptive.campaigns.repository.CampaignRepository;
import com.wificaptive.campaigns.service.CampaignService;
import com.wificaptive.surveys.entity.Survey;
import com.wificaptive.surveys.repository.SurveyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    @Mock
    private SurveyRepository surveyRepository;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(campaignRepository, approvalRepository, surveyRepository);
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

    @Test
    void testGetActiveCampaignForLocationPriorityAndSpecificity() {
        Campaign cityWideP10 = new Campaign();
        cityWideP10.setId(1L);
        cityWideP10.setTitle("Chiến dịch Chuyển đổi số (P10)");
        cityWideP10.setPriority(10);

        Campaign cityWideP1 = new Campaign();
        cityWideP1.setId(2L);
        cityWideP1.setTitle("Hải Phòng - Thành phố cảng (P1)");
        cityWideP1.setPriority(1);

        Campaign locationP5 = new Campaign();
        locationP5.setId(3L);
        locationP5.setTitle("Chiến dịch Nhà Văn Hóa Thượng Lý (P5)");
        locationP5.setPriority(5);
        locationP5.setTargetLocationId(100L);

        // Simulated repository ordered by priority ASC: P1 (priority 1) comes first, then P5, then P10
        when(campaignRepository.findActiveApprovedCampaigns(any(Instant.class)))
                .thenReturn(List.of(cityWideP1, locationP5, cityWideP10));

        // When requesting for a generic location without targeted campaign: cityWideP1 (P1) is selected over cityWideP10
        Campaign chosenGeneral = campaignService.getActiveCampaignForLocation(999L, null);
        assertNotNull(chosenGeneral);
        assertEquals(2L, chosenGeneral.getId());
        assertEquals("Hải Phòng - Thành phố cảng (P1)", chosenGeneral.getTitle());

        // When requesting for location 100L: location-specific campaign takes precedence over city-wide
        Campaign chosenLocation = campaignService.getActiveCampaignForLocation(100L, null);
        assertNotNull(chosenLocation);
        assertEquals(3L, chosenLocation.getId());
        assertEquals("Chiến dịch Nhà Văn Hóa Thượng Lý (P5)", chosenLocation.getTitle());
    }

    @Test
    void testDeleteCampaignUnlinksSurveysAndApprovals() {
        Long campaignId = 55L;
        Campaign c = new Campaign();
        c.setId(campaignId);
        c.setTitle("Chiến dịch cần xóa");

        Survey linkedSurvey = new Survey();
        linkedSurvey.setCampaignId(campaignId);

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(c));
        when(surveyRepository.findByCampaignId(campaignId)).thenReturn(List.of(linkedSurvey));

        campaignService.deleteCampaign(campaignId, "admin_user");

        assertNull(linkedSurvey.getCampaignId());
        verify(surveyRepository, times(1)).saveAll(any());
        verify(approvalRepository, times(1)).deleteByCampaignId(campaignId);
        verify(campaignRepository, times(1)).delete(c);
    }
}

