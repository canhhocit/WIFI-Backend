package com.wificaptive.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.devices.dto.NetworkDeviceDto;
import com.wificaptive.devices.service.NetworkDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller tiếp nhận Webhook sự kiện và cảnh báo từ Grandstream GDMS / GWN Cloud.
 * Khi thiết bị AP/Router GWN tại các Nhà văn hóa bị mất điện, đứt cáp hoặc chuyển trạng thái OFFLINE,
 * hệ thống tự động:
 * 1. Cập nhật trạng thái phần cứng trên hệ thống Hải Phòng.
 * 2. Tự động khởi tạo Phiếu sự cố kỹ thuật (Incident Ticket) với SLA khẩn cấp 4 giờ.
 * 3. Báo động tức thời lên Bản đồ số GIS và gửi thông báo tới cán bộ phụ trách.
 */
@RestController
@RequestMapping("/api/v1/gateway/gwn")
public class GwnWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GwnWebhookController.class);

    private final NetworkDeviceService deviceService;
    private final ObjectMapper objectMapper;

    public GwnWebhookController(NetworkDeviceService deviceService, ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/webhook/health")
    public ResponseEntity<Map<String, Object>> webhookHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Grandstream GDMS Webhook Receiver",
                "message", "Sẵn sàng tiếp nhận cảnh báo phần cứng từ GDMS Cloud"
        ));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGwnCloudWebhook(@RequestBody String rawPayload) {
        log.info("[GWN Webhook] Nhận gói tin cảnh báo từ Grandstream Cloud: {}", rawPayload);

        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            // 1. Trích xuất địa chỉ MAC thiết bị
            String mac = null;
            if (root.has("mac")) {
                mac = root.path("mac").asText();
            } else if (root.has("deviceMac")) {
                mac = root.path("deviceMac").asText();
            } else if (root.has("device_mac")) {
                mac = root.path("device_mac").asText();
            } else if (root.has("data") && root.path("data").has("mac")) {
                mac = root.path("data").path("mac").asText();
            }

            // 2. Trích xuất sự kiện & trạng thái
            String event = root.has("event") ? root.path("event").asText("") :
                    root.has("eventType") ? root.path("eventType").asText("") :
                    root.has("type") ? root.path("type").asText("") : "";

            String status = root.has("status") ? root.path("status").asText("") : "";

            String alarmType = root.has("alarmType") ? root.path("alarmType").asText("") :
                    root.has("alarm_type") ? root.path("alarm_type").asText("") : "";

            String message = root.has("message") ? root.path("message").asText("") :
                    root.has("msg") ? root.path("msg").asText("") :
                    root.has("detail") ? root.path("detail").asText("") : "Cảnh báo từ Grandstream GDMS Cloud";

            // Nếu không xác định được rõ status từ root, phán đoán qua event / alarmType
            String effectiveStatus = status;
            if (effectiveStatus.isBlank()) {
                if (event.toLowerCase().contains("offline") || alarmType.toLowerCase().contains("offline") ||
                    event.toLowerCase().contains("disconnect") || alarmType.toLowerCase().contains("disconnect")) {
                    effectiveStatus = "OFFLINE";
                } else if (event.toLowerCase().contains("online") || alarmType.toLowerCase().contains("online") ||
                           event.toLowerCase().contains("connect") || alarmType.toLowerCase().contains("connect")) {
                    effectiveStatus = "ONLINE";
                }
            }

            if (mac == null || mac.isBlank()) {
                log.warn("[GWN Webhook] Payload không chứa địa chỉ MAC hợp lệ: {}", rawPayload);
                return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu thông tin địa chỉ MAC thiết bị"));
            }

            // 3. Xử lý nghiệp vụ cập nhật thiết bị và tự động tạo phiếu sự cố
            NetworkDeviceDto updatedDev = deviceService.processDeviceAlarmWebhook(mac, effectiveStatus, alarmType, message);

            Map<String, Object> result = Map.of(
                    "mac", mac,
                    "status", effectiveStatus,
                    "alarmType", alarmType,
                    "deviceUpdated", updatedDev != null,
                    "deviceName", updatedDev != null ? updatedDev.getModel() : "Unknown",
                    "actionTaken", "OFFLINE".equalsIgnoreCase(effectiveStatus)
                            ? "Tự động kích hoạt Phiếu sự cố kỹ thuật (SLA 4h) và cảnh báo Bản đồ số GIS"
                            : "Tự động đóng phiếu sự cố và khôi phục trạng thái điểm phát sóng"
            );

            return ResponseEntity.ok(ApiResponse.ok("Xử lý Webhook GDMS thành công", result));
        } catch (Exception e) {
            log.error("[GWN Webhook] Lỗi khi xử lý gói tin webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi cú pháp gói tin Webhook: " + e.getMessage()));
        }
    }
}
