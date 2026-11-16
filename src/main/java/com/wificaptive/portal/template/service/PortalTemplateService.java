package com.wificaptive.portal.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.portal.template.dto.PortalTemplateVersionDto;
import com.wificaptive.portal.template.entity.PortalTemplate;
import com.wificaptive.portal.template.entity.PortalTemplateVersion;
import com.wificaptive.portal.template.repository.PortalTemplateRepository;
import com.wificaptive.portal.template.repository.PortalTemplateVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortalTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PortalTemplateService.class);

    private final PortalTemplateRepository repository;
    private final PortalTemplateVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public PortalTemplateService(
            PortalTemplateRepository repository,
            PortalTemplateVersionRepository versionRepository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PortalTemplateDto> getAllTemplates() {
        return repository.findAll().stream().map(PortalTemplateDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortalTemplateDto getDefaultOrActive() {
        List<PortalTemplate> active = repository.findByActiveTrue();
        if (active.isEmpty()) {
            return repository.findAll().stream().findFirst().map(PortalTemplateDto::from).orElse(null);
        }
        return PortalTemplateDto.from(active.get(0));
    }

    @Transactional
    public PortalTemplateDto saveOrUpdate(PortalTemplateDto dto) {
        PortalTemplate t;
        if (dto.getId() != null) {
            t = repository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("PortalTemplate not found with id: " + dto.getId()));
        } else {
            t = new PortalTemplate();
        }

        t.setName(dto.getName());
        t.setTemplateType(dto.getTemplateType() != null ? dto.getTemplateType() : "DEFAULT");
        t.setThemeColor(dto.getThemeColor());
        t.setBackgroundColor(dto.getBackgroundColor());
        if (dto.getBackgroundImageUrl() != null && !dto.getBackgroundImageUrl().isBlank()) {
            t.setBackgroundImageUrl(dto.getBackgroundImageUrl());
        }
        t.setLogoUrl(dto.getLogoUrl());
        t.setBannerUrl(dto.getBannerUrl());
        t.setTitle(dto.getTitle());
        t.setWelcomeText(dto.getWelcomeText());
        t.setButtonText(dto.getButtonText());
        t.setTermsText(dto.getTermsText());
        t.setLayoutConfig(dto.getLayoutConfig());
        t.setActive(dto.isActive());

        PortalTemplate saved = repository.save(t);

        // Tự động lưu snapshot phiên bản khi cập nhật template
        try {
            int nextVersion = versionRepository.findMaxVersionNumber(saved.getId()) + 1;
            String snapshotJson = objectMapper.writeValueAsString(PortalTemplateDto.from(saved));
            String versionName = "Phiên bản v" + nextVersion + " (" + saved.getName() + ")";
            versionRepository.save(new PortalTemplateVersion(saved.getId(), nextVersion, versionName, snapshotJson, "admin"));
            log.info("[Portal Template] Đã lưu snapshot phiên bản v{} cho template id={}", nextVersion, saved.getId());
        } catch (Exception e) {
            log.warn("[Portal Template] Không thể lưu snapshot phiên bản template: {}", e.getMessage());
        }

        return PortalTemplateDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PortalTemplateVersionDto> getTemplateVersions(Long templateId) {
        return versionRepository.findByTemplateIdOrderByVersionNumberDesc(templateId).stream()
                .map(PortalTemplateVersionDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Khôi phục template về một phiên bản lịch sử.
     */
    @Transactional
    public PortalTemplateDto rollbackToVersion(Long templateId, Integer versionNumber) {
        PortalTemplateVersion version = versionRepository.findByTemplateIdAndVersionNumber(templateId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản " + versionNumber + " của template id=" + templateId));

        try {
            PortalTemplateDto snapshotDto = objectMapper.readValue(version.getSnapshotJson(), PortalTemplateDto.class);
            snapshotDto.setId(templateId);
            PortalTemplateDto restored = saveOrUpdate(snapshotDto);
            log.info("[Portal Template] Đã khôi phục template id={} về phiên bản v{}", templateId, versionNumber);
            return restored;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khôi phục cấu hình từ phiên bản snapshot: " + e.getMessage(), e);
        }
    }
}

