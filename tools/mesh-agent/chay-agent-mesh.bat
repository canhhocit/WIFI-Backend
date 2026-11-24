@echo off
chcp 65001 >nul
title Agent giam sat mesh AP - Wi-Fi Hai Phong
cd /d "%~dp0"

rem ==== Cau hinh ====
set "SERVER=https://wifi-haiphong.duckdns.org"
set "INTERVAL=60"

echo ============================================================
echo   AGENT GIAM SAT MESH AP  -^>  %SERVER%
echo   Doc mesh moi %INTERVAL% giay va gui len he thong.
echo   Giu cua so nay MO trong suot buoi demo (dong lai = AP bao OFFLINE sau 5 phut).
echo   Dung agent: nhan Ctrl+C.
echo ============================================================
echo.

where python >nul 2>&1
if errorlevel 1 (
    echo [LOI] Khong tim thay Python. Cai Python 3 tu https://www.python.org roi chay lai.
    pause
    exit /b 1
)

if not exist ".agent-token" (
    if "%MESH_AGENT_TOKEN%"=="" (
        echo [LOI] Thieu file .agent-token canh file nay ^(token de he thong chap nhan agent^).
        pause
        exit /b 1
    )
)

echo Nhap S/N cua mesh khi duoc hoi ^(go se khong hien ky tu^).
echo.
python -I "%~dp0mesh_agent.py" --server %SERVER% --interval %INTERVAL%

echo.
echo Agent da dung.
pause
