# AuthServiceClientTest - Documentación de Testing

## 📋 Descripción General

Esta clase de test valida la funcionalidad del `WebClient` utilizado para comunicación con el servicio de autenticación, simulando diferentes escenarios de respuesta HTTP.

## 🧪 Estrategia de Testing

### Enfoque Adoptado
- **Tests unitarios con Mockito** para simular dependencias externas
- **Reactor Test (StepVerifier)** para validar flujos reactivos
- **Simulación directa de WebClient** sin dependencias de Resilience4j

### ¿Por qué este enfoque?
- Evita complejidad de mocking de operadores Resilience4j (CircuitBreaker, Retry, TimeLimiter)
- Se enfoca en validar la lógica core de comunicación HTTP
- Mantiene tests simples y mantenibles

## 🎭 Mocks Utilizados

### 1. **@Mock private WebClient webClient**
```java
@Mock
private WebClient webClient;
```
**Propósito:** 
- Es el **punto de entrada principal** para todas las llamadas HTTP
- Simula el cliente HTTP reactivo de Spring WebFlux
- **Sin este mock:** Los tests harían llamadas HTTP reales al servicio de autenticación

**Funcionalidad:**
- Intercepta llamadas como `webClient.get()`, `webClient.post()`, etc.
- Permite controlar qué retorna sin ejecutar lógica real de red

### 2. **@Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec**
```java
@Mock
private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
```
**Propósito:**
- Simula el **segundo paso** en la cadena fluida de WebClient
- Se obtiene después de `webClient.get()`
- Permite configurar la URI del endpoint

**Funcionalidad:**
- Intercepta llamadas como `.uri("http://localhost:8080/api/v1/users/{id}", documentId)`
- Controla el paso de configuración de URL sin validar URLs reales

### 3. **@Mock private WebClient.RequestHeadersSpec requestHeadersSpec**
```java
@Mock
private WebClient.RequestHeadersSpec requestHeadersSpec;
```
**Propósito:**
- Simula el **tercer paso** en la cadena fluida
- Se obtiene después de configurar la URI
- Permite agregar headers HTTP si fuera necesario

**Funcionalidad:**
- Intercepta llamadas como `.header("Authorization", "Bearer token")`
- Controla el paso de configuración de headers
- En nuestros tests, principalmente pasa al siguiente paso con `.retrieve()`

### 4. **@Mock private WebClient.ResponseSpec responseSpec** ⭐ **MÁS IMPORTANTE**
```java
@Mock
private WebClient.ResponseSpec responseSpec;
```
**Propósito:**
- Simula el **paso final** antes de obtener la respuesta
- Se obtiene después de `.retrieve()`
- **Es el más crítico** porque aquí definimos qué respuesta simular

**Funcionalidad:**
- Intercepta `.bodyToMono(UserResponse.class)`
- **Aquí controlamos el resultado:** éxito, error 404, error 500, timeout, etc.
- Permite simular diferentes escenarios sin servicios externos

## 🔗 **Cadena Completa de Mocking**

```java
// Flujo real que simulamos:
webClient.get()                    // → RequestHeadersUriSpec (Mock 2)
    .uri("/users/{id}", "12345")   // → RequestHeadersSpec (Mock 3)  
    .retrieve()                    // → ResponseSpec (Mock 4)
    .bodyToMono(UserResponse.class) // → Mono<UserResponse> (controlado por Mock 4)
```

## 🎯 **¿Por qué necesitamos todos estos mocks?**

**Sin mocks:** 
- ❌ Llamadas HTTP reales
- ❌ Dependencia del servicio de autenticación
- ❌ Tests lentos e inestables
- ❌ Imposible simular errores específicos

**Con mocks:**
- ✅ Tests rápidos y determinísticos  
- ✅ Control total sobre respuestas
- ✅ Simulación de todos los escenarios (éxito, errores, timeouts)
- ✅ Tests independientes de servicios externos

### 2. Configuración de Mocks
```java
when(webClient.get()).thenReturn(requestHeadersUriSpec);
when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
when(responseSpec.bodyToMono(UserResponse.class)).thenReturn(Mono.just(userResponse));
```

**Flujo simulado:**
1. `webClient.get()` → Retorna `RequestHeadersUriSpec`
2. `.uri()` → Retorna `RequestHeadersSpec`  
3. `.retrieve()` → Retorna `ResponseSpec`
4. `.bodyToMono()` → Retorna `Mono<UserResponse>` con datos simulados

## 🧪 Casos de Test Implementados

### 1. **shouldReturnTrueWhenClientExists**
- **Escenario:** Respuesta exitosa (200 OK)
- **Mock:** `Mono.just(userResponse)`
- **Validación:** Respuesta exitosa con UserResponse

### 2. **shouldThrowClientNotFoundExceptionWhenClientNotFound**
- **Escenario:** Cliente no encontrado (404 Not Found)
- **Mock:** `Mono.error(WebClientResponseException.create(404, ...))`
- **Validación:** Error WebClientResponseException

### 3. **shouldThrowServiceUnavailableExceptionWhenServiceUnavailable**
- **Escenario:** Servicio no disponible (503 Service Unavailable)
- **Mock:** `Mono.error(WebClientResponseException.create(503, ...))`
- **Validación:** Error WebClientResponseException

### 4. **shouldThrowServiceUnavailableExceptionWhenInternalServerError**
- **Escenario:** Error interno del servidor (500 Internal Server Error)
- **Mock:** `Mono.error(WebClientResponseException.create(500, ...))`
- **Validación:** Error WebClientResponseException

### 5. **shouldThrowServiceUnavailableExceptionWhenGenericException**
- **Escenario:** Excepción genérica de conectividad
- **Mock:** `Mono.error(new RuntimeException("Connection timeout"))`
- **Validación:** Error RuntimeException

### 6. **shouldHandleTimeoutException**
- **Escenario:** Timeout de conexión
- **Mock:** `Mono.error(new TimeoutException("Request timeout"))`
- **Validación:** Error TimeoutException

## 🔧 Herramientas de Testing

### StepVerifier
```java
StepVerifier.create(result)
    .expectNext(userResponse)
    .verifyComplete();

StepVerifier.create(result)
    .expectError(WebClientResponseException.class)
    .verify();
```

**Funciones:**
- `.expectNext()` - Valida el siguiente elemento emitido
- `.expectError()` - Valida que se emita un error específico
- `.verifyComplete()` - Valida que el flujo se complete exitosamente
- `.verify()` - Ejecuta la verificación

### Mockito Annotations
- `@ExtendWith(MockitoExtension.class)` - Habilita Mockito en JUnit 5
- `@Mock` - Crea mocks automáticamente
- `when().thenReturn()` - Define comportamiento de mocks

## 🎯 Beneficios del Enfoque

### ✅ Ventajas
- **Simplicidad:** Tests fáciles de entender y mantener
- **Rapidez:** Ejecución rápida sin dependencias externas
- **Aislamiento:** Cada test valida un escenario específico
- **Cobertura:** Cubre todos los casos de error y éxito

### ⚠️ Limitaciones
- No valida la integración completa con Resilience4j
- No testea la lógica de retry/circuit breaker directamente
- Se enfoca en la capa de comunicación HTTP únicamente

## 🚀 Ejecución de Tests

```bash
# Ejecutar todos los tests del módulo
./gradlew :webclient-auth:test

# Ejecutar solo esta clase de test
./gradlew test --tests AuthServiceClientTest
```

## 📝 Consideraciones de Mantenimiento

1. **Actualizar mocks** si cambia la API de WebClient
2. **Agregar nuevos casos** si se identifican escenarios adicionales
3. **Revisar assertions** si cambian los tipos de excepción esperados
4. **Mantener consistencia** en la estructura de los tests
