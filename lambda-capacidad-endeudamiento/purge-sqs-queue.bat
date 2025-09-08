@echo off
echo Limpiando cola SQS resultados-evaluacion-queue...

REM Usar curl para purgar las colas (sin AWS CLI)
curl -X POST "http://localhost:4566/000000000000/resultados-evaluacion-queue" -d "Action=PurgeQueue"

echo.
echo Cola resultados purgada.
echo.
echo Limpiando cola de solicitudes...
curl -X POST "http://localhost:4566/000000000000/solicitudes-capacidad-queue" -d "Action=PurgeQueue"

echo.
echo Ambas colas limpiadas exitosamente.
pause
