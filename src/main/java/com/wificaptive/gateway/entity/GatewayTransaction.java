package com.wificaptive.gateway.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gateway_transactions")
public class GatewayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation; // AUTHORIZE, REVOKE, STATUS_CHECK

    @Column(name = "adapter_type", nullable = false, length = 20)
    private String adapterType; // MOCK, OPNSENSE

    @Column(name = "is_simulated", nullable = false)
    private boolean simulated = true;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public GatewayTransaction() {
    }

    public GatewayTransaction(Long sessionId, String operation, String adapterType, boolean simulated, boolean success, Integer statusCode, String errorMessage, String rawResponse, Long executionTimeMs) {
        this.sessionId = sessionId;
        this.operation = operation;
        this.adapterType = adapterType;
        this.simulated = simulated;
        this.success = success;
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.rawResponse = rawResponse;
        this.executionTimeMs = executionTimeMs;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getAdapterType() {
        return adapterType;
    }

    public void setAdapterType(String adapterType) {
        this.adapterType = adapterType;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
