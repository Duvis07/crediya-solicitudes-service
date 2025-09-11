@echo off
echo ========================================
echo REINICIO LIMPIO - SOLICITUDES SERVICE
echo ========================================

echo.
echo 1. Parando todos los servicios...
podman-compose down

echo.
echo 2. Eliminando volumenes para limpiar BD...
podman-compose down -v

echo.
echo 3. Eliminando todas las imagenes del proyecto...
podman rmi -f localhost/solicitudes-service_solicitudes-service:latest 2>nul
podman rmi -f localhost/crediya-solicitudes_solicitudes-service:latest 2>nul
podman rmi -f localhost/crediya/solicitudes-service:latest 2>nul
podman rmi -f crediya-solicitudes_solicitudes-service:latest 2>nul
podman rmi -f crediya/solicitudes-service:latest 2>nul
podman rmi -f solicitudes-service_solicitudes-service:latest 2>nul

echo.
echo 4. Eliminando contenedores parados...
podman container prune -f

echo.
echo 5. Limpiando cache de imagenes y volumenes...
podman system prune -f --volumes

echo.
echo 6. Reconstruyendo imagen...
call scripts\build.bat

echo.
echo 7. Iniciando servicios desde cero...
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
