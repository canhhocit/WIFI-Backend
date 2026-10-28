package com.wificaptive.portal.template.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.portal.template.service.PortalTemplateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/portal-templates")
public class PortalTemplateController {

    private final PortalTemplateService service;

    public PortalTemplateController(PortalTemplateService service) {
        this.service = service;
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
}

