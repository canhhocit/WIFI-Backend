package com.wificaptive.gateway.dto;

public class GwnDeviceSyncDto {
    private String mac;
    private String name;
    private String model;
    private String vendor;
    private String ip;
    private String status;
    private String firmware;
    private Integer cpuPercent;
    private Integer memoryPercent;
    private Long uptimeSeconds;

    public GwnDeviceSyncDto() {}

    public GwnDeviceSyncDto(String mac, String name, String model, String vendor, String ip,
                            String status, String firmware, Integer cpuPercent, Integer memoryPercent, Long uptimeSeconds) {
        this.mac = mac;
        this.name = name;
        this.model = model;
        this.vendor = vendor;
        this.ip = ip;
        this.status = status;
        this.firmware = firmware;
        this.cpuPercent = cpuPercent;
        this.memoryPercent = memoryPercent;
        this.uptimeSeconds = uptimeSeconds;
    }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

    public Integer getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(Integer cpuPercent) { this.cpuPercent = cpuPercent; }

    public Integer getMemoryPercent() { return memoryPercent; }
    public void setMemoryPercent(Integer memoryPercent) { this.memoryPercent = memoryPercent; }

    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
}

