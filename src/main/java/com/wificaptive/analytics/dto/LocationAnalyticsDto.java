package com.wificaptive.analytics.dto;

public class LocationAnalyticsDto {
    private String locationName;
    private long sessionCount;
    private double percentage;

    public LocationAnalyticsDto() {
    }

    public LocationAnalyticsDto(String locationName, long sessionCount, double percentage) {
        this.locationName = locationName;
        this.sessionCount = sessionCount;
        this.percentage = percentage;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public long getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(long sessionCount) {
        this.sessionCount = sessionCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
