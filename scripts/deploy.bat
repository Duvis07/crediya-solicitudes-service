@echo off
REM CrediYa Solicitudes Service - Deploy Script for Docker (Windows)
REM This script manages deployment operations for the solicitudes service

setlocal enabledelayedexpansion

REM Configuration
set COMPOSE_FILE=docker-compose.yml
set SERVICE_NAME=solicitudes-service

REM Function to display usage
if "%1"=="" goto usage
if "%1"=="help" goto usage

REM Check if podman-compose is available
where podman-compose >nul 2>nul
if !errorlevel! neq 0 (
    echo podman-compose not found. Please install: pip install podman-compose
    exit /b 1
)

REM Main script logic
if "%1"=="up" goto start_services
if "%1"=="down" goto stop_services
if "%1"=="restart" goto restart_services
if "%1"=="logs" goto show_logs
if "%1"=="status" goto show_status
if "%1"=="build" goto build_and_start
goto usage

:start_services
echo Starting CrediYa Solicitudes Service...
podman-compose -f %COMPOSE_FILE% up -d
if !errorlevel! neq 0 (
    echo Failed to start services
    exit /b 1
)
echo Services started successfully
echo Service URLs:
echo    • Solicitudes API: http://localhost:8081
echo    • Health Check: http://localhost:8081/actuator/health
echo    • Swagger UI: http://localhost:8081/swagger-ui.html
goto end

:stop_services
echo Stopping CrediYa Solicitudes Service...
podman-compose -f %COMPOSE_FILE% down
echo Services stopped successfully
goto end

:restart_services
echo Restarting CrediYa Solicitudes Service...
call :stop_services
call :start_services
goto end

:show_logs
echo Showing service logs...
podman-compose -f %COMPOSE_FILE% logs -f
goto end

:show_status
echo Service Status:
podman-compose -f %COMPOSE_FILE% ps
echo.
echo Docker Images:
podman images | findstr "crediya postgres" 2>nul || echo No CrediYa images found
goto end

:build_and_start
echo Building and starting services...
call scripts\build.bat
if !errorlevel! neq 0 exit /b 1
call :start_services
goto end

:usage
echo Usage: %0 {up^|down^|restart^|logs^|status^|build}
echo Commands:
echo   up      - Start all services
echo   down    - Stop and remove all services
echo   restart - Restart all services
echo   logs    - Show service logs
echo   status  - Show service status
echo   build   - Build and start services
exit /b 1

:end
endlocal
