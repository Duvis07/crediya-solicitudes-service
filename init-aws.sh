#!/bin/bash

echo "Initializing AWS resources in LocalStack..."

# Create SQS Queues
echo "Creating SQS queues..."

awslocal sqs create-queue --queue-name solicitudes-capacidad-queue
awslocal sqs create-queue --queue-name resultados-evaluacion-queue
awslocal sqs create-queue --queue-name notificaciones-manuales-queue
awslocal sqs create-queue --queue-name loan-approved-events-queue

echo "SQS queues created successfully!"

# List created queues
echo "Available SQS queues:"
awslocal sqs list-queues

echo "AWS resources initialization completed!"
