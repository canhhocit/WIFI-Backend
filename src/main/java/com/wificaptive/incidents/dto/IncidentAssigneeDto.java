package com.wificaptive.incidents.dto;

public record IncidentAssigneeDto(
        Long id,
        String username,
        String fullName,
        String role,
        String roleLabel,
        String unitGroup
) {}
