@echo off
echo ========================================
echo REINICIO LIMPIO - SOLICITUDES SERVICE
echo ========================================

echo.
echo 1. Parando solo el servicio solicitudes-service...
podman stop crediya-solicitudes-service 2>nul
podman stop crediya-postgres-solicitudes 2>nul

echo.
echo 2. Eliminando solo volumenes del proyecto...
podman volume rm crediya-postgres-solicitudes_postgres_solicitudes_data 2>nul

echo.
echo 3. Eliminando todas las imagenes del proyecto...
podman rmi -f localhost/solicitudes-service_solicitudes-service:latest 2>nul
podman rmi -f localhost/crediya-solicitudes_solicitudes-service:latest 2>nul
podman rmi -f localhost/crediya/solicitudes-service:latest 2>nul
podman rmi -f crediya-solicitudes_solicitudes-service:latest 2>nul
podman rmi -f crediya/solicitudes-service:latest 2>nul
podman rmi -f solicitudes-service_solicitudes-service:latest 2>nul

echo.
echo 4. Eliminando solo contenedores del proyecto...
podman rm crediya-solicitudes-service 2>nul
podman rm crediya-postgres-solicitudes 2>nul

echo.
echo 5. Limpiando solo cache de imagenes (sin volumenes)...
podman system prune -f

echo.
echo 6. Reconstruyendo imagen...
call scripts\build.bat

echo.
echo 7. Iniciando solo servicios del proyecto...
podman-compose up -d

echo.
echo 8. Esperando que los servicios esten listos...
timeout /t 30 /nobreak

echo.
echo 9. Verificando estado de los servicios...
podman-compose ps

echo.
echo ========================================
echo REINICIO COMPLETADO
echo ========================================
echo.
echo Para probar los endpoints:
echo curl -X GET http://localhost:8081/actuator/health
echo.
