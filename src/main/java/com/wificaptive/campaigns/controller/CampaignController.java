package com.wificaptive.campaigns.controller;

import com.wificaptive.campaigns.dto.ApproveCampaignRequest;
import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.campaigns.dto.CreateCampaignRequest;
import com.wificaptive.campaigns.service.CampaignService;
import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
@PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'VIEW')")
public class CampaignController {

    private final CampaignService campaignService;
    private final com.wificaptive.portal.service.PortalService portalService;
    private final com.wificaptive.upload.controller.MediaStreamController mediaStreamController;

    @org.springframework.beans.factory.annotation.Autowired
    public CampaignController(
            CampaignService campaignService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.wificaptive.portal.service.PortalService portalService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.wificaptive.upload.controller.MediaStreamController mediaStreamController) {
        this.campaignService = campaignService;
        this.portalService = portalService;
        this.mediaStreamController = mediaStreamController;
    }

    public CampaignController(CampaignService campaignService) {
        this(campaignService, null, null);
    }

    private void invalidatePortalCache() {
        if (portalService != null) {
            portalService.invalidatePortalCache();
        }
    }

    private void warmUpVideoIfNeeded(CampaignDto dto) {
        if (dto != null && mediaStreamController != null && dto.getVideoUrl() != null) {
            mediaStreamController.warmUpYouTubeVideoAsync(dto.getVideoUrl());
        }
    }

    @GetMapping
    public ApiResponse<PageResponse<CampaignDto>> getCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(campaignService.getCampaigns(status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignDto> getCampaignById(@PathVariable Long id) {
        return ApiResponse.ok(campaignService.getCampaignById(id));
    }

    @PostMapping
    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'CREATE')")
    public ApiResponse<CampaignDto> createCampaign(
            @Valid @RequestBody CreateCampaignRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "admin";
        CampaignDto dto = campaignService.createCampaign(req, username);
        warmUpVideoIfNeeded(dto);
        invalidatePortalCache();
        return ApiResponse.ok("Tạo chiến dịch mới thành công", dto);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'EDIT') or @rbac.hasPermission('CAMPAIGNS', 'CREATE')")
    public ApiResponse<CampaignDto> submitForApproval(
            @PathVariable Long id,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "admin";
        CampaignDto dto = campaignService.submitForApproval(id, username);
        warmUpVideoIfNeeded(dto);
        invalidatePortalCache();
        return ApiResponse.ok("Đã gửi phê duyệt chiến dịch", dto);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'APPROVE')")
    public ApiResponse<CampaignDto> approveCampaign(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveCampaignRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "approver";
        CampaignDto dto = campaignService.approveCampaign(id, req, username);
        warmUpVideoIfNeeded(dto);
        invalidatePortalCache();
        return ApiResponse.ok("Phê duyệt chiến dịch thành công", dto);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'APPROVE')")
    public ApiResponse<CampaignDto> rejectCampaign(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveCampaignRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "reviewer";
        CampaignDto dto = campaignService.rejectCampaign(id, req, username);
        invalidatePortalCache();
        return ApiResponse.ok("Đã từ chối chiến dịch", dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'EDIT')")
    public ApiResponse<CampaignDto> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CreateCampaignRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "editor";
        CampaignDto dto = campaignService.updateCampaign(id, req, username);
        warmUpVideoIfNeeded(dto);
        invalidatePortalCache();
        return ApiResponse.ok("Cập nhật chiến dịch thành công", dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'DELETE')")
    public ApiResponse<Void> deleteCampaign(
            @PathVariable Long id,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "admin";
        campaignService.deleteCampaign(id, username);
        invalidatePortalCache();
        return ApiResponse.ok("Đã xóa chiến dịch thành công", null);
    }
}

