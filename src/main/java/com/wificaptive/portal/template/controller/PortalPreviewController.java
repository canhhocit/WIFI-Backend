package com.wificaptive.portal.template.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.portal.template.dto.PortalPreviewDataResponse;
import com.wificaptive.portal.template.service.PortalPreviewTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal/preview")
public class PortalPreviewController {
    private final PortalPreviewTokenService previewTokenService;

    public PortalPreviewController(PortalPreviewTokenService previewTokenService) {
        this.previewTokenService = previewTokenService;
    }

    @GetMapping("/{token}")
    public ApiResponse<PortalPreviewDataResponse> getPreview(@PathVariable String token) {
        return ApiResponse.ok(previewTokenService.readLink(token));
    }
}
