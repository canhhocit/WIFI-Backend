package com.wificaptive.verification.dto;

import java.time.Instant;

public class OtpResultDto {
    private boolean success;
    private String message;
    private String testCodeHint;
    private int expiresInSeconds;
    private Instant bypassOtpUntil;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTestCodeHint() { return testCodeHint; }
    public void setTestCodeHint(String testCodeHint) { this.testCodeHint = testCodeHint; }

    public int getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(int expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public Instant getBypassOtpUntil() { return bypassOtpUntil; }
    public void setBypassOtpUntil(Instant bypassOtpUntil) { this.bypassOtpUntil = bypassOtpUntil; }
}

