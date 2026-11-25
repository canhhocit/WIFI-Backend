package com.wificaptive.gateway.dto;

public record GwnClientDto(
        String mac,
        String ip,
        String hostname,
        String ssid,
        String apMac,
        String apName,
        String radioBand, // 2.4GHz / 5GHz
        Integer rssi, // -30 to -90 dBm
        long bytesRx,
        long bytesTx,
        long connectTimeSeconds,
        String lanPort, // cổng LAN của router mà client đang nối (để thống kê client theo từng AP/cổng); null nếu không có
        boolean wired
) {}
