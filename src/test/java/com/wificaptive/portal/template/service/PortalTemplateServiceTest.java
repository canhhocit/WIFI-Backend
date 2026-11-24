package com.wificaptive.portal.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.portal.template.dto.PortalTemplateDto;
import com.wificaptive.portal.template.entity.PortalTemplate;
import com.wificaptive.portal.template.entity.PortalTemplateVersion;
import com.wificaptive.portal.template.repository.PortalTemplateRepository;
import com.wificaptive.portal.template.repository.PortalTemplateVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalTemplateServiceTest {

    @Mock
    private PortalTemplateRepository templateRepository;

    @Mock
    private PortalTemplateVersionRepository versionRepository;

    private PortalTemplateService service;
    private PortalTemplate storedTemplate;
    private List<PortalTemplateVersion> storedVersions;

    @BeforeEach
    void setUp() {
        service = new PortalTemplateService(templateRepository, versionRepository, new ObjectMapper());
        storedVersions = new ArrayList<>();

        storedTemplate = new PortalTemplate();
        storedTemplate.setId(1L);
        storedTemplate.setName("Giao diện chuẩn");
        storedTemplate.setTemplateType("CULTURAL");
        storedTemplate.setThemeColor("#ee0033");
        storedTemplate.setBackgroundColor("#14141e");
        storedTemplate.setBackgroundImageUrl("");
        storedTemplate.setTitle("Cổng Wi-Fi Hải Phòng");
        storedTemplate.setButtonText("Kết nối ngay");
        storedTemplate.setActive(true);

        when(templateRepository.findById(1L)).thenAnswer(inv -> Optional.of(storedTemplate));
        when(templateRepository.saveAndFlush(any(PortalTemplate.class))).thenAnswer(inv -> {
            storedTemplate = inv.getArgument(0);
            return storedTemplate;
        });
        when(versionRepository.findMaxVersionNumber(1L)).thenAnswer(inv ->
                storedVersions.stream().mapToInt(PortalTemplateVersion::getVersionNumber).max().orElse(0));
        when(versionRepository.save(any(PortalTemplateVersion.class))).thenAnswer(inv -> {
            PortalTemplateVersion v = inv.getArgument(0);
            storedVersions.add(v);
            return v;
        });
        when(versionRepository.findByTemplateIdAndVersionNumber(any(), any())).thenAnswer(inv -> {
            Long tplId = inv.getArgument(0);
            Integer ver = inv.getArgument(1);
            return storedVersions.stream()
                    .filter(v -> v.getTemplateId().equals(tplId) && v.getVersionNumber().equals(ver))
                    .findFirst();
        });
    }

    @Test
    void firstSavePreservesOriginalAsV1AndRollbackRestoresAllFieldsIncludingEmptyBackgroundImage() {
        // Lần sửa 1: đổi sang màu xanh + thêm ảnh nền
        PortalTemplateDto update1 = PortalTemplateDto.from(storedTemplate);
        update1.setThemeColor("#659bcd");
        update1.setBackgroundColor("#0c2340");
        update1.setBackgroundImageUrl("/banners/central.jpg");
        update1.setButtonText("Kết nối v2");

        PortalTemplateDto saved1 = service.saveOrUpdate(update1);
        assertThat(saved1.getThemeColor()).isEqualTo("#659bcd");
        assertThat(storedVersions).hasSize(2);
        assertThat(storedVersions.get(0).getVersionNumber()).isEqualTo(1);
        assertThat(storedVersions.get(0).getVersionName()).contains("Bản gốc ban đầu");
        assertThat(storedVersions.get(1).getVersionNumber()).isEqualTo(2);

        // Khôi phục về v1 (bản gốc ban đầu: #ee0033, backgroundImageUrl rỗng)
        PortalTemplateDto restored = service.rollbackToVersion(1L, 1);

        assertThat(restored.getThemeColor()).isEqualTo("#ee0033");
        assertThat(restored.getBackgroundColor()).isEqualTo("#14141e");
        assertThat(restored.getBackgroundImageUrl()).isEqualTo("");
        assertThat(restored.getButtonText()).isEqualTo("Kết nối ngay");
        assertThat(storedVersions).hasSize(3);
        assertThat(storedVersions.get(2).getVersionNumber()).isEqualTo(3);
        assertThat(storedVersions.get(2).getVersionName()).contains("Khôi phục từ v1");
    }
}
