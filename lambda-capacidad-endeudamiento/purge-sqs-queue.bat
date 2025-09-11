@echo off
echo Limpiando todas las colas SQS...

REM Usar curl para purgar las colas (sin AWS CLI)
echo Limpiando cola resultados-evaluacion-queue...
curl -X POST "http://localhost:4566/000000000000/resultados-evaluacion-queue" -d "Action=PurgeQueue"

echo.
echo Limpiando cola solicitudes-capacidad-queue...
curl -X POST "http://localhost:4566/000000000000/solicitudes-capacidad-queue" -d "Action=PurgeQueue"

echo.
echo Limpiando cola notificaciones-manuales-queue...
curl -X POST "http://localhost:4566/000000000000/notificaciones-manuales-queue" -d "Action=PurgeQueue"

echo.
echo Todas las colas limpiadas exitosamente.
pause
