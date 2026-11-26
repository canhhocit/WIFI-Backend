package com.wificaptive.locations;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.locations.dto.excel.ExcelImportSummaryDto;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.locations.service.LocationExcelService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
            if ("HP".equals(code)) {
                return Optional.of(new AdministrativeUnit(code, "Thành phố Hải Phòng", 1, "/HP/"));
            }
            return Optional.of(new AdministrativeUnit(code, "Đơn vị " + code, 2, "/HP/" + code + "/"));
        });
    }

    @Test
    void testGenerateTemplateAndValidate_success() throws Exception {
        byte[] templateBytes = excelService.generateTemplate();
        assertThat(templateBytes).isNotEmpty();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(templateBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("HP_P_HONGBANG");
            assertThat(sheet.getRow(2).getCell(3).getStringCellValue()).isEqualTo("HP_P_NGOQUYEN");
            assertThat(sheet.getRow(3).getCell(3).getStringCellValue()).isEqualTo("HP_DZ_CATHAI");
        }

        // Validate the generated template
        ExcelImportSummaryDto summary = excelService.validateExcel(new ByteArrayInputStream(templateBytes));

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalRows()).isEqualTo(3);
        assertThat(summary.getValidRows()).isEqualTo(3);
        assertThat(summary.getErrorRows()).isEqualTo(0);
        assertThat(summary.getImportSessionId()).isNotBlank();
    }

    @Test
    void testCommitImport_savesLocationsAndDevices() {
        byte[] templateBytes = excelService.generateTemplate();
        ExcelImportSummaryDto summary = excelService.validateExcel(new ByteArrayInputStream(templateBytes));
        when(locationRepository.save(any(WifiLocation.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = excelService.commitImport(summary.getImportSessionId(), "admin");

        assertThat(count).isEqualTo(3);
        verify(locationRepository, times(3)).save(any(WifiLocation.class));
        verify(deviceRepository, times(6)).save(any());
    }

    @Test
    void testExportAllLocations_usesFetchJoinAndWritesValidWorkbook() throws Exception {
        AdministrativeUnit unit = new AdministrativeUnit("HP_DZ_CATHAI", "Đặc khu Cát Hải", 2, "/HP/HP_DZ_CATHAI/");
        WifiLocation loc = new WifiLocation();
        loc.setLocationCode("HP-CH-NVH03");
        loc.setName("Nhà văn hóa Đặc khu Cát Hải");
        loc.setSlug("hp-ch-nvh03");
        loc.setAdminUnit(unit);
        loc.setAddress("Khu 3, Đặc khu Cát Hải");
        loc.setLatitude(20.7275);
        loc.setLongitude(107.0494);
        loc.setStatus("ACTIVE");
        loc.setIspName("Viettel");

        when(locationRepository.findAllWithAdminUnitAndDeletedFalse()).thenReturn(List.of(loc));

        byte[] bytes = excelService.exportAllLocations();
        assertThat(bytes).isNotEmpty();

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            Row row = sheet.getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("HP-CH-NVH03");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("Đặc khu Cát Hải");
        }
    }

    @Test
    void testValidateExcel_acceptsVietnameseDecimalCommaAndRejectsLevel1() throws Exception {
        byte[] bytes;
        try (Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Test");
            sheet.createRow(0).createCell(0).setCellValue("Mã điểm");

            Row validCommaRow = sheet.createRow(1);
            validCommaRow.createCell(0).setCellValue("HP-TEST-01");
            validCommaRow.createCell(1).setCellValue("NVH Test");
            validCommaRow.createCell(2).setCellValue("hp-test-01");
            validCommaRow.createCell(3).setCellValue("HP_P_HONGBANG");
            validCommaRow.createCell(5).setCellValue("20,865139");
            validCommaRow.createCell(6).setCellValue("106,683830");

            Row level1Row = sheet.createRow(2);
            level1Row.createCell(0).setCellValue("HP-TEST-02");
            level1Row.createCell(1).setCellValue("NVH Level 1");
            level1Row.createCell(2).setCellValue("hp-test-02");
            level1Row.createCell(3).setCellValue("HP");
            level1Row.createCell(5).setCellValue(20.865);
            level1Row.createCell(6).setCellValue(106.683);

            wb.write(out);
            bytes = out.toByteArray();
        }

        ExcelImportSummaryDto summary = excelService.validateExcel(new ByteArrayInputStream(bytes));
        assertThat(summary.getTotalRows()).isEqualTo(2);
        assertThat(summary.getValidRows()).isEqualTo(1);
        assertThat(summary.getErrorRows()).isEqualTo(1);
        assertThat(summary.getRowResults().get(1).getErrors()).anyMatch(e -> e.contains("Phường / Xã / Đặc khu"));
    }
}

