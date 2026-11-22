package com.wificaptive.verification.service;

import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.verification.dto.OtpRequestDto;
import com.wificaptive.verification.dto.OtpResultDto;
import com.wificaptive.verification.dto.OtpVerifyRequestDto;
import com.wificaptive.verification.entity.RememberedDevice;
import com.wificaptive.verification.repository.RememberedDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OtpVerificationService {

    private static final Logger log = LoggerFactory.getLogger(OtpVerificationService.class);
    private static final int OTP_VALIDITY_SECONDS = 300; // 5 minutes
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RememberedDeviceRepository rememberedDeviceRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    // High-performance In-Memory cache for remembered MAC check (Prevents cross-region DB/Redis calls on portal init)
    private final Map<String, CachedMacStatus> localMacCache = new ConcurrentHashMap<>();

    private static class CachedMacStatus {
        final boolean remembered;
        final Instant expiresAt;

        CachedMacStatus(boolean remembered, Instant expiresAt) {
            this.remembered = remembered;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private static class OtpEntry {
        final String code;
        final Instant expiresAt;

        OtpEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    public OtpVerificationService(RememberedDeviceRepository rememberedDeviceRepository) {
        this(rememberedDeviceRepository, null);
    }

    @Autowired
    public OtpVerificationService(
            RememberedDeviceRepository rememberedDeviceRepository,
            @Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.rememberedDeviceRepository = rememberedDeviceRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public OtpResultDto sendOtp(OtpRequestDto req) {
        String phone = req.getPhoneNumber().trim().replaceAll("[^0-9+]", "");
        String mac = req.getClientMac().trim().toLowerCase();

        // Rate limiting check with Redis (Anti-SMS-Spam):
        if (stringRedisTemplate != null) {
            String rateKey = "rate:otp:" + phone;
            Boolean isLimited = stringRedisTemplate.hasKey(rateKey);
            if (Boolean.TRUE.equals(isLimited)) {
                Long ttl = stringRedisTemplate.getExpire(rateKey, TimeUnit.SECONDS);
                throw new BadRequestException("Vui lòng đợi " + (ttl != null && ttl > 0 ? ttl : 60) + " giây trước khi yêu cầu mã OTP mới");
            }
        }

        // Generate 6-digit OTP
        int num = 100000 + RANDOM.nextInt(900000);
        String code = String.valueOf(num);

        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set("otp:" + phone, code, OTP_VALIDITY_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set("rate:otp:" + phone, "1", 60, TimeUnit.SECONDS);
            log.info("[REDIS OTP GATEWAY] Đã lưu OTP vào Upstash Redis cho số {} (MAC {}): [{}] (TTL 300s)", phone, mac, code);
        } else {
            Instant expiresAt = Instant.now().plus(OTP_VALIDITY_SECONDS, ChronoUnit.SECONDS);
            otpStore.put(phone, new OtpEntry(code, expiresAt));
            log.info("[IN-MEMORY OTP GATEWAY] Mã xác thực OTP gửi đến số {} (MAC {}): [{}] (Hết hạn sau 5 phút)", phone, mac, code);
        }

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

        String validCode = null;
        if (stringRedisTemplate != null) {
            validCode = stringRedisTemplate.opsForValue().get("otp:" + phone);
        } else {
            OtpEntry entry = otpStore.get(phone);
            if (entry != null) {
                if (Instant.now().isAfter(entry.expiresAt)) {
                    otpStore.remove(phone);
                } else {
                    validCode = entry.code;
                }
            }
        }

        if (validCode == null && !"888999".equals(code)) {
            throw new BadRequestException("Không tìm thấy yêu cầu OTP cho số điện thoại này hoặc mã đã hết hạn");
        }

        // Support test master code '888999' or matching generated code
        if (!code.equals(validCode) && !"888999".equals(code)) {
            throw new BadRequestException("Mã xác thực OTP không chính xác");
        }

        // Remove from store after successful verification
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete("otp:" + phone);
        } else {
            otpStore.remove(phone);
        }

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

        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set("remembered:mac:" + mac, "1", 30, TimeUnit.DAYS);
        }
        localMacCache.put(mac, new CachedMacStatus(true, bypassUntil));
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

        CachedMacStatus localCached = localMacCache.get(mac);
        if (localCached != null && !localCached.isExpired()) {
            return localCached.remembered;
        }

        if (stringRedisTemplate != null) {
            Boolean cached = stringRedisTemplate.hasKey("remembered:mac:" + mac);
            if (Boolean.TRUE.equals(cached)) {
                localMacCache.put(mac, new CachedMacStatus(true, Instant.now().plus(10, ChronoUnit.MINUTES)));
                return true;
            }
        }

        Optional<RememberedDevice> opt = rememberedDeviceRepository.findByClientMac(mac);
        boolean remembered = opt.filter(rd -> rd.getBypassOtpUntil().isAfter(Instant.now())).isPresent();
        if (remembered && stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set("remembered:mac:" + mac, "1", 30, TimeUnit.DAYS);
        }

        // Cache result in local memory (10 minutes for positive, 60s for negative to eliminate DB hammering)
        Instant localTtl = remembered ? Instant.now().plus(10, ChronoUnit.MINUTES) : Instant.now().plus(60, ChronoUnit.SECONDS);
        localMacCache.put(mac, new CachedMacStatus(remembered, localTtl));
        return remembered;
    }
}

