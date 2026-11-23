package com.wificaptive.devices.service;

import com.wificaptive.audit.entity.AuditAction;
import com.wificaptive.audit.service.AuditService;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.devices.dto.NetworkDeviceImportResultDto;
import com.wificaptive.devices.dto.NetworkDeviceImportResultDto.RowError;
import com.wificaptive.devices.entity.NetworkDevice;
import com.wificaptive.devices.repository.NetworkDeviceRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Import danh sách thiết bị bàn giao (packing list) — mỗi dòng một thiết bị, khóa theo MAC.
 * Dùng để nạp S/N + MAC cho hàng nghìn mesh AP mà không phải nhập tay; S/N là mật khẩu quản trị
 * gốc của mesh, hệ thống dùng nó cho lần đăng nhập đầu rồi đổi sang mật khẩu quản lý chung.
 *
 * Cột được nhận theo tên tiêu đề (không phân biệt hoa thường, có/không dấu):
 *   MAC (bắt buộc) | Serial / S/N (bắt buộc) | Loại thiết bị | Model | Hãng | IP quản trị | Mã điểm
 * Nếu không có dòng tiêu đề, các cột được hiểu theo đúng thứ tự trên.
 */
@Service
public class NetworkDeviceImportService {

    private static final Pattern MAC_HEX = Pattern.compile("^[0-9A-F]{12}$");
    private static final Pattern IPV4 = Pattern.compile("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)(:\\d{1,5})?$");
    private static final Set<String> TYPES = Set.of("ACCESS_POINT", "ROUTER", "SWITCH", "ONT");
    private static final int MAX_ROWS = 20000;

    enum Col { MAC, SERIAL, TYPE, MODEL, VENDOR, IP, LOCATION }

    private final NetworkDeviceRepository deviceRepository;
    private final WifiLocationRepository locationRepository;
    private final AuditService auditService;

    public NetworkDeviceImportService(NetworkDeviceRepository deviceRepository,
                                      WifiLocationRepository locationRepository,
                                      AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.locationRepository = locationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public NetworkDeviceImportResultDto importFile(InputStream in, String filename, boolean dryRun, String actor) {
        List<List<String>> rows = readRows(in, filename);
        if (rows.isEmpty()) {
            throw new BadRequestException("File không có dữ liệu");
        }
        if (rows.size() > MAX_ROWS) {
            throw new BadRequestException("File vượt quá " + MAX_ROWS + " dòng");
        }

        Map<Col, Integer> cols = detectHeader(rows.get(0));
        int start = cols != null ? 1 : 0;
        if (cols == null) cols = defaultColumns();

        List<RowError> errors = new ArrayList<>();
        Set<String> seenMacs = new HashSet<>();
        Set<String> seenSerials = new HashSet<>();
        Map<String, WifiLocation> locCache = new HashMap<>();
        int created = 0, updated = 0, unchanged = 0, total = 0;

        for (int i = start; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            int rowNo = i + 1;
            if (r.stream().allMatch(s -> s == null || s.isBlank())) continue;
            total++;

            String mac = normalizeMac(get(r, cols, Col.MAC));
            String serial = trimToNull(get(r, cols, Col.SERIAL));
            String rawMac = get(r, cols, Col.MAC);
            if (mac == null) { errors.add(new RowError(rowNo, rawMac, "MAC không hợp lệ")); continue; }
            if (serial == null) { errors.add(new RowError(rowNo, mac, "Thiếu số serial (S/N)")); continue; }
            if (serial.length() > 100) { errors.add(new RowError(rowNo, mac, "S/N quá dài")); continue; }
            if (!seenMacs.add(mac)) { errors.add(new RowError(rowNo, mac, "MAC bị trùng trong file")); continue; }
            if (!seenSerials.add(serial.toUpperCase(Locale.ROOT))) {
                errors.add(new RowError(rowNo, mac, "S/N bị trùng trong file")); continue;
            }

            String type = normalizeType(get(r, cols, Col.TYPE));
            if (type == null) { errors.add(new RowError(rowNo, mac, "Loại thiết bị không hợp lệ (AP/ROUTER/SWITCH/ONT)")); continue; }
            String model = trimToNull(get(r, cols, Col.MODEL));
            String vendor = trimToNull(get(r, cols, Col.VENDOR));
            String ip = trimToNull(get(r, cols, Col.IP));
            if (ip != null && !IPV4.matcher(ip).matches()) {
                errors.add(new RowError(rowNo, mac, "IP quản trị không hợp lệ: " + ip)); continue;
            }
            String locCode = trimToNull(get(r, cols, Col.LOCATION));
            WifiLocation location = null;
            if (locCode != null) {
                location = locCache.computeIfAbsent(locCode,
                        c -> locationRepository.findByLocationCodeAndDeletedFalse(c).orElse(null));
                if (location == null) { errors.add(new RowError(rowNo, mac, "Không tìm thấy mã điểm: " + locCode)); continue; }
            }

            Optional<NetworkDevice> bySerial = deviceRepository.findBySerialNumber(serial);
            Optional<NetworkDevice> existing = deviceRepository.findByMacAddress(mac);
            if (bySerial.isPresent() && (existing.isEmpty() || !bySerial.get().getId().equals(existing.get().getId()))) {
                errors.add(new RowError(rowNo, mac, "S/N đã thuộc thiết bị khác: " + bySerial.get().getMacAddress()));
                continue;
            }

            NetworkDevice dev;
            boolean isNew = existing.isEmpty();
            if (isNew) {
                if (location == null) {
                    errors.add(new RowError(rowNo, mac, "Thiết bị mới cần có mã điểm lắp đặt")); continue;
                }
                dev = new NetworkDevice();
                dev.setMacAddress(mac);
                dev.setStatus("UNKNOWN");
                dev.setInstalledAt(Instant.now());
            } else {
                dev = existing.get();
            }

            boolean changed = isNew;
            boolean serialChanged = !serial.equals(dev.getSerialNumber());
            changed |= set(dev.getSerialNumber(), serial, dev::setSerialNumber);
            changed |= set(dev.getDeviceType(), type, dev::setDeviceType);
            if (model != null) changed |= set(dev.getModel(), model, dev::setModel);
            if (vendor != null) changed |= set(dev.getVendor(), vendor, dev::setVendor);
            if (ip != null) changed |= set(dev.getIpAddress(), ip, dev::setIpAddress);
            if (location != null && !Objects.equals(dev.getLocationId(), location.getId())) {
                dev.setLocation(location);
                changed = true;
            }
            if (isNew && dev.getModel() == null && "ACCESS_POINT".equals(type)) dev.setModel("vAP-32X6V1");
            if (isNew && dev.getVendor() == null && "ACCESS_POINT".equals(type)) dev.setVendor("Viettel High Tech");
            if (serialChanged && !isNew) {
                // Thiết bị đổi S/N (thay máy) -> quay về mật khẩu gốc của máy mới
                dev.setMeshCredentialState(NetworkDevice.CRED_FACTORY);
                dev.setMeshPasswordRotatedAt(null);
            }

            if (!changed) { unchanged++; continue; }
            if (!dryRun) deviceRepository.save(dev);
            if (isNew) created++; else updated++;
        }

        if (dryRun) {
            // Chế độ kiểm tra: không để thay đổi trên entity được tự động ghi xuống CSDL
            try {
                org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (org.springframework.transaction.NoTransactionException ignored) {
                // gọi ngoài transaction (unit test) — không có gì để rollback
            }
        }
        if (!dryRun && (created + updated) > 0) {
            auditService.recordEvent(AuditAction.DEVICE_SYNC, actor != null ? actor : "admin", "internal",
                    "NETWORK_DEVICE", "IMPORT",
                    "Import danh sách thiết bị từ file " + filename + ": thêm " + created + ", cập nhật " + updated
                            + ", lỗi " + errors.size());
        }
        return new NetworkDeviceImportResultDto(dryRun, total, created, updated, unchanged, List.copyOf(errors));
    }

    /** File mẫu để đơn vị cung cấp / kỹ thuật điền danh sách bàn giao. */
    public byte[] generateTemplate() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Thiet bi");
            CellStyle head = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            head.setFont(bold);
            String[] headers = {"MAC", "Serial (S/N)", "Loai thiet bi", "Model", "Hang", "IP quan tri", "Ma diem"};
            Row h = sh.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = h.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(head);
                sh.setColumnWidth(c, 18 * 256);
            }
            String[] sample = {"24:0B:2B:00:00:01", "VTGRB3000001", "ACCESS_POINT", "vAP-32X6V1", "Viettel High Tech", "192.168.80.111", "NVH-0001"};
            Row s = sh.createRow(1);
            for (int c = 0; c < sample.length; c++) s.createCell(c).setCellValue(sample[c]);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được file mẫu", e);
        }
    }

    // ------------------------------------------------------------------------------------------

    static List<List<String>> readRows(InputStream in, String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv") || name.endsWith(".txt")) return readCsv(in);
            return readExcel(in);
        } catch (IOException | RuntimeException e) {
            throw new BadRequestException("Không đọc được file (hỗ trợ .xlsx, .xls, .csv): " + e.getMessage());
        }
    }

    private static List<List<String>> readExcel(InputStream in) throws IOException {
        List<List<String>> out = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sh = wb.getSheetAt(0);
            for (Row row : sh) {
                List<String> cells = new ArrayList<>();
                int last = Math.max(row.getLastCellNum(), 0);
                for (int c = 0; c < last; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null || cell.getCellType() == CellType.BLANK) { cells.add(""); continue; }
                    cells.add(fmt.formatCellValue(cell).trim());
                }
                while (out.size() < row.getRowNum()) out.add(List.of()); // giữ đúng số dòng cho báo lỗi
                out.add(cells);
            }
        }
        return out;
    }

    private static List<List<String>> readCsv(InputStream in) throws IOException {
        List<List<String>> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first && line.startsWith("﻿")) line = line.substring(1);
                first = false;
                char sep = line.indexOf(';') >= 0 && line.indexOf(',') < 0 ? ';' : ',';
                out.add(splitCsv(line, sep));
            }
        }
        return out;
    }

    private static List<String> splitCsv(String line, char sep) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else quoted = !quoted;
            } else if (ch == sep && !quoted) {
                cells.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        cells.add(cur.toString().trim());
        return cells;
    }

    static Map<Col, Integer> detectHeader(List<String> first) {
        Map<Col, Integer> map = new HashMap<>();
        for (int i = 0; i < first.size(); i++) {
            String h = fold(first.get(i));
            if (h.isEmpty()) continue;
            if (h.contains("mac")) map.putIfAbsent(Col.MAC, i);
            else if (h.contains("serial") || h.equals("sn") || h.contains("s/n") || h.contains("so seri")) map.putIfAbsent(Col.SERIAL, i);
            else if (h.contains("loai") || h.contains("type")) map.putIfAbsent(Col.TYPE, i);
            else if (h.contains("model")) map.putIfAbsent(Col.MODEL, i);
            else if (h.contains("hang") || h.contains("vendor") || h.contains("nha san xuat")) map.putIfAbsent(Col.VENDOR, i);
            else if (h.contains("ip")) map.putIfAbsent(Col.IP, i);
            else if (h.contains("ma diem") || h.contains("location") || h.contains("diem")) map.putIfAbsent(Col.LOCATION, i);
        }
        return map.containsKey(Col.MAC) && map.containsKey(Col.SERIAL) ? map : null;
    }

    private static Map<Col, Integer> defaultColumns() {
        Map<Col, Integer> m = new HashMap<>();
        for (Col c : Col.values()) m.put(c, c.ordinal());
        return m;
    }

    private static String get(List<String> row, Map<Col, Integer> cols, Col col) {
        Integer idx = cols.get(col);
        if (idx == null || idx >= row.size()) return null;
        return row.get(idx);
    }

    static String normalizeMac(String raw) {
        if (raw == null) return null;
        String hex = raw.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-F]", "");
        if (!MAC_HEX.matcher(hex).matches()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) sb.append(':');
            sb.append(hex, i, i + 2);
        }
        return sb.toString();
    }

    static String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) return "ACCESS_POINT";
        String t = fold(raw).replace(' ', '_').toUpperCase(Locale.ROOT);
        if (t.equals("AP") || t.contains("ACCESS") || t.contains("MESH") || t.contains("TRAM_PHAT")) return "ACCESS_POINT";
        if (t.contains("ROUTER") || t.contains("GATEWAY")) return "ROUTER";
        return TYPES.contains(t) ? t : null;
    }

    private static String fold(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        return n;
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static boolean set(String oldVal, String newVal, java.util.function.Consumer<String> setter) {
        if (Objects.equals(oldVal, newVal)) return false;
        setter.accept(newVal);
        return true;
    }
}
