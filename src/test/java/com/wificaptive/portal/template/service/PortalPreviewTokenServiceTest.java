package com.wificaptive.portal.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.portal.dto.PortalInitResponse;
import com.wificaptive.portal.template.dto.PortalPreviewLinkRequest;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.surveys.dto.SurveyDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class PortalPreviewTokenServiceTest {

    private final PortalPreviewTokenService service = new PortalPreviewTokenService(
            "test-only-secret",
            new ObjectMapper().findAndRegisterModules());

    @Test
    void previewLinkRoundTripsSanitizedDataAndExpiry() {
        LocationDto location = new LocationDto();
        location.setId(15L);
        location.setSlug("nha-van-hoa");
        location.setName("Nhà văn hóa");
        location.setContactName("Người phụ trách");
        location.setContactPhone("0900000000");
        location.setCircuitCode("secret-circuit");
        location.setIspName("ISP nội bộ");
        location.setStatusReason("Thông tin nội bộ");
        location.setLatitude(20.8);

        CampaignDto campaign = new CampaignDto();
        campaign.setTitle("Chiến dịch xem trước");
        campaign.setCreatedBy("admin-internal");
        campaign.setTargetLocationId(15L);

        SurveyDto survey = new SurveyDto();
        survey.setCurrentResponsesCount(42);
        survey.setMaxResponsesQuota(100);

        PortalTemplateDto template = new PortalTemplateDto();
        template.setId(9L);
        template.setActive(true);

        PortalInitResponse preview = new PortalInitResponse();
        preview.setLocation(location);
        preview.setClientIp("192.0.2.1");
        preview.setClientMacHint("00:11:22:33:44:55");
        preview.setActiveCampaign(campaign);
        preview.setActiveSurvey(survey);
        preview.setPortalTemplate(template);

        PortalPreviewLinkRequest request = new PortalPreviewLinkRequest();
        request.setPreviewData(preview);
        request.setExpiresInMinutes(15);

        var link = service.createLink(request);
        var result = service.readLink(link.getToken());

        assertTrue(result.getExpiresAt().isAfter(java.time.Instant.now()));
        assertNull(result.getPreviewData().getClientIp());
        assertNull(result.getPreviewData().getClientMacHint());
        assertEquals(0L, result.getPreviewData().getLocation().getId());
        assertNull(result.getPreviewData().getLocation().getDescription());
        assertNull(result.getPreviewData().getLocation().getContactName());
        assertNull(result.getPreviewData().getLocation().getContactPhone());
        assertNull(result.getPreviewData().getLocation().getCircuitCode());
        assertNull(result.getPreviewData().getLocation().getIspName());
        assertNull(result.getPreviewData().getLocation().getStatusReason());
        assertNull(result.getPreviewData().getLocation().getLatitude());
        assertNull(result.getPreviewData().getActiveCampaign().getCreatedBy());
        assertNull(result.getPreviewData().getActiveCampaign().getTargetLocationId());
        assertNull(result.getPreviewData().getActiveSurvey().getMaxResponsesQuota());
        assertEquals(0, result.getPreviewData().getActiveSurvey().getCurrentResponsesCount());
        assertNull(result.getPreviewData().getPortalTemplate().getId());
        assertFalse(result.getPreviewData().getPortalTemplate().isActive());
    }

    @Test
    void previewLinkRejectsExpirationOutsideAllowedRange() {
        PortalInitResponse preview = new PortalInitResponse();
        preview.setLocation(new LocationDto());
        PortalPreviewLinkRequest request = new PortalPreviewLinkRequest();
        request.setPreviewData(preview);
        request.setExpiresInMinutes(4);

        assertThrows(ResponseStatusException.class, () -> service.createLink(request));
    }
}
