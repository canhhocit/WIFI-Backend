package com.wificaptive.verification.service;

import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.verification.dto.OtpRequestDto;
import com.wificaptive.verification.dto.OtpResultDto;
import com.wificaptive.verification.dto.OtpVerifyRequestDto;
import com.wificaptive.verification.entity.RememberedDevice;
import com.wificaptive.verification.repository.RememberedDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpVerificationService {

    private static final Logger log = LoggerFactory.getLogger(OtpVerificationService.class);
    private static final int OTP_VALIDITY_SECONDS = 300; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RememberedDeviceRepository rememberedDeviceRepository;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static class OtpEntry {
        final String code;
        final Instant expiresAt;

        OtpEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    public OtpVerificationService(RememberedDeviceRepository rememberedDeviceRepository) {
        this.rememberedDeviceRepository = rememberedDeviceRepository;
    }

    public OtpResultDto sendOtp(OtpRequestDto req) {
        String phone = req.getPhoneNumber().trim().replaceAll("[^0-9+]", "");
        String mac = req.getClientMac().trim().toLowerCase();

        // Generate 6-digit OTP
        int num = 100000 + RANDOM.nextInt(900000);
        String code = String.valueOf(num);

        Instant expiresAt = Instant.now().plus(OTP_VALIDITY_SECONDS, ChronoUnit.SECONDS);
        otpStore.put(phone, new OtpEntry(code, expiresAt));

        log.info("[TELCO OTP GATEWAY - SIMULATOR] Mã xác thực OTP gửi đến số {}: [{}] (Hết hạn sau 5 phút)", phone, code);

        OtpResultDto res = new OtpResultDto();
        res.setSuccess(true);
        res.setMessage("Mã xác thực OTP đã được gửi đến số " + phone);
        res.setExpiresInSeconds(OTP_VALIDITY_SECONDS);
        res.setTestCodeHint(code); // Exposed in simulation mode for ease of automated/manual testing
        return res;
    }

    @Transactional
    public OtpResultDto verifyOtp(OtpVerifyRequestDto req) {
        String phone = req.getPhoneNumber().trim().replaceAll("[^0-9+]", "");
        String mac = req.getClientMac().trim().toLowerCase();
        String code = req.getOtpCode().trim();

        OtpEntry entry = otpStore.get(phone);
        if (entry == null) {
            throw new BadRequestException("Không tìm thấy yêu cầu OTP cho số điện thoại này hoặc mã đã hết hạn");
        }

        if (Instant.now().isAfter(entry.expiresAt)) {
            otpStore.remove(phone);
            throw new BadRequestException("Mã OTP đã hết hạn, vui lòng gửi lại mã mới");
        }

        // Support test master code '888999' or matching generated code
        if (!code.equals(entry.code) && !"888999".equals(code)) {
            throw new BadRequestException("Mã xác thực OTP không chính xác");
        }

        // Remove from store after successful verification
        otpStore.remove(phone);

        // Remember device MAC for 30 days
        Instant bypassUntil = Instant.now().plus(30, ChronoUnit.DAYS);
        Optional<RememberedDevice> opt = rememberedDeviceRepository.findByClientMac(mac);
        RememberedDevice rd = opt.orElseGet(RememberedDevice::new);
        rd.setClientMac(mac);
        rd.setPhoneNumber(phone);
        rd.setRememberDurationDays(30);
        rd.setBypassOtpUntil(bypassUntil);
        rd.setLastSeenAt(Instant.now());

        rememberedDeviceRepository.save(rd);
        log.info("Device MAC {} remembered until {} (30 days one-click re-login active)", mac, bypassUntil);

        OtpResultDto res = new OtpResultDto();
        res.setSuccess(true);
        res.setMessage("Xác thực OTP thành công. Thiết bị được ghi nhớ 30 ngày cho các lần truy cập tiếp theo.");
        res.setBypassOtpUntil(bypassUntil);
        return res;
    }

    @Transactional(readOnly = true)
    public boolean isMacRemembered(String clientMac) {
        if (clientMac == null || clientMac.isBlank()) return false;
        String mac = clientMac.trim().toLowerCase();
        Optional<RememberedDevice> opt = rememberedDeviceRepository.findByClientMac(mac);
        return opt.filter(rd -> rd.getBypassOtpUntil().isAfter(Instant.now())).isPresent();
    }
}

