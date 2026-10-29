package com.wificaptive.settings.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.settings.dto.SystemSettingDto;
import com.wificaptive.settings.dto.UpdateSystemSettingRequest;
import com.wificaptive.settings.entity.SystemSetting;
import com.wificaptive.settings.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SystemSettingService {

    private final SystemSettingRepository repository;
    private final AuditService auditService;

    public SystemSettingService(SystemSettingRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SystemSettingDto> getAllSettings() {
        return repository.findAll().stream()
                .map(SystemSettingDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SystemSettingDto getSettingByKey(String key) {
        SystemSetting setting = repository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting", "key", key));
        return SystemSettingDto.from(setting);
    }

    @Transactional
    public SystemSettingDto updateSetting(String key, UpdateSystemSettingRequest request, String currentUsername) {
        SystemSetting setting = repository.findBySettingKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("SystemSetting", "key", key));

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(request.getSettingValue());
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }
        setting.setUpdatedAt(Instant.now());

        SystemSetting saved = repository.save(setting);

        auditService.recordEvent(
                AuditAction.SYSTEM_SETTING_UPDATE,
                currentUsername,
                "SYSTEM",
                "SYSTEM_SETTING",
                saved.getId().toString(),
                "Cập nhật tham số hệ thống: " + key + " từ [" + oldValue + "] sang [" + saved.getSettingValue() + "]"
        );

        return SystemSettingDto.from(saved);
    }
}

