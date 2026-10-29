package com.wificaptive.network;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.network.dto.CreateDnsDomainRuleRequest;
import com.wificaptive.network.dto.DnsDomainRuleDto;
import com.wificaptive.network.entity.DnsDomainRule;
import com.wificaptive.network.repository.DnsDomainRuleRepository;
import com.wificaptive.network.service.DnsDomainRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnsDomainRuleServiceTest {

    @Mock
    private DnsDomainRuleRepository repository;

    @Mock
    private AuditService auditService;

    private DnsDomainRuleService service;

    @BeforeEach
    void setUp() {
        service = new DnsDomainRuleService(repository, auditService);
    }

    @Test
    void testGetActiveWhitelist() {
        DnsDomainRule r1 = new DnsDomainRule("haiphong.gov.vn", "WHITELIST", "GOV", "City Portal", true);
        r1.setId(1L);
        when(repository.findByRuleTypeAndActiveTrue("WHITELIST")).thenReturn(List.of(r1));

        List<DnsDomainRuleDto> list = service.getActiveWhitelist();

        assertEquals(1, list.size());
        assertEquals("haiphong.gov.vn", list.get(0).getDomainName());
    }

    @Test
    void testCreateRule_Success() {
        CreateDnsDomainRuleRequest req = new CreateDnsDomainRuleRequest();
        req.setDomainName("dichvucong.haiphong.gov.vn");
        req.setRuleType("WHITELIST");
        req.setCategory("PUBLIC_SERVICE");
        req.setDescription("Public Service Portal");

        when(repository.existsByDomainName("dichvucong.haiphong.gov.vn")).thenReturn(false);
        DnsDomainRule saved = new DnsDomainRule("dichvucong.haiphong.gov.vn", "WHITELIST", "PUBLIC_SERVICE", "Public Service Portal", true);
        saved.setId(10L);
        when(repository.save(any(DnsDomainRule.class))).thenReturn(saved);

        DnsDomainRuleDto dto = service.createRule(req, "admin");

        assertNotNull(dto);
        assertEquals("dichvucong.haiphong.gov.vn", dto.getDomainName());
        assertEquals("WHITELIST", dto.getRuleType());
        verify(repository).save(any(DnsDomainRule.class));
    }

    @Test
    void testCreateRule_DuplicateThrows() {
        CreateDnsDomainRuleRequest req = new CreateDnsDomainRuleRequest();
        req.setDomainName("bet88.com");
        req.setRuleType("BLOCKLIST");

        when(repository.existsByDomainName("bet88.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.createRule(req, "admin"));
    }

    @Test
    void testToggleRule_Success() {
        DnsDomainRule r1 = new DnsDomainRule("bet88.com", "BLOCKLIST", "GAMBLING", "Gambling", true);
        r1.setId(5L);
        when(repository.findById(5L)).thenReturn(Optional.of(r1));
        when(repository.save(any(DnsDomainRule.class))).thenAnswer(i -> i.getArgument(0));

        DnsDomainRuleDto dto = service.toggleRule(5L, "admin");

        assertFalse(dto.isActive());
    }
}
