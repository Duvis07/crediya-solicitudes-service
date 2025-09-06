@echo off
echo ========================================
echo  Desplegando Lambda en LocalStack
echo ========================================

echo.
echo 1. Verificando LocalStack...
curl -s http://localhost:4566/_localstack/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: LocalStack no está ejecutándose
    exit /b 1
)
echo ✓ LocalStack OK

echo.
echo 2. Creando colas SQS...
curl -s -X POST "http://localhost:4566/_aws/sqs" -H "Content-Type: application/x-www-form-urlencoded" -d "Action=CreateQueue&QueueName=solicitudes-capacidad-queue&Version=2012-11-05" >nul 2>&1
curl -s -X POST "http://localhost:4566/_aws/sqs" -H "Content-Type: application/x-www-form-urlencoded" -d "Action=CreateQueue&QueueName=resultados-evaluacion-queue&Version=2012-11-05" >nul 2>&1
echo ✓ Colas SQS creadas

echo.
echo 3. Empaquetando Lambda...
cd lambda-capacidad-endeudamiento
if exist lambda-package.zip del lambda-package.zip
powershell -Command "Compress-Archive -Path lambda_function.py,requirements.txt -DestinationPath lambda-package.zip -Force"
cd ..
echo ✓ Lambda empaquetada

echo.
echo 4. Desplegando función Lambda...
powershell -Command "$bytes = [System.IO.File]::ReadAllBytes('lambda-capacidad-endeudamiento/lambda-package.zip'); $base64 = [System.Convert]::ToBase64String($bytes); $json = @{FunctionName='capacidad-endeudamiento';Runtime='python3.9';Role='arn:aws:iam::000000000000:role/lambda-role';Handler='lambda_function.lambda_handler';Code=@{ZipFile=$base64};Environment=@{Variables=@{SQS_QUEUE_URL='http://localhost:4566/000000000000/resultados-evaluacion-queue'}}} | ConvertTo-Json -Depth 3; Invoke-RestMethod -Uri 'http://localhost:4566/2015-03-31/functions' -Method POST -Body $json -ContentType 'application/json'"
echo ✓ Lambda desplegada

echo.
echo 5. Probando Lambda...
powershell -Command "$payload = @{documento_identidad='12345678';monto_solicitado=5000000;ingresos_mensuales=2000000;gastos_mensuales=800000;tipo_prestamo='Personal'} | ConvertTo-Json; $body = @{Payload=$payload} | ConvertTo-Json; Invoke-RestMethod -Uri 'http://localhost:4566/2015-03-31/functions/capacidad-endeudamiento/invocations' -Method POST -Body $body -ContentType 'application/json'"

echo.
echo ========================================
echo  Lambda desplegada exitosamente
echo ========================================
echo.
echo Servicios disponibles:
echo - LocalStack: http://localhost:4566
echo - MailHog: http://localhost:8025
echo - Solicitudes Service: http://localhost:8081
echo.
pause
