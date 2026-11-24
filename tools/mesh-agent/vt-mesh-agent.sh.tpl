#!/bin/sh
# vt-mesh-agent: doc trang thai mesh tai cho va gui len he thong giam sat Wi-Fi Hai Phong.
# Cai boi install_on_mesh.py, chay moi phut qua cron. Chi doc, khong doi cau hinh mesh.
# Go bo: python install_on_mesh.py --uninstall
SERVER="__SERVER__"
TOKEN="__TOKEN__"
TMP=/tmp/vt-mesh-agent.json

exec 9>/tmp/vt-mesh-agent.lock
flock -n 9 || exit 0

j() { v=$(ubus call "$@" 2>/dev/null); [ -n "$v" ] && echo "$v" || echo null; }

MAC=$(cat /sys/class/net/br-lan/address 2>/dev/null)
IP=$(uci -q get network.lan.ipaddr)
[ -n "$IP" ] || IP=$(ip -4 addr show br-lan 2>/dev/null | sed -n 's/.*inet \([0-9.]*\).*/\1/p' | head -n1)

ASSOC=""
for i in ra0 rax0 ra1 rax1; do
    a=$(ubus call iwinfo assoclist "{\"device\":\"$i\"}" 2>/dev/null)
    [ -n "$a" ] && ASSOC="$ASSOC${ASSOC:+,}\"$i\":$a"
done

# Bo mat khau Wi-Fi (Pass-phrase) khoi topology truoc khi gui di
TOPO=$(j dat2wifi getTopologyDump | sed 's/\\"Pass-phrase\\":\\"[^"]*"/\\"Pass-phrase\\":\\"\\"/g')

printf '{"agentId":"mesh-%s","devices":[{"mac":"%s","host":"%s","mode":"LIVE","topology":%s,"systemInfo":%s,"board":%s,"hostHints":%s,"assoc":{%s}}]}' \
    "$MAC" "$MAC" "$IP" "$TOPO" "$(j system info)" "$(j system board)" "$(j luci-rpc getHostHints)" "$ASSOC" > "$TMP"

CODE=$(curl -s -m 20 -o /dev/null -w '%{http_code}' -X POST \
    -H 'Content-Type: application/json' -H "X-Agent-Token: $TOKEN" \
    --data-binary @"$TMP" "$SERVER/api/v1/agent/mesh/snapshots" 2>/dev/null)
rm -f "$TMP"
# Ghi syslog khi ket qua thay doi (khong spam moi phut)
LAST=$(cat /tmp/vt-mesh-agent.last 2>/dev/null)
if [ "$CODE" != "$LAST" ]; then
    echo "$CODE" > /tmp/vt-mesh-agent.last
    if [ "$CODE" = "200" ]; then logger -t vt-mesh-agent "gui du lieu OK len $SERVER"
    else logger -t vt-mesh-agent "gui that bai, HTTP ${CODE:-000}"; fi
fi
exit 0
