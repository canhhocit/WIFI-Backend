package com.wificaptive.locations.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.locations.dto.excel.ExcelImportSummaryDto;
import com.wificaptive.locations.dto.excel.ExcelRowValidationResult;
import com.wificaptive.locations.dto.excel.LocationImportRowData;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class LocationExcelService {

    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    private final WifiLocationRepository locationRepository;
    private final AdministrativeUnitRepository adminUnitRepository;
    private final NetworkDeviceRepository deviceRepository;
    private final AuditService auditService;

    // In-memory cache for validated import sessions (SessionId -> List<LocationImportRowData>)
    private final Map<String, List<LocationImportRowData>> validRowsCache = new ConcurrentHashMap<>();
    private final Map<String, List<ExcelRowValidationResult>> errorRowsCache = new ConcurrentHashMap<>();

    public LocationExcelService(WifiLocationRepository locationRepository,
                                AdministrativeUnitRepository adminUnitRepository,
                                NetworkDeviceRepository deviceRepository,
                                AuditService auditService) {
        this.locationRepository = locationRepository;
        this.adminUnitRepository = adminUnitRepository;
        this.deviceRepository = deviceRepository;
        this.auditService = auditService;
    }

    public ExcelImportSummaryDto validateExcel(InputStream inputStream) {
        ExcelImportSummaryDto summary = new ExcelImportSummaryDto();
        String sessionId = UUID.randomUUID().toString();
        summary.setImportSessionId(sessionId);

        List<LocationImportRowData> validRows = new ArrayList<>();
        List<ExcelRowValidationResult> errorRows = new ArrayList<>();
        Set<String> seenCodesInFile = new HashSet<>();
        Set<String> seenSlugsInFile = new HashSet<>();
        Set<String> seenMacsInFile = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = 0;

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int rowNum = r + 1; // 1-based display
                String code = getCellString(row, 0);
                String name = getCellString(row, 1);
                String slug = getCellString(row, 2);
                String adminUnitCode = getCellString(row, 3);
                String address = getCellString(row, 4);
                Double lat = getCellDouble(row, 5);
                Double lng = getCellDouble(row, 6);
                String isp = getCellString(row, 7);
                String circuitCode = getCellString(row, 8);
                String contactName = getCellString(row, 9);
                String contactPhone = getCellString(row, 10);
                String routerMac = getCellString(row, 11);
                String apMac = getCellString(row, 12);

                ExcelRowValidationResult result = new ExcelRowValidationResult(rowNum, code, name);
                LocationImportRowData data = new LocationImportRowData();
                data.setRowNumber(rowNum);
                data.setCode(code);
                data.setName(name);
                data.setSlug(slug);
                data.setAdminUnitCode(adminUnitCode);
                data.setAddress(address);
                data.setLatitude(lat);
                data.setLongitude(lng);
                data.setIspName(isp != null ? isp : "VNPT");
                data.setCircuitCode(circuitCode);
                data.setContactName(contactName);
                data.setContactPhone(contactPhone);
                data.setRouterMac(routerMac);
                data.setApMac(apMac);
                result.setData(data);

                // Criteria 1: Mandatory Fields
                if (code == null || code.isBlank()) result.addError("Mã điểm phát sóng không được để trống");
                if (name == null || name.isBlank()) result.addError("Tên nhà văn hóa không được để trống");
                if (slug == null || slug.isBlank()) {
                    if (code != null) data.setSlug(code.toLowerCase().replace("_", "-"));
                    else result.addError("Slug không được để trống");
                }
                if (adminUnitCode == null || adminUnitCode.isBlank()) result.addError("Mã đơn vị địa bàn không được để trống");

                // Criteria 2: Uniqueness
                if (code != null && !code.isBlank()) {
                    if (seenCodesInFile.contains(code.toUpperCase())) {
                        result.addError("Mã điểm bị trùng lặp trong chính file Excel: " + code);
                    } else if (locationRepository.existsByLocationCode(code.toUpperCase())) {
                        result.addError("Mã điểm đã tồn tại trong cơ sở dữ liệu: " + code);
                    }
                    seenCodesInFile.add(code.toUpperCase());
                }

                if (data.getSlug() != null) {
                    if (seenSlugsInFile.contains(data.getSlug().toLowerCase())) {
                        result.addError("Slug bị trùng lặp trong file: " + data.getSlug());
                    } else if (locationRepository.existsBySlug(data.getSlug().toLowerCase())) {
                        result.addError("Slug đã tồn tại trong CSDL: " + data.getSlug());
                    }
                    seenSlugsInFile.add(data.getSlug().toLowerCase());
                }

                // Criteria 3: Administrative Unit Integrity
                if (adminUnitCode != null && !adminUnitCode.isBlank()) {
                    Optional<AdministrativeUnit> unitOpt = adminUnitRepository.findByCode(adminUnitCode.trim().toUpperCase());
                    if (unitOpt.isEmpty()) {
                        result.addError("Mã địa bàn không tồn tại trong Cây địa bàn Hải Phòng: " + adminUnitCode);
                    }
                }

                // Criteria 4: Geo-fencing Hải Phòng bounds [20.50, 21.15] lat, [106.40, 107.15] lng
                if (lat != null && lng != null) {
                    if (lat < 20.50 || lat > 21.15 || lng < 106.40 || lng > 107.15) {
                        result.addError("Tọa độ (" + lat + ", " + lng + ") nằm ngoài phạm vi thành phố Hải Phòng (Vĩ độ: 20.50-21.15, Kinh độ: 106.40-107.15)");
                    }
                } else {
                    result.addError("Tọa độ Vĩ độ và Kinh độ không được để trống");
                }

                // Criteria 5: MAC address validation
                if (routerMac != null && !routerMac.isBlank()) {
                    if (!MAC_PATTERN.matcher(routerMac.trim()).matches()) {
                        result.addError("Địa chỉ MAC Router không đúng định dạng (XX:XX:XX:XX:XX:XX): " + routerMac);
                    } else {
                        String macClean = routerMac.trim().toUpperCase();
                        if (seenMacsInFile.contains(macClean)) {
                            result.addError("MAC Router bị trùng lặp trong file: " + routerMac);
                        }
                        seenMacsInFile.add(macClean);
                    }
                }

                if (apMac != null && !apMac.isBlank()) {
                    if (!MAC_PATTERN.matcher(apMac.trim()).matches()) {
                        result.addError("Địa chỉ MAC AP không đúng định dạng (XX:XX:XX:XX:XX:XX): " + apMac);
                    } else {
                        String macClean = apMac.trim().toUpperCase();
                        if (seenMacsInFile.contains(macClean)) {
                            result.addError("MAC AP bị trùng lặp trong file: " + apMac);
                        }
                        seenMacsInFile.add(macClean);
                    }
                }

                summary.getRowResults().add(result);
                if (result.isValid()) {
                    validRows.add(data);
                } else {
                    errorRows.add(result);
                }
            }

            summary.setTotalRows(totalRows);
            summary.setValidRows(validRows.size());
            summary.setErrorRows(errorRows.size());

            validRowsCache.put(sessionId, validRows);
            errorRowsCache.put(sessionId, errorRows);

        } catch (IOException e) {
            throw new BadRequestException("Lỗi đọc file Excel: " + e.getMessage());
        }

        return summary;
    }

    @Transactional
    public int commitImport(String importSessionId, String actorUsername) {
        List<LocationImportRowData> validRows = validRowsCache.get(importSessionId);
        if (validRows == null || validRows.isEmpty()) {
            throw new BadRequestException("Không tìm thấy dữ liệu phiên import hoặc không có dòng hợp lệ");
        }

        int count = 0;
        for (LocationImportRowData row : validRows) {
            AdministrativeUnit unit = adminUnitRepository.findByCode(row.getAdminUnitCode().toUpperCase())
                    .orElse(null);

            WifiLocation loc = new WifiLocation();
            loc.setLocationCode(row.getCode().toUpperCase());
            loc.setName(row.getName());
            loc.setSlug(row.getSlug().toLowerCase());
            loc.setAdminUnit(unit);
            loc.setAddress(row.getAddress());
            loc.setLatitude(row.getLatitude());
            loc.setLongitude(row.getLongitude());
            loc.setIspName(row.getIspName());
            loc.setCircuitCode(row.getCircuitCode());
            loc.setContactName(row.getContactName());
            loc.setContactPhone(row.getContactPhone());
            loc.setTermsAndConditions("Điều khoản và điều kiện sử dụng mạng Wi-Fi công cộng thành phố Hải Phòng.");
            loc.setWelcomeMessage("Chào mừng bạn đến với Wi-Fi công cộng " + row.getName());
            loc.setActive(true);
            loc.setStatus("ACTIVE");

            WifiLocation savedLoc = locationRepository.save(loc);

            // Register Router device if provided
            if (row.getRouterMac() != null && !row.getRouterMac().isBlank()) {
                registerDevice(savedLoc, "ROUTER", row.getCode() + "-RTR", row.getRouterMac(), "Mikrotik");
            }

            // Register Access Point device if provided
            if (row.getApMac() != null && !row.getApMac().isBlank()) {
                registerDevice(savedLoc, "ACCESS_POINT", row.getCode() + "-AP", row.getApMac(), "Ruijie");
            }

            count++;
        }

        auditService.recordEvent(
                AuditAction.LOCATION_CREATE,
                actorUsername != null ? actorUsername : "system",
                "internal",
                "LOCATION_BATCH_IMPORT",
                importSessionId,
                "Đã nhập thành công " + count + " điểm Nhà văn hóa từ file Excel"
        );

        validRowsCache.remove(importSessionId);
        errorRowsCache.remove(importSessionId);

        return count;
    }

    private void registerDevice(WifiLocation location, String type, String serial, String mac, String vendor) {
        String cleanMac = mac.trim().toUpperCase();
        if (!deviceRepository.existsByMacAddress(cleanMac)) {
            NetworkDevice dev = new NetworkDevice();
            dev.setLocation(location);
            dev.setDeviceType(type);
            dev.setSerialNumber(serial);
            dev.setMacAddress(cleanMac);
            dev.setVendor(vendor);
            dev.setModel(type.equals("ROUTER") ? "RB750Gr3" : "RG-RAP2200");
            dev.setStatus("ONLINE");
            dev.setLastSeenAt(Instant.now());
            dev.setInstalledAt(Instant.now());
            deviceRepository.save(dev);
        }
    }

    public byte[] generateTemplate() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("1584_Diem_WiFi_HaiPhong");

            Row header = sheet.createRow(0);
            String[] headers = {
                    "Mã điểm", "Tên nhà văn hóa", "Slug (viết thường)", "Mã địa bàn (Cấp 3)",
                    "Địa chỉ chi tiết", "Vĩ độ (Latitude)", "Kinh độ (Longitude)", "Nhà mạng (ISP)",
                    "Mã đường truyền", "Người phụ trách", "SĐT phụ trách", "MAC Router", "MAC AP"
            };

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Sample rows
            Object[][] sampleData = {
                    {"HP-HB-NVH01", "Nhà văn hóa Tổ dân phố số 1", "hp-hb-nvh01", "HP_HB_HVTHU", "Số 15 Hoàng Văn Thụ, Hồng Bàng", 20.865139, 106.683830, "VNPT", "HP-VNPT-0012", "Nguyễn Văn An", "0912345678", "00:1A:2B:3C:4D:5E", "00:1A:2B:3C:4D:5F"},
                    {"HP-NQ-NVH02", "Nhà văn hóa Khu dân cư Máy Tơ", "hp-nq-nvh02", "HP_NQ_MAYTO", "Số 42 Máy Tơ, Ngô Quyền", 20.859210, 106.691230, "Viettel", "HP-VT-0045", "Trần Thị Bình", "0987654321", "00:1A:2B:3C:6A:11", "00:1A:2B:3C:6A:12"},
                    {"HP-CH-NVH03", "Nhà văn hóa Thị trấn Cát Bà", "hp-ch-nvh03", "HP_CH_CATBA", "Tổ dân phố 2, Cát Bà, Cát Hải", 20.725830, 107.049440, "VNPT", "HP-VNPT-0089", "Lê Văn Cường", "0905123456", "00:1A:2B:3C:7B:21", "00:1A:2B:3C:7B:22"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = sampleData[r][c];
                    if (val instanceof String s) cell.setCellValue(s);
                    else if (val instanceof Double d) cell.setCellValue(d);
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Lỗi tạo file mẫu Excel: " + e.getMessage());
        }
    }

    public byte[] exportErrors(String importSessionId) {
        List<ExcelRowValidationResult> errors = errorRowsCache.get(importSessionId);
        if (errors == null) errors = Collections.emptyList();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Danh_sach_dong_loi");

            Row header = sheet.createRow(0);
            String[] headers = {"Dòng lỗi", "Mã điểm", "Tên điểm", "Chi tiết các lỗi phát hiện"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int r = 1;
            for (ExcelRowValidationResult item : errors) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(item.getRowNumber());
                row.createCell(1).setCellValue(item.getCode() != null ? item.getCode() : "");
                row.createCell(2).setCellValue(item.getName() != null ? item.getName() : "");
                row.createCell(3).setCellValue(String.join("; ", item.getErrors()));
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Lỗi xuất file lỗi Excel: " + e.getMessage());
        }
    }

    public byte[] exportAllLocations() {
        List<WifiLocation> locations = locationRepository.findAll();
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Danh_sach_1584_Diem_WiFi");

            Row header = sheet.createRow(0);
            String[] headers = {
                    "Mã điểm", "Tên nhà văn hóa", "Slug", "Địa bàn", "Địa chỉ",
                    "Vĩ độ", "Kinh độ", "Trạng thái", "Nhà mạng", "Mã đường truyền",
                    "Người phụ trách", "Số điện thoại"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int r = 1;
            for (WifiLocation loc : locations) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(loc.getLocationCode() != null ? loc.getLocationCode() : "");
                row.createCell(1).setCellValue(loc.getName());
                row.createCell(2).setCellValue(loc.getSlug());
                row.createCell(3).setCellValue(loc.getAdminUnit() != null ? loc.getAdminUnit().getName() : "");
                row.createCell(4).setCellValue(loc.getAddress() != null ? loc.getAddress() : "");
                row.createCell(5).setCellValue(loc.getLatitude() != null ? loc.getLatitude() : 0.0);
                row.createCell(6).setCellValue(loc.getLongitude() != null ? loc.getLongitude() : 0.0);
                row.createCell(7).setCellValue(loc.getStatus() != null ? loc.getStatus() : "ACTIVE");
                row.createCell(8).setCellValue(loc.getIspName() != null ? loc.getIspName() : "");
                row.createCell(9).setCellValue(loc.getCircuitCode() != null ? loc.getCircuitCode() : "");
                row.createCell(10).setCellValue(loc.getContactName() != null ? loc.getContactName() : "");
                row.createCell(11).setCellValue(loc.getContactPhone() != null ? loc.getContactPhone() : "");
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Lỗi xuất danh sách Excel: " + e.getMessage());
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return null;
    }

    private Double getCellDouble(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
