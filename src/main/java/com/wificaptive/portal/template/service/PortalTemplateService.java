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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        boolean existedBefore = false;
        if (dto.getId() != null) {
            t = repository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("PortalTemplate not found with id: " + dto.getId()));
            existedBefore = true;
        } else {
            t = new PortalTemplate();
        }

        String actor = resolveCurrentActor();

        // Bảo toàn trạng thái gốc trước lần chỉnh sửa đầu tiên nếu chưa từng có snapshot lịch sử
        if (existedBefore && versionRepository.findMaxVersionNumber(t.getId()) == 0) {
            saveVersionSnapshot(t, "Bản gốc ban đầu (" + t.getName() + ")", actor);
        }

        applyDto(t, dto, false);
        PortalTemplate saved = repository.saveAndFlush(t);

        // Lưu snapshot phiên bản sau khi cập nhật
        saveVersionSnapshot(saved, null, actor);

        return PortalTemplateDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PortalTemplateVersionDto> getTemplateVersions(Long templateId) {
        return versionRepository.findByTemplateIdOrderByVersionNumberDesc(templateId).stream()
                .map(PortalTemplateVersionDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Khôi phục template về nguyên vẹn một phiên bản lịch sử.
     */
    @Transactional
    public PortalTemplateDto rollbackToVersion(Long templateId, Integer versionNumber) {
        PortalTemplate t = repository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("PortalTemplate not found with id: " + templateId));

        PortalTemplateVersion version = versionRepository.findByTemplateIdAndVersionNumber(templateId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản " + versionNumber + " của template id=" + templateId));

        try {
            PortalTemplateDto snapshotDto = objectMapper.readValue(version.getSnapshotJson(), PortalTemplateDto.class);
            applyDto(t, snapshotDto, true);
            PortalTemplate saved = repository.saveAndFlush(t);

            String actor = resolveCurrentActor();
            String restoreLabel = "Khôi phục từ v" + versionNumber + " (" + saved.getName() + ")";
            saveVersionSnapshot(saved, restoreLabel, actor);

            log.info("[Portal Template] Đã khôi phục template id={} về phiên bản v{} (themeColor={}, bgColor={})",
                    templateId, versionNumber, saved.getThemeColor(), saved.getBackgroundColor());
            return PortalTemplateDto.from(saved);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khôi phục cấu hình từ phiên bản snapshot: " + e.getMessage(), e);
        }
    }

    private void applyDto(PortalTemplate t, PortalTemplateDto dto, boolean exactRestore) {
        t.setName(dto.getName());
        t.setTemplateType(dto.getTemplateType() != null ? dto.getTemplateType() : "DEFAULT");
        t.setThemeColor(dto.getThemeColor());
        t.setBackgroundColor(dto.getBackgroundColor());
        if (exactRestore || dto.getBackgroundImageUrl() != null) {
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
    }

    private void saveVersionSnapshot(PortalTemplate template, String customVersionName, String actor) {
        try {
            int nextVersion = versionRepository.findMaxVersionNumber(template.getId()) + 1;
            String snapshotJson = objectMapper.writeValueAsString(PortalTemplateDto.from(template));
            String versionName = (customVersionName != null && !customVersionName.isBlank())
                    ? customVersionName
                    : ("Phiên bản v" + nextVersion + " (" + template.getName() + ")");
            versionRepository.save(new PortalTemplateVersion(template.getId(), nextVersion, versionName, snapshotJson, actor));
            log.info("[Portal Template] Đã lưu snapshot phiên bản v{} cho template id={}", nextVersion, template.getId());
        } catch (Exception e) {
            log.warn("[Portal Template] Không thể lưu snapshot phiên bản template: {}", e.getMessage());
        }
    }

    private String resolveCurrentActor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return "admin";
    }
}

