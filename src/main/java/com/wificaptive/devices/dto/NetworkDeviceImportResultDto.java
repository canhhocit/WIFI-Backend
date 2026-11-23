package com.wificaptive.devices.dto;

import java.util.List;

/**
 * Kết quả import danh sách thiết bị bàn giao (S/N + MAC) từ file Excel/CSV.
 * dryRun=true: chỉ kiểm tra, chưa ghi vào CSDL.
 */
public record NetworkDeviceImportResultDto(
        boolean dryRun,
        int totalRows,
        int created,
        int updated,
        int unchanged,
        List<RowError> errors
) {
    public record RowError(int row, String mac, String message) {}

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
