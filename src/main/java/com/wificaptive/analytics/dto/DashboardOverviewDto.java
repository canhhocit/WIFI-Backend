package com.wificaptive.analytics.dto;

import com.wificaptive.gateway.dto.GatewayStatusDto;
import java.util.List;
import java.util.Map;

public class DashboardOverviewDto {
    private long totalSessions;
    private long activeSessions;
    private long totalLocations;
    private long activeLocations;
    private long todaySessions;
    private long totalDevices;
    private GatewayStatusDto gatewayStatus;
    private Map<String, Long> sessionsByStatus;
    private List<LocationAnalyticsDto> locationDistribution;

    public DashboardOverviewDto() {
    }

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public long getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(long activeSessions) {
        this.activeSessions = activeSessions;
    }

    public long getTotalLocations() {
        return totalLocations;
    }

    public void setTotalLocations(long totalLocations) {
        this.totalLocations = totalLocations;
    }

    public long getActiveLocations() {
        return activeLocations;
    }

    public void setActiveLocations(long activeLocations) {
        this.activeLocations = activeLocations;
    }

    public long getTodaySessions() {
        return todaySessions;
    }

    public void setTodaySessions(long todaySessions) {
        this.todaySessions = todaySessions;
    }

    public long getTotalDevices() {
        return totalDevices;
    }

    public void setTotalDevices(long totalDevices) {
        this.totalDevices = totalDevices;
    }

    public GatewayStatusDto getGatewayStatus() {
        return gatewayStatus;
    }

    public void setGatewayStatus(GatewayStatusDto gatewayStatus) {
        this.gatewayStatus = gatewayStatus;
    }

    public Map<String, Long> getSessionsByStatus() {
        return sessionsByStatus;
    }

    public void setSessionsByStatus(Map<String, Long> sessionsByStatus) {
        this.sessionsByStatus = sessionsByStatus;
    }

    public List<LocationAnalyticsDto> getLocationDistribution() {
        return locationDistribution;
    }

    public void setLocationDistribution(List<LocationAnalyticsDto> locationDistribution) {
        this.locationDistribution = locationDistribution;
    }
}
