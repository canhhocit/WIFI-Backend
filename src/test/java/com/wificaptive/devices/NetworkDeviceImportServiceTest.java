package com.wificaptive.devices;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.devices.dto.NetworkDeviceImportResultDto;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.devices.service.NetworkDeviceImportService;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NetworkDeviceImportServiceTest {

    private NetworkDeviceRepository repo;
    private WifiLocationRepository locRepo;
    private NetworkDeviceImportService service;
    private final List<NetworkDevice> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repo = mock(NetworkDeviceRepository.class);
        locRepo = mock(WifiLocationRepository.class);
        service = new NetworkDeviceImportService(repo, locRepo, mock(AuditService.class));

        WifiLocation loc = new WifiLocation();
        loc.setId(1L);
        loc.setLocationCode("NVH-0001");
        when(locRepo.findByLocationCodeAndDeletedFalse("NVH-0001")).thenReturn(Optional.of(loc));
        when(locRepo.findByLocationCodeAndDeletedFalse("NVH-XXXX")).thenReturn(Optional.empty());
        when(repo.findByMacAddress(anyString())).thenReturn(Optional.empty());
        when(repo.findBySerialNumber(anyString())).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> { saved.add(inv.getArgument(0)); return inv.getArgument(0); });
    }

    private static byte[] xlsx(String[]... rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet();
            for (int r = 0; r < rows.length; r++) {
                Row row = sh.createRow(r);
                for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void importsPackingListWithVietnameseHeadersInAnyOrder() throws Exception {
        byte[] file = xlsx(
                new String[]{"Số Serial", "Địa chỉ MAC", "Mã điểm", "IP quản trị"},
                new String[]{"VTGRB3000001", "240b2b000001", "NVH-0001", "192.168.80.111"},
                new String[]{"VTGRB3000002", "24-0B-2B-00-00-02", "NVH-0001", ""});

        NetworkDeviceImportResultDto r = service.importFile(new ByteArrayInputStream(file), "list.xlsx", false, "admin");

        assertEquals(2, r.totalRows());
        assertEquals(2, r.created());
        assertTrue(r.errors().isEmpty());
        assertEquals("24:0B:2B:00:00:01", saved.get(0).getMacAddress());
        assertEquals("VTGRB3000001", saved.get(0).getSerialNumber());
        assertEquals("ACCESS_POINT", saved.get(0).getDeviceType());
        assertEquals("vAP-32X6V1", saved.get(0).getModel());
        assertEquals("192.168.80.111", saved.get(0).getIpAddress());
        assertEquals(NetworkDevice.CRED_FACTORY, saved.get(0).getMeshCredentialState());
    }

    @Test
    void reportsRowErrors() throws Exception {
        String csv = String.join("\n",
                "MAC,Serial,Ma diem",
                "zz:zz,VT1,NVH-0001",
                "24:0B:2B:00:00:03,,NVH-0001",
                "24:0B:2B:00:00:04,VT4,NVH-XXXX",
                "24:0B:2B:00:00:05,VT5,NVH-0001",
                "24:0B:2B:00:00:05,VT6,NVH-0001");

        NetworkDeviceImportResultDto r = service.importFile(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "list.csv", false, "admin");

        assertEquals(1, r.created());
        assertEquals(4, r.errors().size());
        assertEquals(2, r.errors().get(0).row());
        assertTrue(r.errors().get(1).message().contains("S/N"));
        assertTrue(r.errors().get(2).message().contains("mã điểm"));
        assertTrue(r.errors().get(3).message().contains("trùng"));
    }

    @Test
    void updatesExistingDeviceSerialAndResetsCredential() throws Exception {
        NetworkDevice existing = new NetworkDevice();
        existing.setId(9L);
        existing.setMacAddress("24:0B:2B:47:2A:22");
        existing.setSerialNumber("240B2B472A22");
        existing.setDeviceType("ACCESS_POINT");
        existing.setMeshCredentialState(NetworkDevice.CRED_MANAGED);
        when(repo.findByMacAddress("24:0B:2B:47:2A:22")).thenReturn(Optional.of(existing));

        String csv = "MAC,S/N\n24:0B:2B:47:2A:22,VTGRB3NEW0001\n";
        NetworkDeviceImportResultDto r = service.importFile(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "a.csv", false, "admin");

        assertEquals(1, r.updated());
        assertEquals("VTGRB3NEW0001", existing.getSerialNumber());
        assertEquals(NetworkDevice.CRED_FACTORY, existing.getMeshCredentialState());
    }

    @Test
    void dryRunDoesNotSave() throws Exception {
        String csv = "MAC,Serial,Ma diem\n24:0B:2B:00:00:09,VT9,NVH-0001\n";
        NetworkDeviceImportResultDto r = service.importFile(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "a.csv", true, "admin");
        assertTrue(r.dryRun());
        assertEquals(1, r.created());
        verify(repo, never()).save(any());
    }
}
