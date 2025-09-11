# Lambda Capacidad de Endeudamiento - HU7

## 📋 Descripción
Lambda en Python para calcular automáticamente la capacidad de endeudamiento de solicitantes según las reglas de negocio de CrediYa.

## 🚀 Funcionalidades

### Cálculos Implementados
- **Capacidad Máxima**: 35% del salario base
- **Deuda Actual**: Suma de cuotas de préstamos aprobados activos
- **Capacidad Disponible**: Capacidad máxima - deuda actual
- **Cuota Nueva**: Fórmula de amortización estándar

### Decisiones Automáticas
- **APROBADO**: Cuota ≤ capacidad disponible y monto ≤ 5 salarios
- **REVISION_MANUAL**: Cuota ≤ capacidad disponible pero monto > 5 salarios
- **RECHAZADO**: Cuota > capacidad disponible

### Plan de Pagos
- Tabla de amortización completa
- Separación de capital e intereses
- Saldo pendiente por mes

## 🔧 Configuración LocalStack

### Variables de Entorno
```bash
AWS_DEFAULT_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
```

### Endpoints LocalStack
- **DynamoDB**: http://localhost:4566
- **SNS**: http://localhost:4566

## 📊 Estructura de Datos

### Entrada (Event Body)
```json
{
  "documento_identidad": "12345678",
  "monto": 1000000,
  "plazo_meses": 12,
  "tasa_interes_anual": 12.0,
  "salario_base": 3000000,
  "solicitud_id": "SOL-001"
}
```

### Respuesta
```json
{
  "solicitud_id": "SOL-001",
  "decision": "APROBADO",
  "capacidad_disponible": 1050000.00,
  "cuota_calculada": 88849.22,
  "motivo": "Cuota mensual (88849.22) dentro de capacidad disponible (1050000.00)"
}
```

## 🧪 Testing

### Ejecutar Tests Locales
```bash
cd lambda-capacidad-endeudamiento
python test_lambda.py
```

### Tests Incluidos
- Cálculo de cuota mensual
- Decisiones de aprobación/rechazo
- Generación de plan de pagos
- Evento completo de lambda

## 📦 Dependencias
- boto3==1.34.162
- botocore==1.34.162

## 🔄 Integración con LocalStack

### Tablas DynamoDB Requeridas
- `prestamos-activos`: Préstamos aprobados por documento
- `calculos-capacidad`: Resultados de cálculos

### Topics SNS
- `notificaciones-prestamos`: Para envío de plan de pagos

## 💡 Fórmulas Utilizadas

### Cuota Mensual (Amortización)
```
Cuota = P * (i * (1+i)^n) / ((1+i)^n - 1)
Donde:
- P = Monto del préstamo
- i = Tasa de interés mensual
- n = Plazo en meses
```

### Capacidad de Endeudamiento
```
Capacidad Máxima = Salario Base * 0.35
Capacidad Disponible = Capacidad Máxima - Deuda Actual
```
