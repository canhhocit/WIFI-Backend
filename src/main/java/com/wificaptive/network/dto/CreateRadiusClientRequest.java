package com.wificaptive.network.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRadiusClientRequest(
        @NotBlank(message = "Tên RADIUS Client không được để trống")
        String clientName,

        @NotBlank(message = "Địa chỉ IP không được để trống")
        String ipAddress,

        @NotBlank(message = "Shared secret không được để trống")
        String sharedSecret,

        String nasIdentifier,
        String description,
        Boolean enabled
) {}
