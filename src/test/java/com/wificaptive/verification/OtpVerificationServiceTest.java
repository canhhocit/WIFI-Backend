package com.wificaptive.verification;

import com.wificaptive.verification.dto.OtpRequestDto;
import com.wificaptive.verification.dto.OtpResultDto;
import com.wificaptive.verification.dto.OtpVerifyRequestDto;
import com.wificaptive.verification.entity.RememberedDevice;
import com.wificaptive.verification.repository.RememberedDeviceRepository;
import com.wificaptive.verification.service.OtpVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpVerificationServiceTest {

    @Mock
    private RememberedDeviceRepository rememberedDeviceRepository;

    private OtpVerificationService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpVerificationService(rememberedDeviceRepository);
    }

    @Test
    void testSendAndVerifyOtpRememberDevice() {
        OtpRequestDto sendReq = new OtpRequestDto();
        sendReq.setPhoneNumber("0912345678");
        sendReq.setClientMac("00:11:22:33:44:aa");

        OtpResultDto sendResult = otpService.sendOtp(sendReq);
        assertTrue(sendResult.isSuccess());
        assertNotNull(sendResult.getTestCodeHint());

        // Verify with test master code
        OtpVerifyRequestDto verifyReq = new OtpVerifyRequestDto();
        verifyReq.setPhoneNumber("0912345678");
        verifyReq.setClientMac("00:11:22:33:44:aa");
        verifyReq.setOtpCode(sendResult.getTestCodeHint());

        when(rememberedDeviceRepository.findByClientMac("00:11:22:33:44:aa")).thenReturn(Optional.empty());

        OtpResultDto verifyResult = otpService.verifyOtp(verifyReq);
        assertTrue(verifyResult.isSuccess());
        assertNotNull(verifyResult.getBypassOtpUntil());
        verify(rememberedDeviceRepository, times(1)).save(any(RememberedDevice.class));
    }

    @Test
    void testIsMacRemembered() {
        RememberedDevice rd = new RememberedDevice();
        rd.setClientMac("00:11:22:33:44:bb");
        rd.setBypassOtpUntil(Instant.now().plus(10, ChronoUnit.DAYS));

        when(rememberedDeviceRepository.findByClientMac("00:11:22:33:44:bb")).thenReturn(Optional.of(rd));

        boolean remembered = otpService.isMacRemembered("00:11:22:33:44:bb");
        assertTrue(remembered);
    }

    @Test
    void testSendOtpWithRedisAndRateLimit() {
        org.springframework.data.redis.core.StringRedisTemplate redisTemplate = mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, String> valOps = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valOps);

        OtpVerificationService redisOtpService = new OtpVerificationService(rememberedDeviceRepository, redisTemplate);

        OtpRequestDto sendReq = new OtpRequestDto();
        sendReq.setPhoneNumber("0988776655");
        sendReq.setClientMac("11:22:33:44:55:66");

        when(redisTemplate.hasKey("rate:otp:0988776655")).thenReturn(false);

        OtpResultDto sendResult = redisOtpService.sendOtp(sendReq);
        assertTrue(sendResult.isSuccess());
        verify(valOps, times(1)).set(eq("otp:0988776655"), any(), eq(300L), eq(java.util.concurrent.TimeUnit.SECONDS));
        verify(valOps, times(1)).set(eq("rate:otp:0988776655"), eq("1"), eq(60L), eq(java.util.concurrent.TimeUnit.SECONDS));

        // Rate limit hit
        when(redisTemplate.hasKey("rate:otp:0988776655")).thenReturn(true);
        when(redisTemplate.getExpire(eq("rate:otp:0988776655"), any())).thenReturn(45L);

        assertThrows(com.wificaptive.common.exception.BadRequestException.class, () -> redisOtpService.sendOtp(sendReq));
    }
}

