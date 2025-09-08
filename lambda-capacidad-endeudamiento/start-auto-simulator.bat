@echo off
echo ========================================
echo  Auto Lambda Simulator - HU7
echo ========================================
echo.
echo ✅ Simulador automático que:
echo   - Monitorea cola solicitudes-capacidad-queue cada 15s
echo   - Procesa mensajes automáticamente
echo   - Calcula capacidad de endeudamiento
echo   - Envía respuestas a resultados-evaluacion-queue
echo   - COMPLETAMENTE TRANSPARENTE
echo.
echo 🚀 Iniciando simulador...
echo 📧 Presione Ctrl+C para detener
echo.

python auto-lambda-simulator.py

pause
