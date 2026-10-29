package com.wificaptive.network.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.network.dto.CreateDnsDomainRuleRequest;
import com.wificaptive.network.dto.DnsDomainRuleDto;
import com.wificaptive.network.entity.DnsDomainRule;
import com.wificaptive.network.repository.DnsDomainRuleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DnsDomainRuleService {

    private final DnsDomainRuleRepository repository;
    private final AuditService auditService;

    public DnsDomainRuleService(DnsDomainRuleRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<DnsDomainRuleDto> getRules(String ruleType, String search, Pageable pageable) {
        Page<DnsDomainRule> page;
        boolean hasType = ruleType != null && !ruleType.trim().isEmpty() && !"ALL".equalsIgnoreCase(ruleType.trim());
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (hasType && hasSearch) {
            page = repository.findByRuleTypeAndDomainNameContainingIgnoreCase(ruleType.trim().toUpperCase(), search.trim(), pageable);
        } else if (hasType) {
            page = repository.findByRuleType(ruleType.trim().toUpperCase(), pageable);
        } else if (hasSearch) {
            page = repository.findByDomainNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            page = repository.findAll(pageable);
        }

        return page.map(DnsDomainRuleDto::from);
    }

    @Transactional(readOnly = true)
    public List<DnsDomainRuleDto> getActiveWhitelist() {
        return repository.findByRuleTypeAndActiveTrue("WHITELIST").stream()
                .map(DnsDomainRuleDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DnsDomainRuleDto> getActiveBlocklist() {
        return repository.findByRuleTypeAndActiveTrue("BLOCKLIST").stream()
                .map(DnsDomainRuleDto::from)
                .toList();
    }

    @Transactional
    public DnsDomainRuleDto createRule(CreateDnsDomainRuleRequest request, String currentUsername) {
        String domain = request.getDomainName().trim().toLowerCase();
        if (repository.existsByDomainName(domain)) {
            throw new BadRequestException("Tên miền '" + domain + "' đã tồn tại trong quy tắc lọc DNS");
        }

        DnsDomainRule rule = new DnsDomainRule();
        rule.setDomainName(domain);
        rule.setRuleType(request.getRuleType().trim().toUpperCase());
        rule.setCategory(request.getCategory());
        rule.setDescription(request.getDescription());
        rule.setActive(request.getActive() != null ? request.getActive() : true);

        DnsDomainRule saved = repository.save(rule);

        auditService.recordEvent(
                AuditAction.DNS_RULE_CREATE,
                currentUsername,
                "SYSTEM",
                "DNS_RULE",
                saved.getId().toString(),
                "Thêm quy tắc DNS " + saved.getRuleType() + " cho tên miền: " + saved.getDomainName()
        );

        return DnsDomainRuleDto.from(saved);
    }

    @Transactional
    public DnsDomainRuleDto toggleRule(Long id, String currentUsername) {
        DnsDomainRule rule = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DnsDomainRule", "id", id));

        rule.setActive(!rule.isActive());
        DnsDomainRule saved = repository.save(rule);

        auditService.recordEvent(
                AuditAction.DNS_RULE_UPDATE,
                currentUsername,
                "SYSTEM",
                "DNS_RULE",
                saved.getId().toString(),
                "Chuyển trạng thái quy tắc DNS: " + saved.getDomainName() + " -> " + (saved.isActive() ? "Kích hoạt" : "Tạm dừng")
        );

        return DnsDomainRuleDto.from(saved);
    }

    @Transactional
    public void deleteRule(Long id, String currentUsername) {
        DnsDomainRule rule = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DnsDomainRule", "id", id));

        repository.delete(rule);

        auditService.recordEvent(
                AuditAction.DNS_RULE_DELETE,
                currentUsername,
                "SYSTEM",
                "DNS_RULE",
                id.toString(),
                "Xóa quy tắc lọc DNS cho tên miền: " + rule.getDomainName()
        );
    }
}

