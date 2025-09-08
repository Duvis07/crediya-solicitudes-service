@echo off
echo ========================================
echo  Inicializando Colas SQS para HU7
echo ========================================

echo.
echo 1. Verificando LocalStack...
curl -s http://localhost:4566/_localstack/health | findstr "sqs" >nul
if %errorlevel% neq 0 (
    echo ERROR: LocalStack SQS no disponible
    pause
    exit /b 1
)
echo ✓ LocalStack SQS disponible

echo.
echo 2. Creando cola solicitudes-capacidad-queue...
curl -X POST "http://localhost:4566/" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "Action=CreateQueue&QueueName=solicitudes-capacidad-queue&Version=2012-11-05"
echo ✓ Cola solicitudes creada

echo.
echo 3. Creando cola resultados-evaluacion-queue...
curl -X POST "http://localhost:4566/" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "Action=CreateQueue&QueueName=resultados-evaluacion-queue&Version=2012-11-05"
echo ✓ Cola resultados creada

echo.
echo 4. Listando colas disponibles...
curl -X POST "http://localhost:4566/" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "Action=ListQueues&Version=2012-11-05"

echo.
echo ========================================
echo  Colas SQS inicializadas correctamente
echo ========================================
echo.
echo Ahora puede:
echo 1. Ejecutar el simulador: python auto-lambda-simulator.py
echo 2. Crear solicitudes en Postman
echo 3. Ver procesamiento automático
echo.

pause
