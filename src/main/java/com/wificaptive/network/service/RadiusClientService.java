package com.wificaptive.network.service;

import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.network.dto.CreateRadiusClientRequest;
import com.wificaptive.network.dto.RadiusClientDto;
import com.wificaptive.network.entity.RadiusClient;
import com.wificaptive.network.repository.RadiusClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RadiusClientService {

    private final RadiusClientRepository radiusClientRepository;

    public RadiusClientService(RadiusClientRepository radiusClientRepository) {
        this.radiusClientRepository = radiusClientRepository;
    }

    @Transactional(readOnly = true)
    public List<RadiusClientDto> getAllClients() {
        return radiusClientRepository.findAll().stream()
                .map(RadiusClientDto::fromEntity)
                .toList();
    }

    public RadiusClientDto createClient(CreateRadiusClientRequest req) {
        RadiusClient client = new RadiusClient();
        client.setClientName(req.clientName());
        client.setIpAddress(req.ipAddress().trim());
        client.setSharedSecret(req.sharedSecret());
        client.setNasIdentifier(req.nasIdentifier());
        client.setDescription(req.description());
        client.setEnabled(req.enabled() != null ? req.enabled() : true);

        return RadiusClientDto.fromEntity(radiusClientRepository.save(client));
    }

    public RadiusClientDto toggleClient(Long id) {
        RadiusClient client = radiusClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RadiusClient", "id", id));
        client.setEnabled(!client.getEnabled());
        return RadiusClientDto.fromEntity(radiusClientRepository.save(client));
    }
}
