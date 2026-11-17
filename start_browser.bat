@echo off
chcp 65001 >nul
title Simple Web Browser (Lab 4) - VKU
cd /d "%~dp0"
echo ========================================================
echo       KHỞI ĐỘNG SIMPLE WEB BROWSER (LAB 4)
echo ========================================================
echo Đang biên dịch SimpleBrowser.java...
javac -encoding UTF-8 -cp ".;postgresql-connector.jar;mysql-connector.jar" SimpleBrowser.java
if %errorlevel% neq 0 (
    echo [LỖI] Biên dịch SimpleBrowser.java thất bại!
    pause
    exit /b %errorlevel%
)
echo Khởi chạy giao diện trình duyệt...
start "SimpleBrowser" java -cp ".;postgresql-connector.jar;mysql-connector.jar" SimpleBrowser
