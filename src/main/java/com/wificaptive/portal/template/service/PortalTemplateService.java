package com.wificaptive.portal.template.service;

import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.portal.template.entity.PortalTemplate;
import com.wificaptive.portal.template.repository.PortalTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortalTemplateService {

    private final PortalTemplateRepository repository;

    public PortalTemplateService(PortalTemplateRepository repository) {
        this.repository = repository;
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
        t.setLogoUrl(dto.getLogoUrl());
        t.setBannerUrl(dto.getBannerUrl());
        t.setTitle(dto.getTitle());
        t.setWelcomeText(dto.getWelcomeText());
        t.setButtonText(dto.getButtonText());
        t.setTermsText(dto.getTermsText());
        t.setLayoutConfig(dto.getLayoutConfig());
        t.setActive(dto.isActive());

        return PortalTemplateDto.from(repository.save(t));
    }
}

