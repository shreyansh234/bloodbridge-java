@echo off
cd /d "%~dp0"
if not exist .env powershell -NoProfile -File setup-admin.ps1
if errorlevel 1 exit /b 1
call mvn spring-boot:run -Dspring-boot.run.profiles=demo
pause
