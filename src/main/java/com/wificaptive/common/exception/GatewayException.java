package com.wificaptive.common.exception;

public class GatewayException extends AppException {
    private final Integer statusCode;

    public GatewayException(String message) {
        super(message);
        this.statusCode = 502;
    }

    public GatewayException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 502;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
