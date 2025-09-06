@echo off
echo ========================================
echo  Desplegando Lambda en LocalStack
echo ========================================

echo.
echo 1. Verificando que LocalStack esté ejecutándose...
curl -s http://localhost:4566/_localstack/health | findstr "running" >nul
if %errorlevel% neq 0 (
    echo ERROR: LocalStack no está ejecutándose en localhost:4566
    echo Por favor, ejecute: podman-compose up -d
    pause
    exit /b 1
)
echo ✓ LocalStack está ejecutándose

echo.
echo 2. Creando colas SQS...
curl -s -X POST "http://localhost:4566/moto-api/reset" >nul 2>&1
curl -s -X POST "http://localhost:4566/" -H "Content-Type: application/x-amz-json-1.0" -H "X-Amz-Target: AWSSimpleQueueService.CreateQueue" -d "{\"QueueName\":\"solicitudes-capacidad-queue\"}" >nul 2>&1
curl -s -X POST "http://localhost:4566/" -H "Content-Type: application/x-amz-json-1.0" -H "X-Amz-Target: AWSSimpleQueueService.CreateQueue" -d "{\"QueueName\":\"resultados-evaluacion-queue\"}" >nul 2>&1
echo ✓ Colas SQS creadas

echo.
echo 3. Creando tabla DynamoDB para préstamos...
aws --endpoint-url=http://localhost:4566 dynamodb create-table ^
    --table-name prestamos ^
    --attribute-definitions AttributeName=documento_identidad,AttributeType=S ^
    --key-schema AttributeName=documento_identidad,KeyType=HASH ^
    --billing-mode PAY_PER_REQUEST ^
    --region us-east-1
echo ✓ Tabla DynamoDB creada

echo.
echo 4. Creando topic SNS para notificaciones...
aws --endpoint-url=http://localhost:4566 sns create-topic --name capacidad-evaluacion-results --region us-east-1
echo ✓ Topic SNS creado

echo.
echo 5. Empaquetando función Lambda...
cd lambda-capacidad-endeudamiento
if exist lambda-package.zip del lambda-package.zip
powershell -Command "Compress-Archive -Path lambda_function.py,requirements.txt -DestinationPath lambda-package.zip -Force"
echo ✓ Lambda empaquetada

echo.
echo 6. Desplegando función Lambda...
aws --endpoint-url=http://localhost:4566 lambda create-function ^
    --function-name capacidad-endeudamiento-function ^
    --runtime python3.9 ^
    --role arn:aws:iam::000000000000:role/lambda-role ^
    --handler lambda_function.lambda_handler ^
    --zip-file fileb://lambda-package.zip ^
    --timeout 30 ^
    --memory-size 256 ^
    --region us-east-1
echo ✓ Función Lambda desplegada

echo.
echo 7. Configurando variables de entorno de la Lambda...
aws --endpoint-url=http://localhost:4566 lambda update-function-configuration ^
    --function-name capacidad-endeudamiento-function ^
    --environment Variables="{DYNAMODB_ENDPOINT=http://localhost:4566,SNS_ENDPOINT=http://localhost:4566,AWS_DEFAULT_REGION=us-east-1}" ^
    --region us-east-1
echo ✓ Variables de entorno configuradas

echo.
echo 8. Probando función Lambda...
echo {"body":{"solicitud_id":"TEST-001","documento_identidad":"12345678","monto":50000,"plazo_meses":24,"tasa_interes_anual":12.5,"salario_base":3000000,"tipo_prestamo":"PERSONAL","email":"test@example.com","nombre_completo":"Usuario Test","timestamp":1234567890}} > test-event.json

aws --endpoint-url=http://localhost:4566 lambda invoke ^
    --function-name capacidad-endeudamiento-function ^
    --payload file://test-event.json ^
    --region us-east-1 ^
    response.json

echo.
echo Respuesta de la Lambda:
type response.json
echo.

echo.
echo ========================================
echo  Despliegue completado exitosamente
echo ========================================
echo.
echo Servicios disponibles:
echo - LocalStack: http://localhost:4566
echo - MailHog Web UI: http://localhost:8025
echo.
echo Para probar la integración completa:
echo 1. Inicie el microservicio solicitudes-service
echo 2. Cree una nueva solicitud de crédito
echo 3. Verifique los logs y emails en MailHog
echo.

cd ..
pause
