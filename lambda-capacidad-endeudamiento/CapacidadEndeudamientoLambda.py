import time
import requests
import json
import random
import logging
from datetime import datetime, timedelta
from dataclasses import dataclass
from typing import Optional, Dict, List, Any

# Configurar logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Constantes de configuración
class ConfiguracionCapacidad:
    PORCENTAJE_CAPACIDAD_MAXIMA = 0.35
    LIMITE_SALARIOS_REVISION_MANUAL = 5
    TASA_INTERES_DEFAULT = 12.0
    PLAZO_DEFAULT = 12
    DIAS_POR_MES = 30

@dataclass
class SolicitudData:
    """Datos de una solicitud de préstamo"""
    solicitud_id: str
    monto: float
    salario_base: float
    plazo_meses: int
    tasa_interes_anual: float
    email: str
    nombre_completo: str
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'SolicitudData':
        """Crea una instancia desde un diccionario validando los datos"""
        return cls(
            solicitud_id=str(data['solicitud_id']),
            monto=float(data['monto']),
            salario_base=float(data['salario_base']),
            plazo_meses=int(data.get('plazo_meses', ConfiguracionCapacidad.PLAZO_DEFAULT)),
            tasa_interes_anual=float(data.get('tasa_interes_anual', ConfiguracionCapacidad.TASA_INTERES_DEFAULT)),
            email=str(data['email']),
            nombre_completo=str(data['nombre_completo'])
        )

@dataclass
class ResultadoEvaluacion:
    """Resultado de la evaluación de capacidad de endeudamiento"""
    decision: str
    motivo: str
    capacidad_disponible: float
    cuota_calculada: float
    plan_pagos: List[Dict[str, Any]]

class CapacidadEndeudamientoLambda:
    def __init__(self):
        self.localstack_url = "http://localhost:4566"
        self.solicitudes_queue = "solicitudes-capacidad-queue"
        self.notificaciones_manuales_queue = "notificaciones-manuales-queue"
        self.resultados_queue = "resultados-evaluacion-queue"
        self.running = True

    def check_queue_for_messages(self, queue_name):
        """Verifica si hay mensajes en una cola específica"""
        try:
            # Primero crear la cola si no existe
            self.create_queue_if_not_exists(queue_name)

            url = f"{self.localstack_url}/000000000000/{queue_name}"
            params = {
                "Action": "ReceiveMessage",
                "MaxNumberOfMessages": "1",
                "WaitTimeSeconds": "5"
            }

            response = requests.get(url, params=params)

            if response.status_code == 200:
                # LocalStack devuelve XML, no JSON para SQS
                if "<Message>" in response.text:
                    logger.info(f"Mensaje encontrado en cola {queue_name}")
                    # Parsear XML simple para obtener el mensaje
                    import re
                    import html
                    body_match = re.search(r'<Body>(.*?)</Body>', response.text)
                    receipt_match = re.search(r'<ReceiptHandle>(.*?)</ReceiptHandle>', response.text)

                    if body_match:
                        # Decodificar HTML entities y limpiar el mensaje
                        message_body = html.unescape(body_match.group(1))
                        receipt_handle = receipt_match.group(1) if receipt_match else None
                        logger.info(f"Mensaje raw: {message_body[:100]}...")
                        return {"Body": message_body, "ReceiptHandle": receipt_handle, "QueueName": queue_name}

        except Exception as e:
            logger.error(f"Error verificando cola {queue_name}: {e}")

        return None

    def process_message(self, message):
        """Procesa un mensaje de cualquier cola"""
        try:
            queue_name = message.get('QueueName', '')
            message_body = message.get('Body', '{}')

            # Si el mensaje viene como string JSON escapado, limpiarlo
            if message_body.startswith('"') and message_body.endswith('"'):
                message_body = message_body[1:-1].replace('\\"', '"')

            body = json.loads(message_body)

            if queue_name == self.solicitudes_queue:
                self.process_solicitud(body, message)
            elif queue_name == self.notificaciones_manuales_queue:
                self.process_manual_notification(body, message)
            else:
                logger.warning(f"Cola desconocida: {queue_name}")

        except Exception as e:
            logger.error(f"Error procesando mensaje: {e}")

    def process_solicitud(self, body, message):
        """Procesa una solicitud de evaluación automática"""
        try:
            # Validar y estructurar datos de entrada
            solicitud = SolicitudData.from_dict(body)
            logger.info(f"Procesando solicitud: {solicitud.solicitud_id}")
            
            # Validar datos críticos
            if not self._validar_datos_solicitud(solicitud):
                logger.error(f"Datos inválidos para solicitud {solicitud.solicitud_id}")
                return
            
            # Evaluar capacidad de endeudamiento
            resultado = self._evaluar_capacidad_endeudamiento(solicitud)
            
            # Generar respuesta estructurada
            lambda_response = self._crear_respuesta_lambda(solicitud, resultado)
            
            logger.info(f"Decisión: {resultado.decision} - Cuota: {int(resultado.cuota_calculada)} - Capacidad: {int(resultado.capacidad_disponible)}")

            # Enviar respuesta a cola de resultados
            self.send_lambda_response(lambda_response)

            # Eliminar mensaje procesado de la cola
            self.delete_message(message)

        except Exception as e:
            logger.error(f"Error procesando solicitud: {e}")
    
    def _validar_datos_solicitud(self, solicitud: SolicitudData) -> bool:
        """Valida que los datos de la solicitud sean correctos"""
        if solicitud.monto <= 0:
            logger.error(f"Monto inválido: {solicitud.monto}")
            return False
        
        if solicitud.salario_base <= 0:
            logger.error(f"Salario base inválido: {solicitud.salario_base}")
            return False
        
        if solicitud.plazo_meses <= 0:
            logger.error(f"Plazo inválido: {solicitud.plazo_meses}")
            return False
        
        if solicitud.tasa_interes_anual <= 0:
            logger.error(f"Tasa de interés inválida: {solicitud.tasa_interes_anual}")
            return False
        
        return True
    
    def _evaluar_capacidad_endeudamiento(self, solicitud: SolicitudData) -> ResultadoEvaluacion:
        """Evalúa la capacidad de endeudamiento y toma decisión"""
        # Calcular cuota mensual del nuevo préstamo
        cuota_calculada = self._calcular_cuota_mensual(
            solicitud.monto, 
            solicitud.tasa_interes_anual, 
            solicitud.plazo_meses
        )
        
        # Calcular capacidad disponible (35% del salario)
        capacidad_disponible = self._calcular_capacidad_disponible(solicitud.salario_base)
        
        # Determinar decisión y motivo
        decision, motivo = self._determinar_decision(
            cuota_calculada, 
            capacidad_disponible, 
            solicitud.monto, 
            solicitud.salario_base
        )
        
        # Generar plan de pagos si es aprobado
        plan_pagos = []
        if decision == "APROBADO":
            plan_pagos = self.generar_plan_pagos(
                solicitud.monto, 
                solicitud.tasa_interes_anual, 
                solicitud.plazo_meses
            )
        
        return ResultadoEvaluacion(
            decision=decision,
            motivo=motivo,
            capacidad_disponible=capacidad_disponible,
            cuota_calculada=cuota_calculada,
            plan_pagos=plan_pagos
        )
    
    def _calcular_cuota_mensual(self, monto: float, tasa_anual: float, plazo_meses: int) -> float:
        """Calcula la cuota mensual usando la fórmula de amortización"""
        tasa_mensual = tasa_anual / 100 / 12
        if tasa_mensual == 0:
            return monto / plazo_meses
        
        return monto * (tasa_mensual * (1 + tasa_mensual)**plazo_meses) / ((1 + tasa_mensual)**plazo_meses - 1)
    
    def _calcular_capacidad_disponible(self, salario_base: float) -> float:
        """Calcula la capacidad de endeudamiento disponible (35% del salario)"""
        return salario_base * ConfiguracionCapacidad.PORCENTAJE_CAPACIDAD_MAXIMA
    
    def _determinar_decision(self, cuota: float, capacidad_disponible: float, monto: float, salario: float) -> tuple[str, str]:
        """Determina la decisión final basada en la capacidad y políticas de riesgo"""
        if cuota <= capacidad_disponible:
            if monto <= salario * ConfiguracionCapacidad.LIMITE_SALARIOS_REVISION_MANUAL:
                return "APROBADO", "Capacidad de pago suficiente"
            else:
                return "REVISION_MANUAL", "Monto excede 5 salarios, requiere revisión manual"
        else:
            return "RECHAZADO", "Capacidad de pago insuficiente"
    
    def _crear_respuesta_lambda(self, solicitud: SolicitudData, resultado: ResultadoEvaluacion) -> Dict[str, Any]:
        """Crea la respuesta estructurada para enviar a SQS"""
        return {
            "solicitudId": solicitud.solicitud_id,
            "decision": resultado.decision,
            "email": solicitud.email,
            "nombreCompleto": solicitud.nombre_completo,
            "motivo": resultado.motivo,
            "capacidadDisponible": int(resultado.capacidad_disponible),
            "cuotaCalculada": int(resultado.cuota_calculada),
            "montoAprobado": int(solicitud.monto) if resultado.decision == "APROBADO" else 0,
            "tasaInteresAnual": solicitud.tasa_interes_anual,
            "plazoMeses": solicitud.plazo_meses,
            "cuotaMensual": int(resultado.cuota_calculada) if resultado.decision == "APROBADO" else 0,
            "planPagos": resultado.plan_pagos
        }

    def process_manual_notification(self, body, message):
        """Procesa una notificación manual y envía email"""
        try:
            logger.info(f"Procesando notificación manual para aplicación: {body['solicitudId']}")

            # Extraer datos del mensaje
            solicitud_id = body['solicitudId']
            email = body.get('email', 'N/A')  # Este campo puede no venir
            decision = body['decision']
            comments = body['comments']
            reason = body.get('reason', '')  # Este campo puede no venir
            nombre_completo = body['nombreCompleto']

            # Simular envío de email
            self.send_manual_notification_email({
                'solicitudId': solicitud_id,
                'email': email,
                'decision': decision,
                'comments': comments,
                'reason': reason,
                'nombreCompleto': nombre_completo
            })

            # Eliminar mensaje procesado
            self.delete_message(message)

        except Exception as e:
            logger.error(f"Error procesando notificación manual: {e}")

    def send_manual_notification_email(self, notification_data):
        """Envía notificación manual usando SQS (mismo patrón que evaluaciones automáticas)"""
        try:
            solicitud_id = notification_data['solicitudId']
            email = notification_data['email']
            decision = notification_data['decision']
            comments = notification_data['comments']
            reason = notification_data['reason']

            logger.info(f"Procesando notificación manual para {email} - Decisión: {decision}")
            logger.info(f"Comentarios: {comments}")
            logger.info(f"Motivo: {reason}")

            # Crear mensaje de resultado para SQS (mismo formato que evaluaciones automáticas)
            resultado_message = {
                "solicitudId": solicitud_id,
                "email": email,
                "nombreCompleto": notification_data['nombreCompleto'],
                "decision": decision.upper(),
                "comments": comments,
                "reason": reason,
                "isManualNotification": True  # Flag para identificar notificaciones manuales
            }

            # Enviar a cola de resultados (mismo flujo que evaluaciones automáticas)
            self.send_lambda_response(resultado_message)

            logger.info(f"Notificación manual enviada a SQS para aplicación {solicitud_id}")

        except Exception as e:
            logger.error(f"Error enviando notificación manual: {e}")

    def create_queue_if_not_exists(self, queue_name):
        """Crea cola SQS si no existe"""
        try:
            # Usar endpoint raíz de LocalStack para crear colas
            url = f"{self.localstack_url}/"
            headers = {"Content-Type": "application/x-www-form-urlencoded"}
            data = f"Action=CreateQueue&QueueName={queue_name}&Version=2012-11-05"

            response = requests.post(url, headers=headers, data=data)
            if response.status_code == 200:
                logger.info(f"Cola {queue_name} creada/verificada")
            else:
                logger.warning(f"Respuesta cola {queue_name}: {response.status_code}")
        except Exception as e:
            logger.warning(f"Error creando cola {queue_name}: {e}")

    def send_lambda_response(self, response_data):
        """Envía respuesta Lambda simulada a cola de resultados"""
        try:
            # Crear cola de resultados si no existe
            self.create_queue_if_not_exists(self.resultados_queue)

            url = f"{self.localstack_url}/000000000000/{self.resultados_queue}"
            params = {
                "Action": "SendMessage",
                "MessageBody": json.dumps(response_data)
            }

            response = requests.get(url, params=params)

            if response.status_code == 200:
                logger.info("Respuesta Lambda enviada exitosamente")
            else:
                logger.error(f"Error enviando respuesta: {response.status_code}")

        except Exception as e:
            logger.error(f"Error enviando respuesta Lambda: {e}")

    def generar_plan_pagos(self, monto, tasa_anual, plazo_meses):
        """Genera plan de pagos detallado con capital e intereses"""
        plan = []
        tasa_mensual = tasa_anual / 12 / 100
        cuota_mensual = monto * (tasa_mensual * (1 + tasa_mensual)**plazo_meses) / ((1 + tasa_mensual)**plazo_meses - 1)
        saldo_pendiente = monto

        for mes in range(1, plazo_meses + 1):
            interes_mes = saldo_pendiente * tasa_mensual
            capital_mes = cuota_mensual - interes_mes
            saldo_pendiente -= capital_mes

            # Fecha de vencimiento (aproximada)
            fecha_vencimiento = (datetime.now() + timedelta(days=ConfiguracionCapacidad.DIAS_POR_MES*mes)).strftime("%Y-%m-%d")

            plan.append({
                "cuota": mes,
                "fecha_vencimiento": fecha_vencimiento,
                "capital": round(capital_mes, 2),
                "interes": round(interes_mes, 2),
                "saldo": round(max(0, saldo_pendiente), 2)
            })

        return plan

    def delete_message(self, message):
        """Elimina mensaje procesado de la cola"""
        try:
            receipt_handle = message.get('ReceiptHandle')
            queue_name = message.get('QueueName', 'unknown')

            if not receipt_handle:
                logger.warning("No se puede eliminar mensaje: falta ReceiptHandle")
                return

            url = f"{self.localstack_url}/000000000000/{queue_name}"
            params = {
                "Action": "DeleteMessage",
                "ReceiptHandle": receipt_handle
            }

            response = requests.get(url, params=params)

            if response.status_code == 200:
                logger.info(f"Mensaje eliminado exitosamente de {queue_name}")
            else:
                logger.error(f"Error eliminando mensaje de {queue_name}: {response.status_code}")

        except Exception as e:
            logger.error(f"Error eliminando mensaje: {e}")

    def run(self):
        """Ejecuta el simulador en loop continuo"""
        logger.info("Iniciando Lambda CAPACIDAD DE ENDEUDAMIENTO...")
        logger.info("Presione Ctrl+C para detener")

        # Crear colas al inicio
        self.create_queue_if_not_exists(self.solicitudes_queue)
        self.create_queue_if_not_exists(self.notificaciones_manuales_queue)
        self.create_queue_if_not_exists(self.resultados_queue)
        logger.info("Colas SQS inicializadas")

        try:
            while self.running:
                # Verificar ambas colas
                message_found = False

                # Verificar cola de solicitudes automáticas
                message = self.check_queue_for_messages(self.solicitudes_queue)
                if message:
                    self.process_message(message)
                    message_found = True

                # Verificar cola de notificaciones manuales
                message = self.check_queue_for_messages(self.notificaciones_manuales_queue)
                if message:
                    self.process_message(message)
                    message_found = True

                if not message_found:
                    logger.info("No hay mensajes pendientes en ninguna cola...")

                time.sleep(5)  # Esperar 5 segundos antes del siguiente ciclo

        except KeyboardInterrupt:
            logger.info("Simulador detenido por el usuario")
        except Exception as e:
            logger.error(f"Error en simulador: {e}")

if __name__ == "__main__":
    lambda_capacidad = CapacidadEndeudamientoLambda()
    lambda_capacidad.run()
