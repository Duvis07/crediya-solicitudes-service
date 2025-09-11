# 📋 HU4 - Listado de solicitudes para revisión manual

## 📖 Resumen de la Historia de Usuario

**Como Asesor** quiero ver un listado de todas las solicitudes que necesitan mi revisión (aquellas que están "Pendiente de revisión", "Rechazadas", "Revision manual") **para** tomar la decisión final.

## 🔧 Implementación Técnica

### Endpoint Principal
```
GET /api/v1/solicitud?page=0&size=10
```

### Parámetros de Consulta
- **`page`** (opcional): Número de página (comenzando desde 0). Default: 0
- **`size`** (opcional): Tamaño de página (máximo 100). Default: 10

### Autorización
- **Rol requerido**: `ASESOR`
- **Autenticación**: JWT Token requerido

## 📊 Estructura de Respuesta

### Respuesta Paginada
```json
{
  "content": [
    {
      "monto": 5000000.00,
      "plazo": 24,
      "estado_solicitud": "Pendiente de revisión",
      "documento_id": "12345678",
      "email": "cliente@email.com",
      "nombre": "Juan Pérez García",
      "salario_base": 3000000.00,
      "tipo_prestamo": "Crédito Personal",
      "tasa_interes": 15.50,
      "deuda_total_mensual_solicitudes_aprobadas": 800000.00
    }
  ],
  "totalElements": 25,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 10
}
```

## 📋 Campos Detallados

### 💰 Información Financiera de la Solicitud
- **`monto`** (`BigDecimal`): Cantidad de dinero solicitada en pesos colombianos
- **`plazo`** (`Integer`): Número de meses para pagar el préstamo
- **`tasa_interes`** (`BigDecimal`): Tasa de interés anual aplicable (ej: 15.50 = 15.5%)

### 👤 Información del Cliente
- **`documento_id`** (`String`): Número de documento de identidad del solicitante
- **`email`** (`String`): Correo electrónico del cliente
- **`nombre`** (`String`): Nombre completo del solicitante
- **`salario_base`** (`BigDecimal`): Salario mensual del cliente en pesos colombianos

### 📄 Información del Producto
- **`tipo_prestamo`** (`String`): Tipo de préstamo solicitado
  - Ejemplos: "Crédito Personal", "Crédito Hipotecario", "Crédito Vehicular"
- **`estado_solicitud`** (`String`): Estado actual de la solicitud
  - Valores posibles: "Pendiente de revisión", "Rechazada", "Revision manual"

### 💳 Cálculo Financiero Crítico
- **`deuda_total_mensual_solicitudes_aprobadas`** (`BigDecimal`): **Total de cuotas mensuales** que el cliente ya está pagando por otros préstamos **APROBADOS Y DESEMBOLSADOS** en CrediYa

## 🔍 Explicación Detallada: `deuda_total_mensual_solicitudes_aprobadas`

### ¿Qué es exactamente este campo?

Este campo representa la **suma de todas las cuotas mensuales** que el cliente ya está pagando por préstamos anteriores que CrediYa le ha otorgado y que están en estado "Desembolsado".

### ¿Cómo se calcula?

1. **Busca todas las solicitudes del cliente** (por `documento_id`)
2. **Filtra solo las que están en estado "Desembolsado"** (préstamos activos)
3. **Para cada préstamo desembolsado, calcula la cuota mensual** usando la fórmula:
   ```
   Cuota Mensual = [Monto × (Tasa/12) × (1 + Tasa/12)^Plazo] / [(1 + Tasa/12)^Plazo - 1]
   ```
4. **Suma todas las cuotas mensuales** de todos los préstamos activos

### Ejemplo Práctico

**Cliente: Juan Pérez (documento: 12345678)**

**Préstamos anteriores desembolsados:**
- Préstamo 1: $2,000,000 a 12 meses al 18% anual → Cuota mensual: $185,000
- Préstamo 2: $3,000,000 a 24 meses al 15% anual → Cuota mensual: $145,000

**Resultado:**
```json
"deuda_total_mensual_solicitudes_aprobadas": 330000.00
```

### ¿Para qué sirve al asesor?

1. **Evaluar capacidad de pago**: Si el cliente gana $2,000,000 y ya paga $330,000, le quedan $1,670,000 para gastos y nueva cuota
2. **Calcular nivel de endeudamiento**: 330,000 / 2,000,000 = 16.5% de endeudamiento actual
3. **Tomar decisión informada**: ¿Puede el cliente asumir una cuota adicional?

### Estados que NO se incluyen
- "Pendiente de revisión" - Aún no aprobado
- "Rechazada" - No genera deuda
- "Revision manual" - Aún no aprobado
- "Aprobada" - Aprobado pero no desembolsado aún

### Estados que SÍ se incluyen
- "Desembolsada" - Préstamo activo que genera cuota mensual

## 🎯 Casos de Uso para el Asesor

### Escenario 1: Cliente sin deudas previas
```json
"salario_base": 3000000.00,
"deuda_total_mensual_solicitudes_aprobadas": 0.00,
"monto": 5000000.00,
"plazo": 36
```
**Análisis**: Cliente limpio, evaluar solo la nueva cuota vs salario.

### Escenario 2: Cliente con deudas existentes
```json
"salario_base": 2500000.00,
"deuda_total_mensual_solicitudes_aprobadas": 800000.00,
"monto": 3000000.00,
"plazo": 24
```
**Análisis**: Cliente ya compromete 32% de su salario, evaluar si puede asumir cuota adicional.

### Escenario 3: Cliente sobreendeudado
```json
"salario_base": 1800000.00,
"deuda_total_mensual_solicitudes_aprobadas": 1200000.00,
"monto": 2000000.00,
"plazo": 12
```
**Análisis**: Cliente compromete 67% de su salario, probablemente rechazar.

## 🔄 Flujo de Procesamiento

1. **Filtrado automático**: Solo solicitudes en estados que requieren revisión manual
2. **Enriquecimiento de datos**: Combina información de múltiples fuentes:
   - Tabla `applications` (datos básicos de la solicitud)
   - Authentication Service (información del cliente)
   - Tabla `loan_types` (información del producto)
   - Tabla `states` (estado actual)
   - **Cálculo en tiempo real** de deudas mensuales existentes
3. **Paginación**: Control de resultados mostrados
4. **Respuesta estructurada**: JSON con todos los datos necesarios para la decisión

## ✅ Criterios de Aceptación Cumplidos

- ✅ **Endpoint GET /api/v1/solicitud** implementado
- ✅ **Solo rol Asesor** puede acceder
- ✅ **Arquitectura hexagonal** respetada
- ✅ **WebFlux reactivo** implementado
- ✅ **Lista paginada y filtrable** funcionando
- ✅ **Todos los campos requeridos** incluidos:
  - monto, plazo, email, nombre
  - tipo_prestamo, tasa_interes, estado_solicitud
  - salario_base, deuda_total_mensual_solicitudes_aprobadas
- ✅ **Logs de traza** implementados
- ✅ **Manejo de excepciones** completo
- ✅ **Tests unitarios** extensivos

## 🚨 Estados Filtrados Automáticamente

La HU4 solo muestra solicitudes en estos estados:
- **"Pendiente de revisión"** - Solicitudes nuevas que esperan primera revisión
- **"Rechazadas"** - Solicitudes rechazadas que podrían reconsiderarse
- **"Revision manual"** - Solicitudes que requieren análisis adicional del asesor

**Estados excluidos:**
- "Aprobada" - Ya tiene decisión
- "Desembolsada" - Ya está activa
- Cualquier otro estado personalizado

## 📈 Valor de Negocio

Esta funcionalidad permite a CrediYa:
1. **Agilizar decisiones** de aprobación/rechazo
2. **Reducir riesgo crediticio** con información completa de endeudamiento
3. **Mejorar experiencia del asesor** con datos consolidados
4. **Optimizar flujo de trabajo** con paginación y filtros automáticos
