@echo off
echo ========================================
echo  Iniciando Contenedores - HU7
echo ========================================
echo.
echo 🐳 Levantando contenedores con Podman:
echo   - LocalStack (AWS Services)
echo   - MailHog (Email Testing)
echo.

cd "c:\Users\duvan.botero\Documents\Reto Bootcamp"

echo 🚀 Ejecutando podman-compose up -d...
podman-compose up -d

echo.
echo ✅ Contenedores iniciados!
echo.
echo 📋 Contenedores iniciados para HU7:
echo   - LocalStack (AWS Services)
echo   - MailHog (Email Testing)
echo.
echo 🔍 Para verificar estado: podman ps
echo 📧 Para ver logs: podman-compose logs -f [servicio]
echo.

pause
