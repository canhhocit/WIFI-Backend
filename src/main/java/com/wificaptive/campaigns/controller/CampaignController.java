package com.wificaptive.campaigns.controller;

import com.wificaptive.campaigns.dto.ApproveCampaignRequest;
import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.campaigns.dto.CreateCampaignRequest;
import com.wificaptive.campaigns.service.CampaignService;
import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
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
    public ApiResponse<CampaignDto> createCampaign(
            @Valid @RequestBody CreateCampaignRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "admin";
        return ApiResponse.ok("Tạo chiến dịch mới thành công", campaignService.createCampaign(req, username));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<CampaignDto> submitForApproval(
            @PathVariable Long id,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "admin";
        return ApiResponse.ok("Đã gửi phê duyệt chiến dịch", campaignService.submitForApproval(id, username));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<CampaignDto> approveCampaign(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveCampaignRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "approver";
        return ApiResponse.ok("Phê duyệt chiến dịch thành công", campaignService.approveCampaign(id, req, username));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<CampaignDto> rejectCampaign(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveCampaignRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : "reviewer";
        return ApiResponse.ok("Đã từ chối chiến dịch", campaignService.rejectCampaign(id, req, username));
    }
}

