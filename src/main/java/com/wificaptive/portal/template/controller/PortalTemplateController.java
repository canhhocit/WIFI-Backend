package com.wificaptive.portal.template.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.portal.template.dto.PortalPreviewLinkRequest;
import com.wificaptive.portal.template.dto.PortalPreviewLinkResponse;
import com.wificaptive.portal.template.service.PortalTemplateService;
import com.wificaptive.portal.template.service.PortalPreviewTokenService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/portal-templates")
public class PortalTemplateController {

    private final PortalTemplateService service;
    private final PortalPreviewTokenService previewTokenService;

    public PortalTemplateController(PortalTemplateService service, PortalPreviewTokenService previewTokenService) {
        this.service = service;
        this.previewTokenService = previewTokenService;
    }

    @GetMapping
    public ApiResponse<List<PortalTemplateDto>> getTemplates() {
        return ApiResponse.ok(service.getAllTemplates());
    }

    @GetMapping("/active")
    public ApiResponse<PortalTemplateDto> getActiveTemplate() {
        return ApiResponse.ok(service.getDefaultOrActive());
    }

    @PostMapping
    public ApiResponse<PortalTemplateDto> saveTemplate(@RequestBody PortalTemplateDto dto) {
        return ApiResponse.ok("Lưu cấu hình giao diện Cổng chào thành công", service.saveOrUpdate(dto));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<com.wificaptive.portal.template.dto.PortalTemplateVersionDto>> getVersions(@PathVariable Long id) {
        return ApiResponse.ok(service.getTemplateVersions(id));
    }

    @PostMapping("/{id}/rollback/{version}")
    public ApiResponse<PortalTemplateDto> rollbackVersion(@PathVariable Long id, @PathVariable Integer version) {
        return ApiResponse.ok("Khôi phục phiên bản v" + version + " thành công", service.rollbackToVersion(id, version));
    }

    @PostMapping("/preview-links")
    public ApiResponse<PortalPreviewLinkResponse> createPreviewLink(@RequestBody PortalPreviewLinkRequest request) {
        return ApiResponse.ok("Đã tạo liên kết xem trước có thời hạn", previewTokenService.createLink(request));
    }
}

