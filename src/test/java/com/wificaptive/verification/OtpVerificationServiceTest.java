package com.wificaptive.verification;

import com.wificaptive.common.exception.BadRequestException;
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
}

