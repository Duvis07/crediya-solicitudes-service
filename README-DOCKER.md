# 🐳 Solicitudes Service - Docker/Podman

## 📋 Resumen General

Microservicio de solicitudes de crédito completamente dockerizado que se comunica con `authentication-service` y utiliza PostgreSQL.

## 🏗️ Arquitectura

```
PostgreSQL Container ◄─── Solicitudes Service Container ◄─── HTTP Calls to Authentication Service
    (Port 5432)              (Port 8081)                        (Port 8080)
```

### 🌐 Comunicación entre Microservicios

**Red Docker Compartida (`crediya-network`):**
- Ambos microservicios (authentication-service y solicitudes-service) deben usar la **misma red Docker**
- Esto permite que se comuniquen usando nombres de contenedor en lugar de IPs
- La red se crea automáticamente cuando se ejecuta `docker-compose up`

**Flujo de Comunicación:**
1. **Solicitudes Service** recibe petición HTTP en puerto 8081
2. Valida el JWT token llamando a **Authentication Service** en puerto 8080
3. Si el token es válido, procesa la solicitud de crédito
4. Consulta/actualiza datos en **PostgreSQL** en puerto 5432

**Configuración de Red:**
- **Authentication Service:** `http://crediya-authentication-service:8080`
- **Solicitudes Service:** `http://crediya-solicitudes-service:8081`
- **PostgreSQL:** `crediya-postgres-solicitudes:5432`

**¿Por qué la misma red?**
- Sin red compartida: Los contenedores no pueden comunicarse entre sí
- Con red compartida: Resolución de nombres automática entre servicios
- Seguridad: Aislamiento del tráfico de otros contenedores del sistema

## 📁 Archivos Docker

### `deployment/Dockerfile`
**Propósito:** Crea la imagen del microservicio usando multi-stage build para optimizar el tamaño final.
**Contiene:** Instrucciones para construir imagen con OpenJDK 17, copiar JAR compilado, exponer puerto 8081 y definir comando de inicio.

### `docker-compose.yml`
**Propósito:** Orquesta todos los servicios necesarios (PostgreSQL + Solicitudes Service) en contenedores.
**Contiene:** Definición de servicios, puertos, volúmenes, variables de entorno, dependencias y red compartida.

### `deployment/init-db.sql`
**Propósito:** Inicializa automáticamente la base de datos PostgreSQL cuando se crea el contenedor.
**Contiene:** Scripts SQL para crear tablas (states, loan_types, applications) e insertar datos iniciales de prueba.

### `.env`
**Propósito:** Define variables de entorno para ejecución local del microservicio (sin Docker).
**Contiene:** Configuración de base de datos local, URLs del authentication-service, secretos JWT y perfil de Spring.

## 📜 Scripts de Automatización

### `scripts/build.bat`
**Propósito:** Compila la aplicación Spring Boot y prepara el JAR para Docker.
**Contiene:** Comandos para limpiar, compilar con Gradle y copiar JAR al directorio deployment.

### `scripts/clean-restart.bat`
**Propósito:** Reinicio completo del entorno Docker eliminando todo rastro anterior.
**Contiene:** Comandos para parar servicios, eliminar imágenes/volúmenes, reconstruir desde cero e iniciar servicios.

### `scripts/deploy.bat`
**Propósito:** Script de despliegue rápido sin limpieza completa.
**Contiene:** Comandos para construir y desplegar servicios manteniendo datos existentes.

## 🚀 Ejecución con Podman

### Comando Principal
```bash
scripts\clean-restart.bat
```

**Este script:**
- Para servicios existentes y limpia volúmenes
- Elimina imágenes anteriores del proyecto
- Reconstruye desde cero con `gradlew bootJar`
- Inicia PostgreSQL + Solicitudes Service
- Verifica estado final

### Comandos Alternativos
```bash
# Solo iniciar
podman-compose up -d

# Reconstruir
podman-compose build --no-cache

# Ver estado
podman-compose ps
```

## 🔗 Comunicación entre Microservicios

### Implementación
- **AuthServiceClient.java**: Cliente HTTP reactivo con WebClient
- **URL dinámica**: `http://authentication-service:8080/api/v1/usuarios/{documentId}`
- **Resilience4j**: Retry, Circuit Breaker, Timeout para fallos
- **Red Docker**: Comunicación por nombres de contenedor

### Flujo
1. Frontend → Solicitudes Service (JWT en header)
2. Solicitudes Service → Authentication Service (validar usuario)
3. Respuesta con datos de usuario o fallback si falla

## 📊 Verificación

```bash
# Health check
curl http://localhost:8081/actuator/health

# Ver logs
podman-compose logs -f solicitudes-service

# Estado de contenedores
podman-compose ps
```

## 🔧 Solución Rápida de Problemas

- **Error de conexión**: `scripts\clean-restart.bat`
- **Base de datos**: `podman-compose logs postgres-solicitudes`
- **Imagen no encontrada**: Verificar que `scripts\build.bat` completó exitosamente

---

**Resultado**: Microservicio dockerizado funcionando en puerto 8081 con comunicación completa al authentication-service.
