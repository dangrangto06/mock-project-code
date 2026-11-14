@echo off
title CodeForge Runner
echo ========================================================
echo Starting CodeForge (Frontend + Spring Boot Backend)
echo ========================================================

echo.
echo [1/2] Opening Frontend in default web browser...
start "" "%~dp0frontend\index.html"

echo.
echo [2/2] Starting Spring Boot Backend with Dev Profile (H2 DB)...
cd /d "%~dp0backend"
if exist "C:\Users\Aquatic\.maven\apache-maven-3.9.15\bin\mvn.cmd" (
    call "C:\Users\Aquatic\.maven\apache-maven-3.9.15\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
) else (
    call mvn spring-boot:run -Dspring-boot.run.profiles=dev
)

pause

