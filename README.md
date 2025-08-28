# CrediYa - Microservicio de Solicitudes

**Microservicio para gestión de solicitudes de préstamos implementando Clean Architecture con Spring Boot WebFlux**

## 📋 Descripción del Proyecto

CrediYa es una plataforma que digitaliza y optimiza la gestión de solicitudes de préstamos personales, eliminando procesos manuales y presenciales. Este microservicio maneja específicamente las **solicitudes de préstamos** (HU2), permitiendo a los clientes enviar solicitudes con información del préstamo deseado para evaluación automatizada.

### Funcionalidades Principales

- ✅ **Registro de Solicitudes**: Los clientes pueden enviar solicitudes de préstamo con documento, email, monto, plazo y tipo de préstamo
- ✅ **Validación Automática**: Validación de datos de entrada, montos (100k-50M), plazos (6-60 meses) y formatos
- ✅ **Gestión de Estados**: Las solicitudes inician con estado "Pendiente de revisión"
- ✅ **Tipos de Préstamo**: Soporte para PERSONAL, MORTGAGE, VEHICLE, MICROCREDIT, BUSINESS
- ✅ **API Reactiva**: Implementado con Spring Boot WebFlux para alta concurrencia

## 🏗️ Arquitectura

### Clean Architecture (Hexagonal)

![Clean Architecture](https://miro.medium.com/max/1400/1*ZdlHz8B0-qu9Y-QO3AXR_w.png)

```
solicitudes-service/
├── applications/app-service/          # 🚀 Aplicación principal
├── domain/
│   ├── model/                         # 🏛️ Entidades del dominio
│   └── usecase/                       # 📋 Casos de uso
└── infrastructure/
    ├── driven-adapters/
    │   └── r2dbc-postgresql/          # 🗄️ Persistencia reactiva
    └── entry-points/
        └── reactive-web/              # 🌐 API REST reactiva
```

### Stack Tecnológico

- **Framework**: Spring Boot 3.5.4 con WebFlux (Programación Reactiva)
- **Base de Datos**: PostgreSQL con R2DBC (Acceso reactivo)
- **Mapeo**: MapStruct para conversión de DTOs
- **Validación**: Bean Validation con validadores personalizados
- **Documentación**: OpenAPI 3 / Swagger
- **Testing**: JUnit 5, Mockito, WebTestClient
- **Calidad**: Jacoco (Coverage), PiTest (Mutation Testing), SonarLint

## 🚀 Inicio Rápido

### Prerrequisitos

- Java 17+
- PostgreSQL 12+
- Gradle 8+

### Configuración

1. **Clonar el repositorio**
```bash
git clone <repository-url>
cd solicitudes-service
```

2. **Configurar base de datos**
```sql
-- Crear base de datos
CREATE DATABASE crediya_solicitudes;

-- Configurar usuario (opcional)
CREATE USER crediya_user WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE crediya_solicitudes TO crediya_user;
```

3. **Configurar variables de entorno**
```bash
# .env (ejemplo)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crediya_solicitudes
DB_USER=crediya_user
DB_PASSWORD=password
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

4. **Ejecutar la aplicación**
```bash
# Compilar y ejecutar
./gradlew bootRun

# O ejecutar tests
./gradlew test

# Generar reporte de cobertura
./gradlew jacocoTestReport
```

## 📚 API Documentation

### Swagger UI
Una vez iniciada la aplicación, accede a la documentación interactiva:

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8081/v3/api-docs

### Endpoints Principales

#### POST /api/v1/solicitud
Crear una nueva solicitud de préstamo

**Request Body:**
```json
{
  "documentId": "12345678",
  "email": "cliente@example.com",
  "amount": 500000,
  "term": 24,
  "loanType": "PERSONAL"
}
```

**Response:**
```json
{
  "applicationId": 1,
  "documentId": "12345678",
  "email": "cliente@example.com",
  "amount": 500000,
  "term": 24,
  "stateId": 1,
  "loanTypeId": 1,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Validaciones

- **documentId**: Requerido, no vacío
- **email**: Formato de email válido
- **amount**: Entre 100,000 y 50,000,000
- **term**: Entre 6 y 60 meses
- **loanType**: Uno de: PERSONAL, MORTGAGE, VEHICLE, MICROCREDIT, BUSINESS

## 🧪 Testing

### Ejecutar Tests
```bash
# Todos los tests
./gradlew test

# Tests específicos
./gradlew :domain:usecase:test
./gradlew :infrastructure:entry-points:reactive-web:test

# Con reporte de cobertura
./gradlew test jacocoTestReport
```

### Cobertura de Código
Los reportes se generan en:
- `build/reports/jacoco/test/html/index.html`
- Cobertura objetivo: >80%

## 📁 Estructura del Proyecto

### Domain Layer
- **`domain/model/`**: Entidades del dominio (Application, LoanType, State)
- **`domain/usecase/`**: Lógica de negocio (ApplicationUseCase)

### Infrastructure Layer
- **`driven-adapters/r2dbc-postgresql/`**: Repositorios reactivos y mappers
- **`entry-points/reactive-web/`**: Controllers, DTOs y configuración web

### Application Layer
- **`applications/app-service/`**: Configuración principal y punto de entrada

## 🔧 Configuración

### application.yml
```yaml
server:
  port: 8081

spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/crediya_solicitudes
    username: ${DB_USER:crediya_user}
    password: ${DB_PASSWORD:password}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}

logging:
  level:
    co.com.crediya: DEBUG
```

## 🔒 Seguridad

### Headers de Seguridad
- Content-Security-Policy
- Strict-Transport-Security
- X-Content-Type-Options
- Cache-Control

### CORS
Configurado para permitir orígenes específicos en desarrollo y producción.

## 📊 Monitoreo y Logs

### Logs
- Nivel DEBUG para paquetes de la aplicación
- Trazabilidad de operaciones críticas
- Manejo centralizado de excepciones

### Métricas
- Actuator endpoints disponibles
- Métricas de rendimiento de WebFlux
- Monitoreo de base de datos R2DBC

## 🚀 Despliegue

### Docker
```dockerfile
FROM openjdk:17-jre-slim
COPY build/libs/solicitudes-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```
## 🤝 Contribución

### Estándares de Código
- SonarLint para validación
- Cobertura mínima: 80%
- Tests unitarios obligatorios
- Documentación de APIs con OpenAPI

### Git Flow
- Feature branches para nuevas funcionalidades

## 📋 Requerimientos de Negocio

### HU2 - Registrar Solicitud de Préstamo
**Como cliente, quiero enviar mi solicitud de préstamo con la información necesaria (monto y plazo deseado) para que CrediYa pueda evaluarla**

#### Criterios de Aceptación:
- ✅ Endpoint POST /api/v1/solicitud
- ✅ Validación de información del cliente y préstamo
- ✅ Transacciones reactivas con WebFlux
- ✅ Logs de trazabilidad
- ✅ Manejo de excepciones
- ✅ Estado inicial "Pendiente de revisión"
- ✅ Validación de tipos de préstamo existentes
