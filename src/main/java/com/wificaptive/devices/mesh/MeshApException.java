package com.wificaptive.devices.mesh;

/**
 * Lỗi khi đọc dữ liệu trực tiếp từ thiết bị mesh (Viettel HomeWiFi / OpenWrt LuCI).
 */
public class MeshApException extends Exception {

    public enum Kind {
        /** Không kết nối được tới thiết bị (mất mạng, sai IP, hết thời gian chờ). */
        UNREACHABLE,
        /** Thiết bị phản hồi nhưng từ chối tài khoản / mật khẩu. */
        AUTH_FAILED,
        /** Phiên ubus hết hạn hoặc không đủ quyền gọi lệnh. */
        ACCESS_DENIED,
        /** Thiết bị trả dữ liệu không đúng định dạng mong đợi. */
        PROTOCOL
    }

    private final Kind kind;

    public MeshApException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public MeshApException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
