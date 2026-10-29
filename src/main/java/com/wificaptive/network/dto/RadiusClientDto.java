package com.wificaptive.network.dto;

import com.wificaptive.network.entity.RadiusClient;
import java.time.Instant;

public record RadiusClientDto(
        Long id,
        String clientName,
        String ipAddress,
        String sharedSecret,
        String nasIdentifier,
        String description,
        Boolean enabled,
        Instant createdAt
) {
    public static RadiusClientDto fromEntity(RadiusClient c) {
        return new RadiusClientDto(
                c.getId(),
                c.getClientName(),
                c.getIpAddress(),
                c.getSharedSecret(),
                c.getNasIdentifier(),
                c.getDescription(),
                c.getEnabled(),
                c.getCreatedAt()
        );
    }
}
