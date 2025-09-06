# 🧪 **GUÍA DE PRUEBAS POSTMAN - HU6**

## 🚀 **CONFIGURACIÓN INICIAL**

### **1. Variables de Entorno**
Crear una nueva colección en Postman y configurar estas variables:

```
BASE_URL: http://localhost:8080
JWT_TOKEN: [Token obtenido del servicio de autenticación]
```

### **2. Headers Globales**
```
Content-Type: application/json
Authorization: Bearer {{JWT_TOKEN}}
```

## 🔐 **PASO 1: OBTENER TOKEN DE AUTENTICACIÓN**

### **Request: Login Asesor**
```
POST {{BASE_URL}}/api/v1/auth/login
```

**Body:**
```json
{
  "username": "asesor@crediya.com",
  "password": "password123"
}
```

**Response esperado:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "ASESOR",
  "expiresIn": 3600
}
```

⚡ **Copiar el token y guardarlo en la variable JWT_TOKEN**

## 📋 **PASO 2: LISTAR SOLICITUDES EN REVISIÓN**

### **Request: Ver solicitudes pendientes**
```
GET {{BASE_URL}}/api/v1/solicitud?page=0&size=10
```

**Headers:**
```
Authorization: Bearer {{JWT_TOKEN}}
```

**Response esperado:**
```json
{
  "content": [
    {
      "applicationId": 123,
      "documentId": "12345678",
      "amount": 50000.00,
      "term": 12,
      "email": "cliente@email.com",
      "stateName": "En revisión manual",
      "loanTypeName": "Personal"
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

📝 **Anotar el applicationId de una solicitud en "En revisión manual"**

## ✅ **PASO 3: APROBAR SOLICITUD**

### **Request: Aprobar solicitud**
```
PUT {{BASE_URL}}/api/v1/solicitud/123
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {{JWT_TOKEN}}
```

**Body:**
```json
{
  "estado": "Aprobada",
  "comentarios": "Cliente cumple con todos los requisitos crediticios. Capacidad de pago verificada."
}
```

**Response esperado:**
```json
{
  "application_id": 123,
  "estado_anterior": "En revisión manual",
  "estado_nuevo": "Aprobada",
  "mensaje": "Estado actualizado exitosamente",
  "fecha_actualizacion": "2025-01-05T08:52:26",
  "comentarios": "Cliente cumple con todos los requisitos crediticios. Capacidad de pago verificada."
}
```

**Logs esperados en consola:**
```
📧 ========== SIMULATED EMAIL NOTIFICATION ==========
📧 To: cliente@email.com
📧 Subject: Decisión sobre su solicitud de crédito #123
📧 Status: Aprobada
📧 Amount: $50,000.00
📧 Content: ¡Felicitaciones! Su solicitud ha sido aprobada...

🔔 ========== SIMULATED SQS MESSAGE ==========
🔔 Queue: crediya-loan-notifications
🔔 Message Type: LOAN_STATUS_UPDATE
```

## ❌ **PASO 4: RECHAZAR SOLICITUD**

### **Request: Rechazar solicitud**
```
PUT {{BASE_URL}}/api/v1/solicitud/124
```

**Body:**
```json
{
  "estado": "Rechazada",
  "comentarios": "Ingresos insuficientes para el monto solicitado. Ratio deuda/ingreso excede límites permitidos."
}
```

**Response esperado:**
```json
{
  "application_id": 124,
  "estado_anterior": "En revisión manual",
  "estado_nuevo": "Rechazada",
  "mensaje": "Estado actualizado exitosamente",
  "fecha_actualizacion": "2025-01-05T08:52:26",
  "comentarios": "Ingresos insuficientes para el monto solicitado..."
}
```

## 🚫 **CASOS DE ERROR - VALIDACIONES**

### **Error 1: ID Inválido**
```
PUT {{BASE_URL}}/api/v1/solicitud/abc
```

**Response:**
```json
{
  "error": "ID de solicitud inválido",
  "status": 400
}
```

### **Error 2: Solicitud No Encontrada**
```
PUT {{BASE_URL}}/api/v1/solicitud/99999
```

**Response:**
```json
{
  "error": "Solicitud no encontrada con ID: 99999",
  "status": 404
}
```

### **Error 3: Estado Inválido**
```json
{
  "estado": "Pendiente",
  "comentarios": "Test"
}
```

**Response:**
```json
{
  "error": "Estado no válido: Pendiente. Solo se permite 'Aprobada' o 'Rechazada'",
  "status": 400
}
```

### **Error 4: Estado Vacío**
```json
{
  "comentarios": "Solo comentarios"
}
```

**Response:**
```json
{
  "error": "El estado es obligatorio",
  "status": 400
}
```

### **Error 5: Transición No Permitida**
```
PUT {{BASE_URL}}/api/v1/solicitud/125  // Solicitud ya aprobada
```

**Response:**
```json
{
  "error": "La solicitud no puede ser modificada en su estado actual. Solo se pueden aprobar/rechazar solicitudes en revisión manual.",
  "status": 400
}
```

### **Error 6: Sin Autorización**
```
PUT {{BASE_URL}}/api/v1/solicitud/123
// Sin header Authorization
```

**Response:**
```json
{
  "error": "No autorizado - Se requiere autenticación",
  "status": 401
}
```

### **Error 7: Rol Incorrecto**
```
// Con token de CLIENTE en lugar de ASESOR
```

**Response:**
```json
{
  "error": "Prohibido - Solo asesores pueden actualizar solicitudes",
  "status": 403
}
```

## 📊 **COLECCIÓN POSTMAN COMPLETA**

### **Estructura de Carpetas:**
```
📁 HU6 - Aprobación/Rechazo
├── 🔐 Autenticación
│   └── Login Asesor
├── 📋 Consultas
│   └── Listar Solicitudes
├── ✅ Casos Exitosos
│   ├── Aprobar Solicitud
│   └── Rechazar Solicitud
└── 🚫 Casos de Error
    ├── ID Inválido
    ├── Solicitud No Encontrada
    ├── Estado Inválido
    ├── Estado Vacío
    ├── Transición No Permitida
    ├── Sin Autorización
    └── Rol Incorrecto
```

## 🎯 **FLUJO DE PRUEBAS RECOMENDADO**

1. **🔐 Autenticarse** como asesor
2. **📋 Listar solicitudes** para encontrar IDs válidos
3. **✅ Aprobar una solicitud** y verificar logs
4. **❌ Rechazar otra solicitud** y verificar logs
5. **🚫 Probar casos de error** para validar validaciones
6. **📋 Verificar cambios** listando solicitudes nuevamente

## 🔍 **VERIFICACIONES IMPORTANTES**

- ✅ **Status Code 200** para casos exitosos
- ✅ **Logs de notificación** en consola del servidor
- ✅ **Estructura JSON** correcta en responses
- ✅ **Timestamps** actualizados
- ✅ **Validaciones** funcionando correctamente
- ✅ **Autorización** requerida para todos los endpoints

## 📝 **NOTAS ADICIONALES**

- **Swagger UI**: Disponible en `http://localhost:8080/swagger-ui.html`
- **Logs**: Monitorear consola del servidor para ver notificaciones simuladas
- **Base de Datos**: Verificar cambios de estado en BD si es necesario
- **Tokens**: Renovar JWT si expira durante las pruebas
