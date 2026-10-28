package com.wificaptive.verification.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpVerifyRequestDto {

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    @NotBlank(message = "Địa chỉ MAC không được để trống")
    private String clientMac;

    @NotBlank(message = "Mã xác thực OTP không được để trống")
    private String otpCode;

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getClientMac() { return clientMac; }
    public void setClientMac(String clientMac) { this.clientMac = clientMac; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}

