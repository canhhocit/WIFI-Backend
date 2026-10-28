package com.wificaptive.locations;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.locations.dto.excel.ExcelImportSummaryDto;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.locations.service.LocationExcelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationExcelServiceTest {

    @Mock
    private WifiLocationRepository locationRepository;

    @Mock
    private AdministrativeUnitRepository adminUnitRepository;

    @Mock
    private NetworkDeviceRepository deviceRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LocationExcelService excelService;

    @BeforeEach
    void setUp() {
        lenient().when(adminUnitRepository.findByCode(anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return Optional.of(new AdministrativeUnit(code, "Đơn vị " + code, 3, "/HP/" + code + "/"));
        });
    }

    @Test
    void testGenerateTemplateAndValidate_success() {
        byte[] templateBytes = excelService.generateTemplate();
        assertThat(templateBytes).isNotEmpty();

        // Validate the generated template
        ExcelImportSummaryDto summary = excelService.validateExcel(new ByteArrayInputStream(templateBytes));

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalRows()).isEqualTo(3);
        assertThat(summary.getValidRows()).isEqualTo(3);
        assertThat(summary.getErrorRows()).isEqualTo(0);
        assertThat(summary.getImportSessionId()).isNotBlank();
    }
}

