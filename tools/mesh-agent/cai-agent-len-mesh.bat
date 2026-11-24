@echo off
chcp 65001 >nul
title Cai agent len mesh AP - Wi-Fi Hai Phong
cd /d "%~dp0"

rem Cach dung:  cai-agent-len-mesh.bat                 (cai len 192.168.80.111)
rem             cai-agent-len-mesh.bat 192.168.80.112  (cai len mesh khac)
rem             cai-agent-len-mesh.bat 192.168.80.111 --status | --uninstall
set "HOST=%~1"
if "%HOST%"=="" set "HOST=192.168.80.111"

where python >nul 2>&1
if errorlevel 1 (
    echo [LOI] Khong tim thay Python 3.
    pause
    exit /b 1
)

echo Nhap S/N cua mesh %HOST% khi duoc hoi ^(go se khong hien ky tu^).
python -I "%~dp0install_on_mesh.py" --host %HOST% %2
echo.
pause
