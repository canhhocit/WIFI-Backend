package com.wificaptive.network;

import com.wificaptive.network.dto.CreateBlacklistRequest;
import com.wificaptive.network.dto.MacBlacklistDto;
import com.wificaptive.network.entity.MacBlacklist;
import com.wificaptive.network.repository.MacBlacklistRepository;
import com.wificaptive.network.service.MacBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MacBlacklistServiceTest {

    @Mock
    private MacBlacklistRepository blacklistRepository;

    private MacBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new MacBlacklistService(blacklistRepository);
    }

    @Test
    void testIsBlacklisted() {
        when(blacklistRepository.existsByMacAddressIgnoreCaseAndActiveTrue("AA:BB:CC:DD:EE:FF"))
                .thenReturn(true);
        when(blacklistRepository.existsByMacAddressIgnoreCaseAndActiveTrue("11:22:33:44:55:66"))
                .thenReturn(false);

        assertTrue(blacklistService.isBlacklisted("AA:BB:CC:DD:EE:FF"));
        assertFalse(blacklistService.isBlacklisted("11:22:33:44:55:66"));
        assertFalse(blacklistService.isBlacklisted(null));
    }

    @Test
    void testAddToBlacklistAndUnblock() {
        CreateBlacklistRequest req = new CreateBlacklistRequest(
                "AA:BB:CC:DD:EE:FF",
                "Spamming authentication attempts"
        );

        when(blacklistRepository.findByMacAddressIgnoreCase("AA:BB:CC:DD:EE:FF"))
                .thenReturn(Optional.empty());

        MacBlacklist saved = new MacBlacklist();
        saved.setId(10L);
        saved.setMacAddress("AA:BB:CC:DD:EE:FF");
        saved.setReason(req.reason());
        saved.setBlockedBy("security_admin");
        saved.setActive(true);

        when(blacklistRepository.save(any(MacBlacklist.class))).thenReturn(saved);

        MacBlacklistDto result = blacklistService.addToBlacklist(req, "security_admin");
        assertNotNull(result);
        assertEquals("AA:BB:CC:DD:EE:FF", result.macAddress());
        assertTrue(result.active());

        when(blacklistRepository.findById(10L)).thenReturn(Optional.of(saved));

        MacBlacklistDto unblocked = blacklistService.unblock(10L);
        assertFalse(unblocked.active());
    }
}
