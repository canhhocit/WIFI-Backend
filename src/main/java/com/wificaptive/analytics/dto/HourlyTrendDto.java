package com.wificaptive.analytics.dto;

public class HourlyTrendDto {
    private String timeLabel;
    private long sessionCount;
    private long uniqueDevices;

    public HourlyTrendDto() {
    }

    public HourlyTrendDto(String timeLabel, long sessionCount, long uniqueDevices) {
        this.timeLabel = timeLabel;
        this.sessionCount = sessionCount;
        this.uniqueDevices = uniqueDevices;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public void setTimeLabel(String timeLabel) {
        this.timeLabel = timeLabel;
    }

    public long getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(long sessionCount) {
        this.sessionCount = sessionCount;
    }

    public long getUniqueDevices() {
        return uniqueDevices;
    }

    public void setUniqueDevices(long uniqueDevices) {
        this.uniqueDevices = uniqueDevices;
    }
}
