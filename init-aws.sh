#!/bin/bash

echo "Initializing AWS resources in LocalStack..."

# Create SQS Queues
echo "Creating SQS queues..."

awslocal sqs create-queue --queue-name solicitudes-capacidad-queue
awslocal sqs create-queue --queue-name resultados-evaluacion-queue
awslocal sqs create-queue --queue-name notificaciones-manuales-queue
awslocal sqs create-queue --queue-name loan-approved-events-queue

echo "SQS queues created successfully!"

# Create DynamoDB table for reportes-service
echo "Creating DynamoDB table for reportes-service..."
awslocal dynamodb create-table \
    --table-name loan-reports \
    --attribute-definitions \
        AttributeName=id,AttributeType=S \
    --key-schema \
        AttributeName=id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5

echo "DynamoDB table created successfully!"

# List created queues
echo "Available SQS queues:"
awslocal sqs list-queues

# List created tables
echo "Available DynamoDB tables:"
awslocal dynamodb list-tables

echo "AWS resources initialization completed!"
