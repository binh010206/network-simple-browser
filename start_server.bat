@echo off
chcp 65001 >nul
title HTTP Server (Lab 4) - VKU
cd /d "%~dp0"
echo ========================================================
echo       KHỞI ĐỘNG HTTP WEB SERVER (LAB 4)
echo ========================================================
echo Đang biên dịch httpServer.java...
javac -encoding UTF-8 httpServer.java
if %errorlevel% neq 0 (
    echo [LỖI] Biên dịch httpServer.java thất bại!
    pause
    exit /b %errorlevel%
)
echo.
java httpServer
pause
