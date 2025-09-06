@echo off
echo Configurando servicios AWS en LocalStack para HU7...

REM Configurar variables de entorno para LocalStack
set AWS_ENDPOINT_URL=http://localhost:4566
set AWS_DEFAULT_REGION=us-east-1
set AWS_ACCESS_KEY_ID=test
set AWS_SECRET_ACCESS_KEY=test

echo.
echo === Creando bucket S3 para documentos ===
aws --endpoint-url=%AWS_ENDPOINT_URL% s3 mb s3://solicitudes-documents

echo.
echo === Creando tabla DynamoDB para metadatos ===
aws --endpoint-url=%AWS_ENDPOINT_URL% dynamodb create-table ^
    --table-name DocumentMetadata ^
    --attribute-definitions ^
        AttributeName=documentId,AttributeType=S ^
        AttributeName=solicitudId,AttributeType=S ^
    --key-schema ^
        AttributeName=documentId,KeyType=HASH ^
        AttributeName=solicitudId,KeyType=RANGE ^
    --billing-mode PAY_PER_REQUEST

echo.
echo === Creando colas SQS para HU7 ===
aws --endpoint-url=%AWS_ENDPOINT_URL% sqs create-queue ^
    --queue-name solicitudes-capacidad-queue

aws --endpoint-url=%AWS_ENDPOINT_URL% sqs create-queue ^
    --queue-name resultados-evaluacion-queue

echo.
echo === Creando topico SNS para notificaciones ===
aws --endpoint-url=%AWS_ENDPOINT_URL% sns create-topic ^
    --name document-notifications

echo.
echo === Verificando identidad SES para emails ===
aws --endpoint-url=%AWS_ENDPOINT_URL% ses verify-email-identity ^
    --email-address noreply@crediya.com

echo.
echo === Configuracion completada ===
echo S3 Bucket: solicitudes-documents
echo DynamoDB: DocumentMetadata
echo SQS Queues: solicitudes-capacidad-queue, resultados-evaluacion-queue
echo SNS Topic: document-notifications
echo SES Email: noreply@crediya.com
echo.
echo LocalStack Dashboard: http://localhost:4566
echo MailHog Web UI: http://localhost:8025
echo.
pause
