package com.wificaptive.authentication.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AdminRole {
    ROLE_SUPER_ADMIN,
    ROLE_ADMIN,
    ROLE_SOTTTT_ADMIN,
    ROLE_DISTRICT_OFFICER,
    ROLE_COMMUNE_OFFICER,
    ROLE_VIETTEL_ENGINEER,
    ROLE_VNPT_ENGINEER; // Kept for backward compatibility with legacy database rows

    @JsonCreator
    public static AdminRole fromString(String value) {
        if (value == null) return ROLE_ADMIN;
        String trimmed = value.trim().toUpperCase();
        if ("ROLE_VNPT_ENGINEER".equals(trimmed)) {
            return ROLE_VIETTEL_ENGINEER;
        }
        for (AdminRole r : values()) {
            if (r.name().equalsIgnoreCase(trimmed)) {
                return r;
            }
        }
        return ROLE_ADMIN;
    }
}

