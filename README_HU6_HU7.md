# README - Historias de Usuario 6 y 7
## Sistema de Notificaciones y Cálculo de Capacidad de Pago

### 📋 Tabla de Contenidos
1. [Visión General](#visión-general)
2. [Historia de Usuario 6 - Sistema de Notificaciones](#historia-de-usuario-6---sistema-de-notificaciones)
3. [Historia de Usuario 7 - Cálculo de Capacidad](#historia-de-usuario-7---cálculo-de-capacidad)
4. [Arquitectura del Sistema](#arquitectura-del-sistema)
5. [Flujos de Datos](#flujos-de-datos)
6. [Configuración y Despliegue](#configuración-y-despliegue)

---

## 🎯 Visión General

Las Historias de Usuario 6 y 7 implementan un sistema completo de notificaciones automáticas y cálculo de capacidad de pago que utiliza:

- **AWS SQS** para colas de mensajes asíncronos
- **AWS Lambda** para procesamiento automático
- **Sistema de Cache** para resultados en tiempo real
- **Notificaciones por Email** automáticas
- **Validación de Usuarios** con authentication-service

---

## 📧 Historia de Usuario 6 - Sistema de Notificaciones

### Funcionalidad Principal
Envío automático de notificaciones por email cuando cambia el estado de una solicitud de préstamo.

### 🔄 Flujo Completo del Sistema

#### 1. **Actualización Manual de Estado**
```http
PUT /api/v1/applications/{applicationId}/status
Content-Type: application/json

{
    "status": "Aprobada",
    "comments": "Solicitud aprobada tras revisión"
}
```

#### 2. **Procesamiento en el Handler**
```java
// Handler.updateApplicationStatus()
return request.bodyToMono(UpdateApplicationStatusRequest.class)
    .flatMap(req -> requestValidator.validate(req, "updateApplicationStatusRequest"))
    .flatMap(validatedRequest -> 
        updateApplicationStatusUseCase.updateApplicationStatus(
            applicationId,
            validatedRequest.getStatus(),
            validatedRequest.getComments()))
```

#### 3. **Lógica de Negocio en UseCase**
```java
// UpdateApplicationStatusUseCase.updateApplicationStatus()
return validateStateTransition(applicationId, targetStatus)
    .then(updateStatus(applicationId, targetStatus, comments))
    .flatMap(updatedApplication -> 
        sendNotificationToSQS(updatedApplication, comments)
            .thenReturn(updatedApplication))
```

#### 4. **Envío a Cola SQS**
```java
// ManualDecisionAdapter.sendManualDecision()
ManualDecisionMessage message = ManualDecisionMessage.builder()
    .applicationId(application.getId())
    .documentId(application.getDocumentId())
    .email(application.getEmail())
    .decision(application.getState().getName())
    .comments(comments)
    .timestamp(LocalDateTime.now())
    .build();

return messageQueueService.sendMessage(message, queueUrl);
```

#### 5. **Estructura del Mensaje SQS**
```json
{
    "applicationId": "12345",
    "documentId": "11223344",
    "email": "cliente@crediya.com",
    "decision": "Aprobada",
    "comments": "Solicitud aprobada tras revisión",
    "timestamp": "2025-09-11T15:30:00"
}
```

#### 6. **Procesamiento por Lambda**
La Lambda recibe el mensaje y:
- Valida los datos del mensaje
- Selecciona la plantilla de email apropiada
- Genera el contenido personalizado
- Envía el email a través de AWS SES

#### 7. **Plantillas de Email**
```
📧 Aprobada: "¡Felicitaciones! Tu solicitud ha sido aprobada"
📧 Rechazada: "Información sobre tu solicitud de préstamo"
📧 En Revisión: "Tu solicitud está siendo procesada"
```

### 🛡️ Validaciones Implementadas

#### Transiciones de Estado Válidas
```java
private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
    "Pendiente de revision", Set.of("En revision", "Rechazada"),
    "En revision", Set.of("Aprobada", "Rechazada", "Pendiente de revision"),
    "Aprobada", Set.of("Rechazada"),
    "Rechazada", Set.of("En revision")
);
```

#### Manejo de Errores
- `InvalidStateTransitionException`: Transición no permitida
- `ApplicationNotFoundException`: Solicitud no existe
- `EmailNotificationException`: Error en envío de notificación

---

## 💰 Historia de Usuario 7 - Cálculo de Capacidad

### Funcionalidad Principal
Cálculo automático de capacidad de pago mediante procesamiento asíncrono con Lambda.

### 🔄 Flujo Completo del Sistema

#### 1. **Request de Cálculo**
```http
POST /api/v1/calculate-capacity
Content-Type: application/json

{
    "documentoIdentidad": "11223344",
    "email": "cliente@crediya.com",
    "monto": 5000000,
    "plazoMeses": 12,
    "tasaInteresAnual": 12.0,
    "salarioBase": 1000000
}
```

#### 2. **Validación de Usuario**
```java
// CapacityCalculationUseCase.validateClient()
return clientValidationRepository.getUserEmailByDocumentId(documentId)
    .switchIfEmpty(Mono.error(new ClientNotFoundException()))
    .flatMap(userEmail -> {
        if (!userEmail.equals(requestEmail.trim())) {
            return Mono.error(new ClientNotFoundException(
                "Access denied: You can only calculate capacity for yourself"));
        }
        return Mono.empty();
    });
```

#### 3. **Envío a Cola SQS para Cálculo**
```java
// AutomaticEvaluationAdapter.sendForCapacityCalculation()
EvaluationRequestDto evaluationRequest = EvaluationRequestDto.builder()
    .applicationId("CAPACITY_CALC_" + System.currentTimeMillis())
    .documentId(request.getDocumentoIdentidad())
    .email(request.getEmail())
    .requestedAmount(request.getMonto())
    .termMonths(request.getPlazoMeses())
    .annualInterestRate(request.getTasaInteresAnual())
    .baseSalary(request.getSalarioBase())
    .build();

return messageQueueService.sendMessage(evaluationRequest, queueUrl);
```

#### 4. **Procesamiento por Lambda de Cálculo**

##### Algoritmo de Cálculo Implementado:
```python
def calculate_capacity(salary, requested_amount, term_months, interest_rate):
    # 1. Calcular capacidad máxima (30% del salario)
    monthly_capacity = salary * 0.30
    
    # 2. Calcular cuota mensual del préstamo solicitado
    monthly_rate = interest_rate / 100 / 12
    monthly_payment = (requested_amount * monthly_rate * (1 + monthly_rate)**term_months) / ((1 + monthly_rate)**term_months - 1)
    
    # 3. Determinar decisión
    if monthly_payment <= monthly_capacity:
        decision = "APROBADA"
        approved_amount = requested_amount
    else:
        decision = "PARCIAL"
        # Calcular monto máximo aprobable
        max_amount = (monthly_capacity * ((1 + monthly_rate)**term_months - 1)) / (monthly_rate * (1 + monthly_rate)**term_months)
        approved_amount = max_amount
    
    # 4. Generar plan de pagos
    payment_plan = generate_payment_schedule(approved_amount, term_months, interest_rate)
    
    return {
        "decision": decision,
        "approvedAmount": approved_amount,
        "monthlyPayment": monthly_payment,
        "paymentPlan": payment_plan
    }
```

#### 5. **Sistema de Cache y Espera**
```java
// Handler.calculateCapacity()
return capacityResultCache.waitForResult(messageId, Duration.ofSeconds(25))
    .flatMap(result -> {
        CalculateCapacityResponse response = CalculateCapacityResponse.builder()
            .decision(result.getDecision())
            .motivo(result.getMotivo())
            .capacidadDisponible(result.getCapacidadDisponible())
            .cuotaCalculada(result.getCuotaCalculada())
            .montoAprobado(result.getMontoAprobado())
            .tasaInteresAnual(result.getTasaInteresAnual())
            .plazoMeses(result.getPlazoMeses())
            .cuotaMensual(result.getCuotaMensual())
            .planPagos(result.getPlanPagos())
            .build();
        
        return ServerResponse.ok().bodyValue(response);
    })
    .onErrorResume(timeoutError -> {
        return ServerResponse.status(408)
            .bodyValue(Map.of(
                "error", "Request timeout",
                "message", "Capacity calculation is taking longer than expected"
            ));
    });
```

#### 6. **Respuesta del Cálculo**
```json
{
    "decision": "APROBADA",
    "motivo": "Capacidad de pago suficiente",
    "capacidadDisponible": 300000,
    "cuotaCalculada": 456789.12,
    "montoAprobado": 5000000,
    "tasaInteresAnual": 12.0,
    "plazoMeses": 12,
    "cuotaMensual": 456789.12,
    "planPagos": [
        {
            "numeroCuota": 1,
            "fechaVencimiento": "2025-10-11",
            "valorCuota": 456789.12,
            "capital": 406789.12,
            "interes": 50000.00,
            "saldoPendiente": 4593210.88
        }
    ]
}
```

### 🎯 Tipos de Decisiones

#### APROBADA
- La cuota mensual ≤ 30% del salario
- Se aprueba el monto completo solicitado

#### PARCIAL  
- La cuota mensual > 30% del salario
- Se calcula y aprueba un monto menor
- Se ajusta el plan de pagos

#### RECHAZADA
- Salario insuficiente para cualquier monto
- No se genera plan de pagos

---

## 🏗️ Arquitectura del Sistema

### Componentes Principales

#### 1. **Entry Points (Controllers)**
```
├── Handler.java
│   ├── updateApplicationStatus()     # HU 6
│   └── calculateCapacity()           # HU 7
└── RouterRest.java                   # Configuración de rutas
```

#### 2. **Use Cases (Lógica de Negocio)**
```
├── UpdateApplicationStatusUseCase.java    # HU 6
│   ├── validateStateTransition()
│   ├── updateStatus()
│   └── sendNotificationToSQS()
└── CapacityCalculationUseCase.java        # HU 7
    ├── validateClient()
    └── sendForCapacityCalculation()
```

#### 3. **Adapters (Infraestructura)**
```
├── ManualDecisionAdapter.java         # HU 6 - Envío a SQS
├── AutomaticEvaluationAdapter.java    # HU 7 - Envío a SQS
├── MessageQueueService.java           # Servicio SQS común
├── EvaluationResultConsumer.java      # Consumer de resultados
└── CapacityResultCache.java           # Cache de resultados
```

#### 4. **External Services**
```
├── AuthServiceClient.java             # Validación de usuarios
├── AWS SQS                           # Colas de mensajes
├── AWS Lambda                        # Procesamiento automático
└── AWS SES                           # Envío de emails
```

### 🔄 Flujo de Datos

#### HU 6 - Notificaciones
```
[Cliente] → [Handler] → [UseCase] → [Adapter] → [SQS] → [Lambda] → [SES] → [Email]
```

#### HU 7 - Cálculo de Capacidad
```
[Cliente] → [Handler] → [UseCase] → [Adapter] → [SQS] → [Lambda] → [SQS Response] → [Cache] → [Cliente]
```

---

## ⚙️ Configuración y Despliegue

### Variables de Entorno Requeridas

#### AWS Configuration
```properties
# SQS Queues
aws.sqs.manual-decision-queue=manual-decision-queue
aws.sqs.automatic-evaluation-queue=automatic-evaluation-queue
aws.sqs.evaluation-result-queue=evaluation-result-queue

# AWS Credentials
aws.accessKeyId=${AWS_ACCESS_KEY_ID}
aws.secretKey=${AWS_SECRET_ACCESS_KEY}
aws.region=${AWS_REGION:us-east-1}
```

#### Authentication Service
```properties
# Auth Service URLs
auth.service.base-url=http://localhost:8080
auth.service.get-user-by-document=/api/v1/users/document/{documentId}
```

### Colas SQS Configuradas

#### 1. **manual-decision-queue**
- **Propósito**: Notificaciones de cambios de estado
- **Consumidor**: Lambda de notificaciones
- **Mensaje**: ManualDecisionMessage

#### 2. **automatic-evaluation-queue**
- **Propósito**: Solicitudes de cálculo de capacidad
- **Consumidor**: Lambda de cálculo
- **Mensaje**: EvaluationRequestDto

#### 3. **evaluation-result-queue**
- **Propósito**: Resultados de cálculos
- **Consumidor**: EvaluationResultConsumer
- **Mensaje**: EvaluationResultDto

---

## 🚀 Funciones AWS Lambda - Implementación Detallada

### 📧 Lambda 1: Notification Service (HU 6)

#### **Propósito**
Procesa mensajes de cambios de estado y envía notificaciones por email personalizadas.

#### **Trigger**
- **Fuente**: SQS Queue `manual-decision-queue`
- **Batch Size**: 1 mensaje por invocación
- **Timeout**: 30 segundos

#### **Implementación Completa**
```python
import json
import boto3
import logging
from datetime import datetime
from typing import Dict, Any

# Configuración
ses_client = boto3.client('ses', region_name='us-east-1')
logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event: Dict[str, Any], context) -> Dict[str, Any]:
    """
    Procesa notificaciones de cambio de estado y envía emails
    """
    try:
        # 1. Procesar cada mensaje del batch SQS
        for record in event['Records']:
            message_body = json.loads(record['body'])
            
            # 2. Extraer datos del mensaje
            notification_data = {
                'application_id': message_body['applicationId'],
                'document_id': message_body['documentId'],
                'email': message_body['email'],
                'decision': message_body['decision'],
                'comments': message_body.get('comments', ''),
                'timestamp': message_body['timestamp']
            }
            
            logger.info(f"Processing notification for application: {notification_data['application_id']}")
            
            # 3. Seleccionar plantilla según decisión
            email_template = select_email_template(notification_data['decision'])
            
            # 4. Generar contenido personalizado
            email_content = generate_email_content(email_template, notification_data)
            
            # 5. Enviar email via SES
            send_email_result = send_notification_email(
                notification_data['email'],
                email_content['subject'],
                email_content['body_html'],
                email_content['body_text']
            )
            
            logger.info(f"Email sent successfully. MessageId: {send_email_result['MessageId']}")
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Notifications processed successfully',
                'processed_count': len(event['Records'])
            })
        }
        
    except Exception as e:
        logger.error(f"Error processing notifications: {str(e)}")
        raise e

def select_email_template(decision: str) -> Dict[str, str]:
    """
    Selecciona la plantilla de email según la decisión
    """
    templates = {
        'Aprobada': {
            'template_name': 'loan_approved',
            'subject_prefix': '¡Felicitaciones! Tu solicitud ha sido aprobada'
        },
        'Rechazada': {
            'template_name': 'loan_rejected',
            'subject_prefix': 'Información sobre tu solicitud de préstamo'
        },
        'En revision': {
            'template_name': 'loan_under_review',
            'subject_prefix': 'Tu solicitud está siendo procesada'
        },
        'Pendiente de revision': {
            'template_name': 'loan_pending',
            'subject_prefix': 'Hemos recibido tu solicitud'
        }
    }
    
    return templates.get(decision, templates['En revision'])

def generate_email_content(template: Dict[str, str], data: Dict[str, Any]) -> Dict[str, str]:
    """
    Genera el contenido personalizado del email
    """
    decision = data['decision']
    application_id = data['application_id']
    comments = data['comments']
    timestamp = data['timestamp']
    
    # Plantillas HTML por tipo de decisión
    if decision == 'Aprobada':
        html_body = f"""
        <html>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #28a745;">¡Felicitaciones! 🎉</h2>
                <p>Nos complace informarte que tu solicitud de préstamo ha sido <strong>APROBADA</strong>.</p>
                
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <h3>Detalles de tu solicitud:</h3>
                    <p><strong>Número de solicitud:</strong> {application_id}</p>
                    <p><strong>Estado:</strong> Aprobada</p>
                    <p><strong>Fecha de procesamiento:</strong> {timestamp}</p>
                    {f'<p><strong>Comentarios:</strong> {comments}</p>' if comments else ''}
                </div>
                
                <h3>Próximos pasos:</h3>
                <ul>
                    <li>Recibirás un correo con los documentos de formalización</li>
                    <li>Deberás firmar digitalmente el contrato</li>
                    <li>El desembolso se realizará en 24-48 horas hábiles</li>
                </ul>
                
                <p style="margin-top: 30px;">
                    <strong>¡Gracias por confiar en Crediya!</strong><br>
                    Equipo de Préstamos
                </p>
            </div>
        </body>
        </html>
        """
        
        text_body = f"""
        ¡Felicitaciones!
        
        Tu solicitud de préstamo ha sido APROBADA.
        
        Detalles:
        - Número de solicitud: {application_id}
        - Estado: Aprobada
        - Fecha: {timestamp}
        {f'- Comentarios: {comments}' if comments else ''}
        
        Próximos pasos:
        1. Recibirás documentos de formalización
        2. Firma digital del contrato
        3. Desembolso en 24-48 horas
        
        ¡Gracias por confiar en Crediya!
        """
        
    elif decision == 'Rechazada':
        html_body = f"""
        <html>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #dc3545;">Información sobre tu solicitud</h2>
                <p>Lamentamos informarte que tu solicitud de préstamo no ha sido aprobada en esta ocasión.</p>
                
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <h3>Detalles de tu solicitud:</h3>
                    <p><strong>Número de solicitud:</strong> {application_id}</p>
                    <p><strong>Estado:</strong> No aprobada</p>
                    <p><strong>Fecha de procesamiento:</strong> {timestamp}</p>
                    {f'<p><strong>Motivo:</strong> {comments}</p>' if comments else ''}
                </div>
                
                <h3>¿Qué puedes hacer?</h3>
                <ul>
                    <li>Puedes aplicar nuevamente en 30 días</li>
                    <li>Revisa y mejora tu perfil crediticio</li>
                    <li>Considera solicitar un monto menor</li>
                </ul>
                
                <p style="margin-top: 30px;">
                    <strong>Gracias por tu interés en Crediya</strong><br>
                    Equipo de Préstamos
                </p>
            </div>
        </body>
        </html>
        """
        
        text_body = f"""
        Información sobre tu solicitud
        
        Tu solicitud de préstamo no ha sido aprobada.
        
        Detalles:
        - Número de solicitud: {application_id}
        - Estado: No aprobada
        - Fecha: {timestamp}
        {f'- Motivo: {comments}' if comments else ''}
        
        Opciones:
        1. Aplicar nuevamente en 30 días
        2. Mejorar perfil crediticio
        3. Considerar monto menor
        
        Gracias por tu interés en Crediya
        """
    
    else:  # En revision, Pendiente de revision
        html_body = f"""
        <html>
        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #007bff;">Tu solicitud está siendo procesada</h2>
                <p>Hemos recibido tu solicitud y nuestro equipo la está revisando cuidadosamente.</p>
                
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                    <h3>Detalles de tu solicitud:</h3>
                    <p><strong>Número de solicitud:</strong> {application_id}</p>
                    <p><strong>Estado actual:</strong> {decision}</p>
                    <p><strong>Última actualización:</strong> {timestamp}</p>
                    {f'<p><strong>Comentarios:</strong> {comments}</p>' if comments else ''}
                </div>
                
                <h3>Tiempo estimado de respuesta:</h3>
                <p>Recibirás una respuesta en un plazo máximo de <strong>48 horas hábiles</strong>.</p>
                
                <p style="margin-top: 30px;">
                    <strong>Gracias por tu paciencia</strong><br>
                    Equipo de Préstamos Crediya
                </p>
            </div>
        </body>
        </html>
        """
        
        text_body = f"""
        Tu solicitud está siendo procesada
        
        Hemos recibido tu solicitud y la estamos revisando.
        
        Detalles:
        - Número de solicitud: {application_id}
        - Estado: {decision}
        - Última actualización: {timestamp}
        {f'- Comentarios: {comments}' if comments else ''}
        
        Tiempo estimado: 48 horas hábiles
        
        Gracias por tu paciencia
        Equipo Crediya
        """
    
    return {
        'subject': f"{template['subject_prefix']} - Solicitud #{application_id}",
        'body_html': html_body,
        'body_text': text_body
    }

def send_notification_email(to_email: str, subject: str, html_body: str, text_body: str) -> Dict[str, Any]:
    """
    Envía el email usando AWS SES
    """
    try:
        response = ses_client.send_email(
            Source='noreply@crediya.com',
            Destination={
                'ToAddresses': [to_email]
            },
            Message={
                'Subject': {
                    'Data': subject,
                    'Charset': 'UTF-8'
                },
                'Body': {
                    'Html': {
                        'Data': html_body,
                        'Charset': 'UTF-8'
                    },
                    'Text': {
                        'Data': text_body,
                        'Charset': 'UTF-8'
                    }
                }
            }
        )
        
        logger.info(f"Email sent to {to_email}. MessageId: {response['MessageId']}")
        return response
        
    except Exception as e:
        logger.error(f"Failed to send email to {to_email}: {str(e)}")
        raise e
```

---

### 💰 Lambda 2: Capacity Calculation Service (HU 7)

#### **Propósito**
Calcula la capacidad de pago del solicitante y genera un plan de pagos detallado.

#### **Trigger**
- **Fuente**: SQS Queue `automatic-evaluation-queue`
- **Batch Size**: 1 mensaje por invocación
- **Timeout**: 60 segundos

#### **Implementación Completa**
```python
import json
import boto3
import logging
import math
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from typing import Dict, Any, List

# Configuración
sqs_client = boto3.client('sqs', region_name='us-east-1')
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# URL de la cola de respuestas
RESULT_QUEUE_URL = 'https://sqs.us-east-1.amazonaws.com/123456789/evaluation-result-queue'

def lambda_handler(event: Dict[str, Any], context) -> Dict[str, Any]:
    """
    Procesa solicitudes de cálculo de capacidad de pago
    """
    try:
        for record in event['Records']:
            message_body = json.loads(record['body'])
            
            # Extraer datos de la solicitud
            request_data = {
                'application_id': message_body['applicationId'],
                'document_id': message_body['documentId'],
                'email': message_body['email'],
                'requested_amount': float(message_body['requestedAmount']),
                'term_months': int(message_body['termMonths']),
                'annual_interest_rate': float(message_body['annualInterestRate']),
                'base_salary': float(message_body['baseSalary'])
            }
            
            logger.info(f"Processing capacity calculation for application: {request_data['application_id']}")
            
            # Realizar cálculo de capacidad
            calculation_result = calculate_loan_capacity(request_data)
            
            # Enviar resultado a cola de respuestas
            send_result_to_queue(calculation_result)
            
            logger.info(f"Capacity calculation completed for: {request_data['application_id']}")
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Capacity calculations processed successfully',
                'processed_count': len(event['Records'])
            })
        }
        
    except Exception as e:
        logger.error(f"Error in capacity calculation: {str(e)}")
        raise e

def calculate_loan_capacity(data: Dict[str, Any]) -> Dict[str, Any]:
    """
    Algoritmo principal de cálculo de capacidad de pago
    """
    try:
        # Parámetros de entrada
        salary = data['base_salary']
        requested_amount = data['requested_amount']
        term_months = data['term_months']
        annual_rate = data['annual_interest_rate']
        
        # Constantes del negocio
        MAX_DEBT_RATIO = 0.30  # Máximo 30% del salario para deudas
        MIN_SALARY = 1000000   # Salario mínimo requerido
        MAX_TERM = 60          # Máximo 60 meses
        MIN_AMOUNT = 500000    # Monto mínimo de préstamo
        
        logger.info(f"Calculating capacity for: Salary={salary}, Amount={requested_amount}, Term={term_months}")
        
        # Validaciones básicas
        if salary < MIN_SALARY:
            return create_rejection_result(data, "Salario insuficiente para otorgar préstamo")
        
        if term_months > MAX_TERM:
            return create_rejection_result(data, "Plazo solicitado excede el máximo permitido")
        
        if requested_amount < MIN_AMOUNT:
            return create_rejection_result(data, "Monto solicitado es menor al mínimo requerido")
        
        # Cálculo de capacidad máxima mensual
        monthly_capacity = salary * MAX_DEBT_RATIO
        
        # Cálculo de cuota mensual para el monto solicitado
        monthly_rate = annual_rate / 100 / 12
        
        if monthly_rate == 0:  # Préstamo sin interés
            monthly_payment_requested = requested_amount / term_months
        else:
            # Fórmula de cuota fija con interés compuesto
            factor = (1 + monthly_rate) ** term_months
            monthly_payment_requested = (requested_amount * monthly_rate * factor) / (factor - 1)
        
        logger.info(f"Monthly capacity: {monthly_capacity}, Required payment: {monthly_payment_requested}")
        
        # Determinar decisión y monto aprobado
        if monthly_payment_requested <= monthly_capacity:
            # APROBACIÓN COMPLETA
            decision = "APROBADA"
            approved_amount = requested_amount
            monthly_payment = monthly_payment_requested
            motivo = "Capacidad de pago suficiente para el monto solicitado"
            
        else:
            # APROBACIÓN PARCIAL - Calcular monto máximo
            if monthly_rate == 0:
                max_amount = monthly_capacity * term_months
            else:
                factor = (1 + monthly_rate) ** term_months
                max_amount = (monthly_capacity * (factor - 1)) / (monthly_rate * factor)
            
            if max_amount >= MIN_AMOUNT:
                decision = "PARCIAL"
                approved_amount = math.floor(max_amount / 100000) * 100000  # Redondear a múltiplos de 100k
                
                # Recalcular cuota para el monto aprobado
                if monthly_rate == 0:
                    monthly_payment = approved_amount / term_months
                else:
                    factor = (1 + monthly_rate) ** term_months
                    monthly_payment = (approved_amount * monthly_rate * factor) / (factor - 1)
                
                motivo = f"Monto ajustado según capacidad de pago. Máximo aprobable: ${approved_amount:,.0f}"
            else:
                return create_rejection_result(data, "Capacidad de pago insuficiente para el monto mínimo")
        
        # Generar plan de pagos
        payment_plan = generate_payment_schedule(
            approved_amount, 
            term_months, 
            annual_rate, 
            monthly_payment
        )
        
        # Crear resultado exitoso
        result = {
            'applicationId': data['application_id'],
            'documentId': data['document_id'],
            'decision': decision,
            'motivo': motivo,
            'capacidadDisponible': monthly_capacity,
            'cuotaCalculada': monthly_payment,
            'montoAprobado': approved_amount,
            'tasaInteresAnual': annual_rate,
            'plazoMeses': term_months,
            'cuotaMensual': monthly_payment,
            'planPagos': payment_plan,
            'timestamp': datetime.now().isoformat()
        }
        
        logger.info(f"Calculation result: {decision}, Amount: {approved_amount}, Payment: {monthly_payment}")
        return result
        
    except Exception as e:
        logger.error(f"Error in calculation algorithm: {str(e)}")
        return create_rejection_result(data, f"Error en el cálculo: {str(e)}")

def generate_payment_schedule(amount: float, term_months: int, annual_rate: float, monthly_payment: float) -> List[Dict[str, Any]]:
    """
    Genera el cronograma de pagos detallado
    """
    schedule = []
    remaining_balance = amount
    monthly_rate = annual_rate / 100 / 12
    current_date = datetime.now()
    
    for month in range(1, term_months + 1):
        # Calcular interés del mes
        interest_payment = remaining_balance * monthly_rate if monthly_rate > 0 else 0
        
        # Calcular capital del mes
        if month == term_months:
            # Última cuota: ajustar para saldar completamente
            principal_payment = remaining_balance
            current_monthly_payment = principal_payment + interest_payment
        else:
            principal_payment = monthly_payment - interest_payment
            current_monthly_payment = monthly_payment
        
        # Actualizar saldo
        remaining_balance -= principal_payment
        
        # Fecha de vencimiento
        due_date = current_date + timedelta(days=30 * month)
        
        # Agregar cuota al cronograma
        payment_detail = {
            'numeroCuota': month,
            'fechaVencimiento': due_date.strftime('%Y-%m-%d'),
            'valorCuota': round(current_monthly_payment, 2),
            'capital': round(principal_payment, 2),
            'interes': round(interest_payment, 2),
            'saldoPendiente': round(max(0, remaining_balance), 2)
        }
        
        schedule.append(payment_detail)
    
    return schedule

def create_rejection_result(data: Dict[str, Any], reason: str) -> Dict[str, Any]:
    """
    Crea un resultado de rechazo
    """
    return {
        'applicationId': data['application_id'],
        'documentId': data['document_id'],
        'decision': 'RECHAZADA',
        'motivo': reason,
        'capacidadDisponible': 0,
        'cuotaCalculada': 0,
        'montoAprobado': 0,
        'tasaInteresAnual': data['annual_interest_rate'],
        'plazoMeses': data['term_months'],
        'cuotaMensual': 0,
        'planPagos': [],
        'timestamp': datetime.now().isoformat()
    }

def send_result_to_queue(result: Dict[str, Any]) -> None:
    """
    Envía el resultado del cálculo a la cola de respuestas
    """
    try:
        message_body = json.dumps(result, default=str)
        
        response = sqs_client.send_message(
            QueueUrl=RESULT_QUEUE_URL,
            MessageBody=message_body,
            MessageAttributes={
                'ApplicationId': {
                    'StringValue': result['applicationId'],
                    'DataType': 'String'
                },
                'Decision': {
                    'StringValue': result['decision'],
                    'DataType': 'String'
                }
            }
        )
        
        logger.info(f"Result sent to queue. MessageId: {response['MessageId']}")
        
    except Exception as e:
        logger.error(f"Failed to send result to queue: {str(e)}")
        raise e
```

### 🔧 Configuración de las Lambdas

#### **Variables de Entorno**
```bash
# Lambda de Notificaciones
SES_REGION=us-east-1
FROM_EMAIL=noreply@crediya.com
LOG_LEVEL=INFO

# Lambda de Cálculo
RESULT_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/evaluation-result-queue
MAX_DEBT_RATIO=0.30
MIN_SALARY=1000000
LOG_LEVEL=INFO
```

#### **Permisos IAM Requeridos**
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ses:SendEmail",
                "ses:SendRawEmail"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "sqs:ReceiveMessage",
                "sqs:DeleteMessage",
                "sqs:SendMessage",
                "sqs:GetQueueAttributes"
            ],
            "Resource": [
                "arn:aws:sqs:*:*:manual-decision-queue",
                "arn:aws:sqs:*:*:automatic-evaluation-queue",
                "arn:aws:sqs:*:*:evaluation-result-queue"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "arn:aws:logs:*:*:*"
        }
    ]
}
```

### 📊 Monitoreo de Lambdas

#### **Métricas Clave**
- **Duración**: Tiempo de ejecución promedio
- **Errores**: Tasa de fallos por función
- **Throttles**: Limitaciones de concurrencia
- **Dead Letter Queue**: Mensajes fallidos

#### **Logs Importantes**
```python
# Lambda de Notificaciones
logger.info(f"Processing notification for application: {application_id}")
logger.info(f"Email sent successfully. MessageId: {message_id}")
logger.error(f"Failed to send email to {email}: {error}")

# Lambda de Cálculo
logger.info(f"Calculating capacity for: Salary={salary}, Amount={amount}")
logger.info(f"Calculation result: {decision}, Amount: {approved_amount}")
logger.error(f"Error in calculation algorithm: {error}")
```

---

## 🧪 Pruebas y Validación

### Casos de Prueba HU 6

#### Caso 1: Notificación de Aprobación
```bash
curl -X PUT http://localhost:8081/api/v1/applications/123/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "Aprobada",
    "comments": "Solicitud aprobada tras revisión completa"
  }'
```

**Resultado Esperado:**
- Status 200 OK
- Email enviado al cliente
- Estado actualizado en BD

#### Caso 2: Transición Inválida
```bash
curl -X PUT http://localhost:8081/api/v1/applications/123/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "Aprobada",
    "comments": "Intento de transición inválida"
  }'
```

**Resultado Esperado:**
- Status 400 Bad Request
- Error: "Invalid state transition"

### Casos de Prueba HU 7

#### Caso 1: Cálculo Exitoso
```bash
curl -X POST http://localhost:8081/api/v1/calculate-capacity \
  -H "Content-Type: application/json" \
  -d '{
    "documentoIdentidad": "11223344",
    "email": "cliente@crediya.com",
    "monto": 5000000,
    "plazoMeses": 12,
    "tasaInteresAnual": 12.0,
    "salarioBase": 2000000
  }'
```

**Resultado Esperado:**
- Status 200 OK
- Respuesta con decisión y plan de pagos

#### Caso 2: Usuario No Autorizado
```bash
curl -X POST http://localhost:8081/api/v1/calculate-capacity \
  -H "Content-Type: application/json" \
  -d '{
    "documentoIdentidad": "11223344",
    "email": "otro@email.com",
    "monto": 5000000,
    "plazoMeses": 12,
    "tasaInteresAnual": 12.0,
    "salarioBase": 2000000
  }'
```

**Resultado Esperado:**
- Status 404 Not Found
- Error: "Access denied: You can only calculate capacity for yourself"

---

## 📊 Monitoreo y Logs

### Logs Importantes a Monitorear

#### HU 6 - Notificaciones
```
INFO: Manual status update initiated for application: {applicationId}
INFO: State transition validated: {oldState} -> {newState}
INFO: Notification sent to SQS with messageId: {messageId}
ERROR: Invalid state transition attempted: {details}
```

#### HU 7 - Cálculo de Capacidad
```
INFO: Starting capacity calculation for document: {documentId}
INFO: Client validation successful for documentId: {documentId}
INFO: Capacity calculation request sent to SQS with messageId: {messageId}
INFO: Capacity calculation result received for messageId: {messageId}
ERROR: OWNERSHIP DENIED: User attempted to use incorrect email
```

### Métricas Clave
- **Tiempo de respuesta** de cálculos de capacidad
- **Tasa de éxito** de notificaciones por email
- **Errores de validación** de usuarios
- **Timeouts** en cálculos de capacidad

---

## 🔧 Troubleshooting

### Problemas Comunes

#### 1. **Timeout en Cálculo de Capacidad**
**Síntoma:** Status 408 Request Timeout
**Causa:** Lambda tarda más de 25 segundos
**Solución:** Verificar logs de Lambda y optimizar algoritmo

#### 2. **Email No Coincide**
**Síntoma:** "Access denied: You can only calculate capacity for yourself"
**Causa:** Email del request no coincide con usuario registrado
**Solución:** Verificar datos en authentication-service

#### 3. **Transición de Estado Inválida**
**Síntoma:** "Invalid state transition"
**Causa:** Intento de cambio de estado no permitido
**Solución:** Revisar matriz de transiciones válidas

#### 4. **Usuario No Encontrado**
**Síntoma:** "Client not found with documentId"
**Causa:** Usuario no existe en authentication-service
**Solución:** Verificar registro del usuario

---

## 📈 Futuras Mejoras

### Optimizaciones Propuestas
1. **Cache distribuido** para resultados de cálculo
2. **Retry automático** para fallos de Lambda
3. **Notificaciones push** además de email
4. **Dashboard** de monitoreo en tiempo real
5. **Algoritmos de ML** para cálculos más precisos

### Escalabilidad
- **Auto-scaling** de Lambdas según demanda
- **Particionamiento** de colas SQS por región
- **CDN** para recursos estáticos de emails
- **Base de datos** de solo lectura para consultas

---

*Documentación generada para el sistema de Solicitudes de Préstamo - Crediya*
*Versión: 1.0 | Fecha: Septiembre 2025*
