"""
Agent thu thập mesh Viettel (vAP-32X6V1) đặt trong LAN điểm phát.

Máy chủ trung tâm không vào được LAN sau NAT, nên agent này: hỏi máy chủ danh sách AP cần đọc ->
đăng nhập từng mesh (form LuCI, user admin) -> đọc các lệnh ubus thô -> gửi lên máy chủ phân tích.
Chỉ gọi lệnh ĐỌC, không đổi cấu hình mesh. Không ghi mật khẩu ra đĩa.

Chạy:
    python mesh_agent.py --server https://wifi-haiphong.duckdns.org
Biến môi trường (tùy chọn): MESH_AGENT_TOKEN, MESH_AP_PASSWORD (nếu thiếu sẽ hỏi khi khởi động),
MESH_AGENT_TOKEN_FILE (mặc định .agent-token cạnh script).
"""
import argparse
import getpass
import http.cookiejar
import ipaddress
import json
import os
import socket
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

USER = "admin"

for _s in (sys.stdout, sys.stderr):  # console Windows mặc định cp1252
    try:
        _s.reconfigure(encoding="utf-8", errors="replace")
    except Exception:  # noqa: BLE001
        pass

# Mesh dùng chứng chỉ tự ký -> chỉ bỏ kiểm tra chứng chỉ cho IP nội bộ.
LAN_CTX = ssl.create_default_context()
LAN_CTX.check_hostname = False
LAN_CTX.verify_mode = ssl.CERT_NONE


def log(*a):
    print(time.strftime("%H:%M:%S"), *a, flush=True)


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None


class Mesh:
    def __init__(self, host, password, timeout):
        ip = ipaddress.ip_address(host.split(":")[0])
        if not ip.is_private:
            raise ValueError(f"{host} không phải IP nội bộ")
        self.host, self.password, self.timeout = host, password, timeout
        self.sid = None
        self._id = 0

    def login(self):
        jar = http.cookiejar.CookieJar()
        opener = urllib.request.build_opener(urllib.request.HTTPSHandler(context=LAN_CTX),
                                             urllib.request.HTTPCookieProcessor(jar), _NoRedirect)
        base = f"https://{self.host}/cgi-bin/luci/"
        data = urllib.parse.urlencode({"luci_username": USER, "luci_password": self.password}).encode()
        req = urllib.request.Request(base, data=data, headers={
            "Content-Type": "application/x-www-form-urlencoded", "Referer": base, "User-Agent": "mesh-agent"})
        try:
            opener.open(req, timeout=self.timeout)
        except urllib.error.HTTPError:
            pass  # 302 sau đăng nhập thành công, 403 khi sai mật khẩu
        sid = next((c.value for c in jar if c.name.startswith("sysauth") and c.value != "deleted"), None)
        if not sid:
            raise PermissionError("Mesh từ chối đăng nhập (sai S/N/mật khẩu)")
        self.sid = sid

    def call(self, obj, method, args=None, required=False):
        self._id += 1
        body = json.dumps({"jsonrpc": "2.0", "id": self._id, "method": "call",
                           "params": [self.sid, obj, method, args or {}]}).encode()
        req = urllib.request.Request(f"https://{self.host}/ubus", data=body,
                                     headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req, context=LAN_CTX, timeout=self.timeout) as r:
            resp = json.loads(r.read().decode("utf-8", "replace"))
        if "error" in resp:
            if resp["error"].get("code") == -32002:
                raise PermissionError("access denied")
            if required:
                raise RuntimeError(f"{obj}.{method}: {resp['error']}")
            return None
        res = resp.get("result") or []
        if len(res) >= 2 and res[0] == 0:
            return res[1]
        if required:
            raise RuntimeError(f"{obj}.{method} trả mã {res[:1]}")
        return None

    def collect(self):
        if not self.sid:
            self.login()
        try:
            return self._collect()
        except PermissionError:
            self.login()  # phiên hết hạn (thiết bị khởi động lại...)
            return self._collect()

    def _collect(self):
        topo = self.call("dat2wifi", "getTopologyDump", required=True)
        signals = {}
        wifi = self.call("dat2wifi", "getWifiInfo") or {}
        for radio in wifi.values() if isinstance(wifi, dict) else []:
            for vif in (radio.get("vifs") or {}).values():
                name = vif.get("vifname")
                if not name or str(vif.get("state", "")).lower() != "up":
                    continue
                assoc = self.call("iwinfo", "assoclist", {"device": name}) or {}
                for sta in assoc.get("results", []):
                    if sta.get("mac") and "signal" in sta:
                        signals[sta["mac"].upper()] = sta["signal"]
        return {
            "mode": "LIVE",
            "topology": (topo or {}).get("luaTopologyInfo", "{}"),
            "systemInfo": self.call("system", "info"),
            "board": self.call("system", "board"),
            "hostHints": self.call("luci-rpc", "getHostHints"),
            "signals": signals,
        }


class Server:
    def __init__(self, base, token, timeout=20):
        self.base, self.token, self.timeout = base.rstrip("/"), token, timeout

    def _req(self, path, payload=None):
        data = json.dumps(payload).encode() if payload is not None else None
        req = urllib.request.Request(self.base + path, data=data, headers={
            "X-Agent-Token": self.token, "Content-Type": "application/json", "User-Agent": "mesh-agent"})
        with urllib.request.urlopen(req, timeout=self.timeout) as r:
            return json.loads(r.read().decode("utf-8"))

    def targets(self):
        return self._req("/api/v1/agent/mesh/targets").get("targets", [])

    def push(self, payload):
        return self._req("/api/v1/agent/mesh/snapshots", payload)


def read_token():
    if os.environ.get("MESH_AGENT_TOKEN"):
        return os.environ["MESH_AGENT_TOKEN"].strip()
    f = Path(os.environ.get("MESH_AGENT_TOKEN_FILE", Path(__file__).with_name(".agent-token")))
    if f.exists():
        return f.read_text(encoding="utf-8").strip()
    return getpass.getpass("Token agent: ").strip()


def main():
    ap = argparse.ArgumentParser(description="Agent thu thập mesh tại điểm phát")
    ap.add_argument("--server", default="https://wifi-haiphong.duckdns.org")
    ap.add_argument("--interval", type=int, default=60, help="giây giữa 2 lần đọc")
    ap.add_argument("--timeout", type=int, default=8)
    ap.add_argument("--agent-id", default=socket.gethostname())
    ap.add_argument("--once", action="store_true", help="đọc 1 lần rồi thoát")
    args = ap.parse_args()

    token = read_token()
    password = os.environ.get("MESH_AP_PASSWORD") or getpass.getpass("Mật khẩu admin của mesh (S/N): ")
    server = Server(args.server, token)
    meshes = {}

    while True:
        try:
            targets = server.targets()
        except urllib.error.HTTPError as e:
            log("Máy chủ từ chối:", e.code, "(sai token hoặc máy chủ chưa cấu hình MESH_AGENT_TOKEN_SHA256)")
            targets = None
        except Exception as e:  # noqa: BLE001
            log("Không gọi được máy chủ:", e)
            targets = None

        if targets is not None:
            devices = []
            for t in targets:
                mac, host = t.get("mac"), t.get("host")
                try:
                    m = meshes.get(host) or meshes.setdefault(host, Mesh(host, password, args.timeout))
                    item = m.collect()
                except PermissionError as e:
                    item = {"mode": "AUTH_FAILED", "message": str(e)}
                    meshes.pop(host, None)
                except (urllib.error.URLError, socket.timeout, ConnectionError, OSError) as e:
                    item = {"mode": "UNREACHABLE", "message": f"Agent không kết nối được {host}: {e}"}
                    meshes.pop(host, None)
                except Exception as e:  # noqa: BLE001
                    item = {"mode": "ERROR", "message": str(e)}
                    meshes.pop(host, None)
                item.update({"mac": mac, "host": host})
                devices.append(item)
            try:
                r = server.push({"agentId": args.agent_id, "devices": devices})
                summary = ", ".join(f"{d['host']}={d['mode']}" for d in devices) or "không có AP"
                log(f"Đã gửi {r.get('accepted')} AP: {summary}")
            except Exception as e:  # noqa: BLE001
                log("Gửi dữ liệu thất bại:", e)

        if args.once:
            return
        time.sleep(args.interval)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(0)
