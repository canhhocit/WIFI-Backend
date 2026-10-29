package com.wificaptive.network.dto;

import com.wificaptive.network.entity.NetworkBandwidthPolicy;

public record NetworkBandwidthPolicyDto(
        Long id,
        String name,
        String description,
        Integer downloadSpeedKbps,
        Integer uploadSpeedKbps,
        Integer maxSessionSeconds,
        Integer quotaMb,
        Integer priority,
        Boolean active
) {
    public static NetworkBandwidthPolicyDto fromEntity(NetworkBandwidthPolicy p) {
        return new NetworkBandwidthPolicyDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getDownloadSpeedKbps(),
                p.getUploadSpeedKbps(),
                p.getMaxSessionSeconds(),
                p.getQuotaMb(),
                p.getPriority(),
                p.getActive()
        );
    }
}

