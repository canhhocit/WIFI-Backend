"""
Cài agent chạy NGAY TRÊN mesh Viettel vAP-32X6V1 (không cần máy tính tại điểm phát).

Cài đặt (qua quyền admin của LuCI, không cần SSH):
  - /etc/luci-uploads/vt-mesh-agent.sh : script đọc ubus tại chỗ rồi gửi HTTPS lên máy chủ
  - 1 dòng trong /etc/crontabs/root    : chạy script mỗi phút
  - 2 dòng trong /etc/sysupgrade.conf  : giữ agent khi nâng firmware
  - reload cron

Chạy:
    python install_on_mesh.py                      (cài lên 192.168.80.111)
    python install_on_mesh.py --host 192.168.80.111 --server https://wifi-haiphong.duckdns.org
    python install_on_mesh.py --status             (xem log agent trên mesh)
    python install_on_mesh.py --uninstall          (gỡ bỏ)
Hỏi mật khẩu admin (S/N) khi chạy, không lưu.
"""
import argparse
import getpass
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from mesh_agent import Mesh, read_token, log  # noqa: E402

SCRIPT = "/etc/luci-uploads/vt-mesh-agent.sh"
CRONTAB = "/etc/crontabs/root"
SYSUPGRADE = "/etc/sysupgrade.conf"
CRON_LINE = f"* * * * * sh {SCRIPT} >/dev/null 2>&1"
MARK = "vt-mesh-agent"


def read_file(m, path):
    try:
        r = m.call("file", "read", {"path": path})
    except PermissionError:
        raise
    except Exception:  # noqa: BLE001
        return ""
    return (r or {}).get("data", "") if isinstance(r, dict) else ""


def write_file(m, path, data):
    m.call("file", "write", {"path": path, "data": data}, required=True)


def lines_without(text, needle):
    return [ln for ln in text.splitlines() if needle not in ln]


def reload_cron(m):
    m.call("file", "exec", {"command": "/etc/init.d/cron", "params": ["reload"]}, required=True)


def agent_log(m):
    r = m.call("file", "exec", {"command": "/sbin/logread"})
    out = (r or {}).get("stdout", "") if isinstance(r, dict) else ""
    return [ln for ln in out.splitlines() if MARK in ln or "crond" in ln][-15:]


def install(m, server, token):
    tpl = (HERE / "vt-mesh-agent.sh.tpl").read_text(encoding="utf-8").replace("
", "
")  # ash trên mesh không chạy file CRLF
    script = tpl.replace("__SERVER__", server.rstrip("/")).replace("__TOKEN__", token)
    write_file(m, SCRIPT, script)
    log("Đã ghi", SCRIPT)

    cron = lines_without(read_file(m, CRONTAB), MARK)
    cron.append(CRON_LINE)
    write_file(m, CRONTAB, "\n".join(cron) + "\n")
    log("Đã thêm lịch chạy mỗi phút vào", CRONTAB)

    keep = read_file(m, SYSUPGRADE).splitlines()
    for p in (SCRIPT, CRONTAB):
        if p not in keep:
            keep.append(p)
    write_file(m, SYSUPGRADE, "\n".join(keep) + "\n")
    log("Đã đăng ký giữ agent khi nâng firmware trong", SYSUPGRADE)

    reload_cron(m)
    log("Đã reload cron. Chờ ~70 giây để mesh gửi lần đầu...")
    time.sleep(70)
    show_status(m)


def uninstall(m):
    cron = lines_without(read_file(m, CRONTAB), MARK)
    write_file(m, CRONTAB, ("\n".join(cron) + "\n") if cron else "")
    write_file(m, SCRIPT, "#!/bin/sh\n# vt-mesh-agent da go bo\nexit 0\n")
    keep = [ln for ln in read_file(m, SYSUPGRADE).splitlines() if ln.strip() not in (SCRIPT, CRONTAB)]
    write_file(m, SYSUPGRADE, "\n".join(keep) + "\n")
    reload_cron(m)
    log("Đã gỡ agent khỏi mesh (xóa lịch cron, vô hiệu script, bỏ khỏi sysupgrade.conf).")


def show_status(m):
    cron = read_file(m, CRONTAB)
    log("Lịch cron agent:", "CÓ" if MARK in cron else "KHÔNG")
    lines = agent_log(m)
    log("Log gần nhất trên mesh:" if lines else "Chưa có log agent/crond trên mesh.")
    for ln in lines:
        print("   ", ln)


def main():
    ap = argparse.ArgumentParser(description="Cài agent chạy trên mesh")
    ap.add_argument("--host", default="192.168.80.111")
    ap.add_argument("--server", default="https://wifi-haiphong.duckdns.org")
    g = ap.add_mutually_exclusive_group()
    g.add_argument("--uninstall", action="store_true")
    g.add_argument("--status", action="store_true")
    args = ap.parse_args()

    token = None if (args.uninstall or args.status) else read_token()
    password = getpass.getpass(f"Mật khẩu admin@{args.host} (S/N): ")
    m = Mesh(args.host, password, 15)
    try:
        m.login()
    except PermissionError:
        log("Đăng nhập THẤT BẠI — kiểm tra lại S/N.")
        sys.exit(1)
    log("Đăng nhập mesh OK")

    if args.uninstall:
        uninstall(m)
    elif args.status:
        show_status(m)
    else:
        install(m, args.server, token)


if __name__ == "__main__":
    main()
