package com.wificaptive.verification.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.verification.dto.OtpRequestDto;
import com.wificaptive.verification.dto.OtpResultDto;
import com.wificaptive.verification.dto.OtpVerifyRequestDto;
import com.wificaptive.verification.service.OtpVerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/verification")
public class VerificationPortalController {

    private final OtpVerificationService otpVerificationService;

    public VerificationPortalController(OtpVerificationService otpVerificationService) {
        this.otpVerificationService = otpVerificationService;
    }

    @PostMapping("/otp/send")
    public ApiResponse<OtpResultDto> sendOtp(@Valid @RequestBody OtpRequestDto req) {
        return ApiResponse.ok(otpVerificationService.sendOtp(req));
    }

    @PostMapping("/otp/verify")
    public ApiResponse<OtpResultDto> verifyOtp(@Valid @RequestBody OtpVerifyRequestDto req) {
        return ApiResponse.ok(otpVerificationService.verifyOtp(req));
    }

    @GetMapping("/check-mac")
    public ApiResponse<Boolean> checkMacRemembered(@RequestParam String mac) {
        return ApiResponse.ok(otpVerificationService.isMacRemembered(mac));
    }
}

