package com.wificaptive.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBlacklistRequest(
        @NotBlank(message = "Địa chỉ MAC không được để trống")
        @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", message = "Địa chỉ MAC không đúng định dạng (VD: AA:BB:CC:DD:EE:FF)")
        String macAddress,

        @NotBlank(message = "Lý do chặn không được để trống")
        String reason
) {}
