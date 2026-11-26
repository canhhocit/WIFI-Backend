package com.wificaptive.locations.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.geography.service.AdminScopeService;
import com.wificaptive.locations.dto.excel.ExcelImportSummaryDto;
import com.wificaptive.locations.dto.excel.ExcelRowValidationResult;
import com.wificaptive.locations.dto.excel.LocationImportRowData;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private AdminScopeService adminScopeService;

    // In-memory cache for validated import sessions (SessionId -> List<LocationImportRowData>)
    private final Map<String, List<LocationImportRowData>> validRowsCache = new ConcurrentHashMap<>();
    private final Map<String, List<ExcelRowValidationResult>> errorRowsCache = new ConcurrentHashMap<>();

    @Autowired
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
                data.setIspName(isp != null ? isp : "Viettel");
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
                    if (code != null && !code.isBlank()) data.setSlug(code.trim().toLowerCase().replace("_", "-"));
                    else result.addError("Slug không được để trống");
                }
                if (adminUnitCode == null || adminUnitCode.isBlank()) result.addError("Mã đơn vị địa bàn không được để trống");

                // Criteria 2: Uniqueness
                if (code != null && !code.isBlank()) {
                    String normalizedCode = code.trim().toUpperCase();
                    if (seenCodesInFile.contains(normalizedCode)) {
                        result.addError("Mã điểm bị trùng lặp trong chính file Excel: " + code);
                    } else if (locationRepository.existsByLocationCodeAndDeletedFalse(normalizedCode)) {
                        result.addError("Mã điểm đã tồn tại trong cơ sở dữ liệu: " + code);
                    }
                    seenCodesInFile.add(normalizedCode);
                }

                if (data.getSlug() != null && !data.getSlug().isBlank()) {
                    String normalizedSlug = data.getSlug().trim().toLowerCase();
                    if (seenSlugsInFile.contains(normalizedSlug)) {
                        result.addError("Slug bị trùng lặp trong file: " + data.getSlug());
                    } else if (locationRepository.existsBySlugAndDeletedFalse(normalizedSlug)) {
                        result.addError("Slug đã tồn tại trong CSDL: " + data.getSlug());
                    }
                    seenSlugsInFile.add(normalizedSlug);
                }

                // Criteria 3: Administrative Unit Integrity (2-tier: Thành phố -> Phường/Xã/Đặc khu)
                Set<Long> scopedUnitIds = adminScopeService != null ? adminScopeService.getCurrentUserScopedUnitIds() : null;
                if (adminUnitCode != null && !adminUnitCode.isBlank()) {
                    Optional<AdministrativeUnit> unitOpt = adminUnitRepository.findByCode(adminUnitCode.trim().toUpperCase());
                    if (unitOpt.isEmpty()) {
                        result.addError("Mã địa bàn không tồn tại trong Cây địa bàn Hải Phòng: " + adminUnitCode);
                    } else {
                        AdministrativeUnit unit = unitOpt.get();
                        if (!"ACTIVE".equalsIgnoreCase(unit.getStatus())) {
                            result.addError("Đơn vị địa bàn không còn hiệu lực: " + adminUnitCode);
                        } else if (unit.getLevel() != null && unit.getLevel() < 2) {
                            result.addError("Vui lòng dùng mã địa bàn cấp Phường / Xã / Đặc khu thay vì cấp Thành phố: " + adminUnitCode);
                        } else if (scopedUnitIds != null && !scopedUnitIds.contains(unit.getId())) {
                            result.addError("Mã địa bàn không thuộc phạm vi quản lý của tài khoản: " + adminUnitCode);
                        }
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
            String normalizedUnitCode = row.getAdminUnitCode() != null ? row.getAdminUnitCode().trim().toUpperCase() : "";
            AdministrativeUnit unit = adminUnitRepository.findByCode(normalizedUnitCode)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn vị địa bàn: " + row.getAdminUnitCode()));

            WifiLocation loc = new WifiLocation();
            loc.setLocationCode(row.getCode().trim().toUpperCase());
            loc.setName(row.getName().trim());
            loc.setSlug(row.getSlug().trim().toLowerCase());
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
            Sheet sheet = wb.createSheet("Diem_WiFi_HaiPhong");

            Row header = sheet.createRow(0);
            String[] headers = {
                    "Mã điểm", "Tên nhà văn hóa", "Slug (viết thường)", "Mã địa bàn (Phường/Xã/Đặc khu)",
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
                sheet.setColumnWidth(i, 24 * 256);
            }

            // Sample rows using post-merger 2-tier Hải Phòng administrative unit codes (114 Phường/Xã/Đặc khu)
            Object[][] sampleData = {
                    {"HP-HB-NVH01", "Nhà văn hóa Tổ dân phố số 1", "hp-hb-nvh01", "HP_P_HONGBANG", "Số 15 Hoàng Văn Thụ, Phường Hồng Bàng", 20.865139, 106.683830, "Viettel", "HP-VIETTEL-0012", "Nguyễn Văn An", "0912345678", "00:1A:2B:3C:4D:5E", "00:1A:2B:3C:4D:5F"},
                    {"HP-NQ-NVH02", "Nhà văn hóa Khu dân cư Máy Tơ", "hp-nq-nvh02", "HP_P_NGOQUYEN", "Số 42 Máy Tơ, Phường Ngô Quyền", 20.859210, 106.691230, "Viettel", "HP-VT-0045", "Trần Thị Bình", "0987654321", "00:1A:2B:3C:6A:11", "00:1A:2B:3C:6A:12"},
                    {"HP-CH-NVH03", "Nhà văn hóa Đặc khu Cát Hải", "hp-ch-nvh03", "HP_DZ_CATHAI", "Tổ dân phố 2, Đặc khu Cát Hải", 20.725830, 107.049440, "Viettel", "HP-VIETTEL-0089", "Lê Văn Cường", "0905123456", "00:1A:2B:3C:7B:21", "00:1A:2B:3C:7B:22"}
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

    @Transactional(readOnly = true)
    public byte[] exportAllLocations() {
        Set<Long> scopedUnitIds = adminScopeService != null ? adminScopeService.getCurrentUserScopedUnitIds() : null;
        List<WifiLocation> locations;
        if (scopedUnitIds != null) {
            locations = scopedUnitIds.isEmpty()
                    ? List.of()
                    : locationRepository.findByAdminUnit_IdInAndDeletedFalse(scopedUnitIds);
        } else {
            locations = locationRepository.findAllWithAdminUnitAndDeletedFalse();
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Danh_sach_Diem_WiFi");

            Row header = sheet.createRow(0);
            String[] headers = {
                    "Mã điểm", "Tên nhà văn hóa", "Slug", "Địa bàn", "Địa chỉ",
                    "Vĩ độ", "Kinh độ", "Trạng thái", "Nhà mạng", "Mã đường truyền",
                    "Người phụ trách", "Số điện thoại"
            };

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            int r = 1;
            for (WifiLocation loc : locations) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(loc.getLocationCode() != null ? loc.getLocationCode() : "");
                row.createCell(1).setCellValue(loc.getName() != null ? loc.getName() : "");
                row.createCell(2).setCellValue(loc.getSlug() != null ? loc.getSlug() : "");
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
        if (row.getFirstCellNum() < 0) return true;
        DataFormatter formatter = new DataFormatter();
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        String formatted = new DataFormatter().formatCellValue(cell).trim();
        return formatted.isEmpty() ? null : formatted;
    }

    private Double getCellDouble(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        String raw = new DataFormatter().formatCellValue(cell).trim().replace(',', '.');
        if (raw.isEmpty()) return null;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
