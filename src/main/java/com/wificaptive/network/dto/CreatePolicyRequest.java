package com.wificaptive.network.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePolicyRequest(
        @NotBlank(message = "Tên chính sách băng thông không được để trống")
        String name,
        String description,
        @NotNull @Positive
        Integer downloadSpeedKbps,
        @NotNull @Positive
        Integer uploadSpeedKbps,
        @NotNull @Positive
        Integer maxSessionSeconds,
        Integer quotaMb,
        Integer priority,
        Boolean active
) {}

