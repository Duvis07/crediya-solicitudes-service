import json
import boto3
import logging
import math
from decimal import Decimal, ROUND_HALF_UP
from typing import Dict, List, Any

# Configurar logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Clientes AWS para LocalStack
dynamodb = boto3.resource('dynamodb', endpoint_url='http://localhost:4566')
sns = boto3.client('sns', endpoint_url='http://localhost:4566')

def lambda_handler(event, context):
    """
    Lambda para calcular capacidad de endeudamiento y tomar decisión automática
    """
    try:
        logger.info(f"Evento recibido: {json.dumps(event)}")
        
        # Parsear datos de la solicitud
        body = json.loads(event['body']) if isinstance(event.get('body'), str) else event.get('body', {})
        
        solicitud_data = {
            'documento_identidad': body.get('documento_identidad'),
            'monto': Decimal(str(body.get('monto', 0))),
            'plazo_meses': int(body.get('plazo_meses', 0)),
            'tasa_interes_anual': Decimal(str(body.get('tasa_interes_anual', 0))),
            'salario_base': Decimal(str(body.get('salario_base', 0))),
            'solicitud_id': body.get('solicitud_id')
        }
        
        logger.info(f"Datos de solicitud: {solicitud_data}")
        
        # Calcular capacidad de endeudamiento
        resultado = calcular_capacidad_endeudamiento(solicitud_data)
        
        # Guardar resultado en DynamoDB
        guardar_resultado_calculo(solicitud_data['solicitud_id'], resultado)
        
        # Enviar notificación si es necesario
        if resultado['decision'] in ['APROBADO', 'REVISION_MANUAL']:
            enviar_plan_pagos(solicitud_data, resultado)
        
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({
                'solicitud_id': solicitud_data['solicitud_id'],
                'decision': resultado['decision'],
                'capacidad_disponible': float(resultado['capacidad_disponible']),
                'cuota_calculada': float(resultado['cuota_nueva']),
                'motivo': resultado['motivo']
            })
        }
        
    except Exception as e:
        logger.error(f"Error en lambda_handler: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': 'Error interno del servidor',
                'message': str(e)
            })
        }

def calcular_capacidad_endeudamiento(solicitud_data: Dict) -> Dict:
    """
    Calcula la capacidad de endeudamiento según las reglas de negocio
    """
    try:
        # 1. Capacidad máxima (35% del salario)
        capacidad_maxima = solicitud_data['salario_base'] * Decimal('0.35')
        logger.info(f"Capacidad máxima: {capacidad_maxima}")
        
        # 2. Obtener deuda mensual actual
        deuda_actual = obtener_deuda_mensual_actual(solicitud_data['documento_identidad'])
        logger.info(f"Deuda actual: {deuda_actual}")
        
        # 3. Capacidad disponible
        capacidad_disponible = capacidad_maxima - deuda_actual
        logger.info(f"Capacidad disponible: {capacidad_disponible}")
        
        # 4. Calcular cuota del nuevo préstamo
        cuota_nueva = calcular_cuota_prestamo(
            solicitud_data['monto'],
            solicitud_data['tasa_interes_anual'],
            solicitud_data['plazo_meses']
        )
        logger.info(f"Cuota nueva: {cuota_nueva}")
        
        # 5. Tomar decisión
        decision_data = tomar_decision(
            cuota_nueva, 
            capacidad_disponible, 
            solicitud_data['monto'], 
            solicitud_data['salario_base']
        )
        
        # 6. Generar plan de pagos si es aprobado
        plan_pagos = []
        if decision_data['decision'] in ['APROBADO', 'REVISION_MANUAL']:
            plan_pagos = generar_plan_pagos(
                solicitud_data['monto'],
                solicitud_data['tasa_interes_anual'],
                solicitud_data['plazo_meses']
            )
        
        return {
            'capacidad_maxima': capacidad_maxima,
            'deuda_actual': deuda_actual,
            'capacidad_disponible': capacidad_disponible,
            'cuota_nueva': cuota_nueva,
            'decision': decision_data['decision'],
            'motivo': decision_data['motivo'],
            'plan_pagos': plan_pagos
        }
        
    except Exception as e:
        logger.error(f"Error en calcular_capacidad_endeudamiento: {str(e)}")
        raise

def obtener_deuda_mensual_actual(documento_identidad: str) -> Decimal:
    """
    Obtiene la suma de cuotas mensuales de préstamos aprobados activos
    """
    try:
        # Simular consulta a DynamoDB de préstamos activos
        # En implementación real, consultaría la tabla de solicitudes aprobadas
        table = dynamodb.Table('prestamos-activos')
        
        response = table.scan(
            FilterExpression='documento_identidad = :doc AND estado_solicitud = :estado',
            ExpressionAttributeValues={
                ':doc': documento_identidad,
                ':estado': 'Aprobada'
            }
        )
        
        deuda_total = Decimal('0')
        for prestamo in response.get('Items', []):
            cuota = calcular_cuota_prestamo(
                Decimal(str(prestamo['monto'])),
                Decimal(str(prestamo['tasa_interes_anual'])),
                int(prestamo['plazo_meses'])
            )
            deuda_total += cuota
            
        return deuda_total
        
    except Exception as e:
        logger.warning(f"Error consultando deuda actual, asumiendo 0: {str(e)}")
        return Decimal('0')

def calcular_cuota_prestamo(monto: Decimal, tasa_anual: Decimal, plazo_meses: int) -> Decimal:
    """
    Calcula la cuota mensual usando la fórmula de amortización
    Cuota = P * (i * (1+i)^n) / ((1+i)^n - 1)
    """
    try:
        if monto <= 0 or tasa_anual <= 0 or plazo_meses <= 0:
            return Decimal('0')
            
        # Tasa mensual
        tasa_mensual = tasa_anual / Decimal('100') / Decimal('12')
        
        # Fórmula de amortización
        factor = (1 + tasa_mensual) ** plazo_meses
        cuota = monto * (tasa_mensual * factor) / (factor - 1)
        
        # Redondear a 2 decimales
        return cuota.quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
        
    except Exception as e:
        logger.error(f"Error calculando cuota: {str(e)}")
        return Decimal('0')

def tomar_decision(cuota_nueva: Decimal, capacidad_disponible: Decimal, 
                  monto: Decimal, salario_base: Decimal) -> Dict:
    """
    Toma la decisión de aprobación según las reglas de negocio
    """
    try:
        # Verificar si la cuota cabe en la capacidad
        if cuota_nueva > capacidad_disponible:
            return {
                'decision': 'RECHAZADO',
                'motivo': f'Cuota mensual ({cuota_nueva}) excede capacidad disponible ({capacidad_disponible})'
            }
        
        # Si cabe, verificar si es más de 5 salarios
        limite_revision_manual = salario_base * Decimal('5')
        
        if monto > limite_revision_manual:
            return {
                'decision': 'REVISION_MANUAL',
                'motivo': f'Monto ({monto}) excede 5 salarios base ({limite_revision_manual}), requiere revisión manual'
            }
        
        return {
            'decision': 'APROBADO',
            'motivo': f'Cuota mensual ({cuota_nueva}) dentro de capacidad disponible ({capacidad_disponible})'
        }
        
    except Exception as e:
        logger.error(f"Error tomando decisión: {str(e)}")
        return {
            'decision': 'RECHAZADO',
            'motivo': f'Error en evaluación: {str(e)}'
        }

def generar_plan_pagos(monto: Decimal, tasa_anual: Decimal, plazo_meses: int) -> List[Dict]:
    """
    Genera el plan de pagos (tabla de amortización)
    """
    try:
        plan = []
        saldo_pendiente = monto
        tasa_mensual = tasa_anual / Decimal('100') / Decimal('12')
        cuota_fija = calcular_cuota_prestamo(monto, tasa_anual, plazo_meses)
        
        for mes in range(1, plazo_meses + 1):
            interes_mes = (saldo_pendiente * tasa_mensual).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
            abono_capital = (cuota_fija - interes_mes).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
            saldo_pendiente = (saldo_pendiente - abono_capital).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
            
            # Ajuste para último mes (evitar saldos negativos por redondeo)
            if mes == plazo_meses:
                abono_capital += saldo_pendiente
                saldo_pendiente = Decimal('0')
            
            plan.append({
                'mes': mes,
                'cuota_total': float(cuota_fija),
                'abono_capital': float(abono_capital),
                'interes': float(interes_mes),
                'saldo_pendiente': float(saldo_pendiente)
            })
        
        return plan
        
    except Exception as e:
        logger.error(f"Error generando plan de pagos: {str(e)}")
        return []

def guardar_resultado_calculo(solicitud_id: str, resultado: Dict):
    """
    Guarda el resultado del cálculo en DynamoDB
    """
    try:
        table = dynamodb.Table('calculos-capacidad')
        
        # Convertir Decimals a float para JSON
        resultado_json = json.loads(json.dumps(resultado, default=str))
        
        table.put_item(
            Item={
                'solicitud_id': solicitud_id,
                'timestamp': int(context.aws_request_id) if 'context' in globals() else 0,
                'resultado': resultado_json
            }
        )
        logger.info(f"Resultado guardado para solicitud {solicitud_id}")
        
    except Exception as e:
        logger.error(f"Error guardando resultado: {str(e)}")

def enviar_plan_pagos(solicitud_data: Dict, resultado: Dict):
    """
    Envía el plan de pagos por SNS/Email
    """
    try:
        mensaje = {
            'solicitud_id': solicitud_data['solicitud_id'],
            'documento_identidad': solicitud_data['documento_identidad'],
            'decision': resultado['decision'],
            'monto': float(solicitud_data['monto']),
            'cuota_mensual': float(resultado['cuota_nueva']),
            'plan_pagos': resultado['plan_pagos']
        }
        
        sns.publish(
            TopicArn='arn:aws:sns:us-east-1:000000000000:notificaciones-prestamos',
            Message=json.dumps(mensaje),
            Subject=f'Plan de Pagos - Solicitud {solicitud_data["solicitud_id"]}'
        )
        
        logger.info(f"Plan de pagos enviado para solicitud {solicitud_data['solicitud_id']}")
        
    except Exception as e:
        logger.error(f"Error enviando plan de pagos: {str(e)}")
