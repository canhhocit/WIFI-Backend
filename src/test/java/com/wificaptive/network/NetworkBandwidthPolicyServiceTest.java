package com.wificaptive.network;

import com.wificaptive.network.dto.CreatePolicyRequest;
import com.wificaptive.network.dto.NetworkBandwidthPolicyDto;
import com.wificaptive.network.entity.NetworkBandwidthPolicy;
import com.wificaptive.network.repository.NetworkBandwidthPolicyRepository;
import com.wificaptive.network.service.NetworkBandwidthPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkBandwidthPolicyServiceTest {

    @Mock
    private NetworkBandwidthPolicyRepository policyRepository;

    private NetworkBandwidthPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new NetworkBandwidthPolicyService(policyRepository);
    }

    @Test
    void testCreateAndTogglePolicy() {
        CreatePolicyRequest req = new CreatePolicyRequest(
                "VIP Cultural Center",
                "High-speed bandwidth for commune centers",
                10240,
                5120,
                7200,
                0,
                3,
                true
        );

        NetworkBandwidthPolicy entity = new NetworkBandwidthPolicy();
        entity.setId(1L);
        entity.setName("VIP Cultural Center");
        entity.setDownloadSpeedKbps(10240);
        entity.setUploadSpeedKbps(5120);
        entity.setMaxSessionSeconds(7200);
        entity.setActive(true);

        when(policyRepository.save(any(NetworkBandwidthPolicy.class))).thenReturn(entity);

        NetworkBandwidthPolicyDto created = policyService.createPolicy(req);
        assertNotNull(created);
        assertEquals("VIP Cultural Center", created.name());
        assertEquals(10240, created.downloadSpeedKbps());

        when(policyRepository.findById(1L)).thenReturn(Optional.of(entity));

        NetworkBandwidthPolicyDto toggled = policyService.togglePolicy(1L);
        assertFalse(toggled.active());
    }

    @Test
    void testGetActivePolicies() {
        NetworkBandwidthPolicy p1 = new NetworkBandwidthPolicy();
        p1.setId(1L);
        p1.setName("Standard");
        p1.setPriority(1);
        p1.setActive(true);

        when(policyRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(List.of(p1));

        List<NetworkBandwidthPolicyDto> active = policyService.getActivePolicies();
        assertEquals(1, active.size());
        assertEquals("Standard", active.get(0).name());
    }
}
