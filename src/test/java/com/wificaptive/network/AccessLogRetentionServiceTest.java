package com.wificaptive.network;

import com.wificaptive.network.entity.AccessLogRetention;
import com.wificaptive.network.repository.AccessLogRetentionRepository;
import com.wificaptive.network.service.AccessLogRetentionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessLogRetentionServiceTest {

    @Mock
    private AccessLogRetentionRepository retentionRepository;

    private AccessLogRetentionService retentionService;

    @BeforeEach
    void setUp() {
        retentionService = new AccessLogRetentionService(retentionRepository);
    }

    @Test
    void testRecordSessionCompletion_Sets365DaysRetention() {
        Instant now = Instant.now();
        Instant disconnected = now.plus(90, ChronoUnit.MINUTES);

        when(retentionRepository.save(any(AccessLogRetention.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccessLogRetention record = retentionService.recordSessionCompletion(
                100L,
                "sess_12345",
                "00:11:22:33:44:55",
                "192.168.1.50",
                1L,
                "Nhà văn hóa An Dương",
                "0912345678",
                now,
                disconnected,
                5400L,
                10485760L,
                20971520L,
                "EXPIRED"
        );

        assertNotNull(record);
        assertEquals("00:11:22:33:44:55", record.getClientMac());
        assertEquals("192.168.1.50", record.getClientIp());
        assertEquals("EXPIRED", record.getTerminationReason());

        // Verify retained_until is 365 days after authenticated_at
        long daysBetween = ChronoUnit.DAYS.between(record.getAuthenticatedAt(), record.getRetainedUntil());
        assertEquals(365, daysBetween);
    }
}
