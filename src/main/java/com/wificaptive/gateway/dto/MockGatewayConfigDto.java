package com.wificaptive.gateway.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class MockGatewayConfigDto {
    private Boolean simulateFailures;

    @Min(value = 0, message = "Latency cannot be negative")
    @Max(value = 10000, message = "Latency cannot exceed 10000ms")
    private Integer simulatedLatencyMs;

    public MockGatewayConfigDto() {
    }

    public MockGatewayConfigDto(Boolean simulateFailures, Integer simulatedLatencyMs) {
        this.simulateFailures = simulateFailures;
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    public Boolean getSimulateFailures() {
        return simulateFailures;
    }

    public void setSimulateFailures(Boolean simulateFailures) {
        this.simulateFailures = simulateFailures;
    }

    public Integer getSimulatedLatencyMs() {
        return simulatedLatencyMs;
    }

    public void setSimulatedLatencyMs(Integer simulatedLatencyMs) {
        this.simulatedLatencyMs = simulatedLatencyMs;
    }
}
