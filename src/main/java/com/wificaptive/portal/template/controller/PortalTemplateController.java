package com.wificaptive.portal.template.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.portal.service.PortalService;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.portal.template.dto.PortalPreviewLinkRequest;
import com.wificaptive.portal.template.dto.PortalPreviewLinkResponse;
import com.wificaptive.portal.template.service.PortalTemplateService;
import com.wificaptive.portal.template.service.PortalPreviewTokenService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/portal-templates")
@PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'VIEW')")
public class PortalTemplateController {

    private final PortalTemplateService service;
    private final PortalPreviewTokenService previewTokenService;
    private final PortalService portalService;

    public PortalTemplateController(
            PortalTemplateService service,
            PortalPreviewTokenService previewTokenService,
            PortalService portalService) {
        this.service = service;
        this.previewTokenService = previewTokenService;
        this.portalService = portalService;
    }

    @GetMapping
    public ApiResponse<List<PortalTemplateDto>> getTemplates() {
        return ApiResponse.ok(service.getAllTemplates());
    }

    @GetMapping("/active")
    public ApiResponse<PortalTemplateDto> getActiveTemplate() {
        return ApiResponse.ok(service.getDefaultOrActive());
    }

    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'EDIT')")
    @PostMapping
    public ApiResponse<PortalTemplateDto> saveTemplate(@RequestBody PortalTemplateDto dto) {
        PortalTemplateDto saved = service.saveOrUpdate(dto);
        portalService.invalidatePortalCache();
        return ApiResponse.ok("Lưu cấu hình giao diện Cổng chào thành công", saved);
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<com.wificaptive.portal.template.dto.PortalTemplateVersionDto>> getVersions(@PathVariable Long id) {
        return ApiResponse.ok(service.getTemplateVersions(id));
    }

    @PreAuthorize("@rbac.hasPermission('CAMPAIGNS', 'EDIT')")
    @PostMapping("/{id}/rollback/{version}")
    public ApiResponse<PortalTemplateDto> rollbackVersion(@PathVariable Long id, @PathVariable Integer version) {
        PortalTemplateDto restored = service.rollbackToVersion(id, version);
        portalService.invalidatePortalCache();
        return ApiResponse.ok("Khôi phục phiên bản v" + version + " thành công", restored);
    }

    @PostMapping("/preview-links")
    public ApiResponse<PortalPreviewLinkResponse> createPreviewLink(@RequestBody PortalPreviewLinkRequest request) {
        return ApiResponse.ok("Đã tạo liên kết xem trước có thời hạn", previewTokenService.createLink(request));
    }
}

