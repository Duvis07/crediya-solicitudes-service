import time
import requests
import json
import random
import logging
from datetime import datetime

# Configurar logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class AutoLambdaSimulator:
    def __init__(self):
        self.localstack_url = "http://localhost:4566"
        self.solicitudes_queue = "solicitudes-capacidad-queue"
        self.resultados_queue = "resultados-evaluacion-queue"
        self.running = True
        
    def check_solicitudes_queue(self):
        """Verifica si hay mensajes en la cola de solicitudes"""
        try:
            # Primero crear la cola si no existe
            self.create_queue_if_not_exists(self.solicitudes_queue)
            
            url = f"{self.localstack_url}/000000000000/{self.solicitudes_queue}"
            params = {
                "Action": "ReceiveMessage",
                "MaxNumberOfMessages": "1",
                "WaitTimeSeconds": "5"
            }
            
            response = requests.get(url, params=params)
            
            if response.status_code == 200:
                # LocalStack devuelve XML, no JSON para SQS
                if "<Message>" in response.text:
                    logger.info("📨 Mensaje encontrado en cola")
                    # Parsear XML simple para obtener el mensaje
                    import re
                    import html
                    body_match = re.search(r'<Body>(.*?)</Body>', response.text)
                    receipt_match = re.search(r'<ReceiptHandle>(.*?)</ReceiptHandle>', response.text)
                    
                    if body_match:
                        # Decodificar HTML entities y limpiar el mensaje
                        message_body = html.unescape(body_match.group(1))
                        receipt_handle = receipt_match.group(1) if receipt_match else None
                        logger.info(f"🔍 Mensaje raw: {message_body[:100]}...")
                        return {"Body": message_body, "ReceiptHandle": receipt_handle}
                    
        except Exception as e:
            logger.error(f"Error verificando cola: {e}")
            
        return None
    
    def process_solicitud(self, message):
        """Procesa una solicitud y genera respuesta Lambda simulada"""
        try:
            # Parsear mensaje - puede venir con comillas escapadas
            message_body = message.get('Body', '{}')
            
            # Si el mensaje viene como string JSON escapado, limpiarlo
            if message_body.startswith('"') and message_body.endswith('"'):
                message_body = message_body[1:-1].replace('\\"', '"')
            
            body = json.loads(message_body)
            logger.info(f"🔄 Procesando solicitud: {body.get('solicitud_id', 'N/A')}")
            
            # Simular cálculo de capacidad (lógica simplificada)
            monto = float(body.get('monto', 0))
            salario = float(body.get('salario_base', 0))
            plazo = int(body.get('plazo_meses', 12))
            tasa = float(body.get('tasa_interes_anual', 12.0))
            
            # Calcular cuota mensual
            tasa_mensual = tasa / 100 / 12
            cuota = monto * (tasa_mensual * (1 + tasa_mensual)**plazo) / ((1 + tasa_mensual)**plazo - 1)
            
            # Capacidad disponible (35% del salario)
            capacidad_disponible = salario * 0.35
            
            # Decisión automática
            if cuota <= capacidad_disponible:
                if monto <= salario * 5:
                    decision = "APROBADO"
                    motivo = "Capacidad de pago suficiente"
                else:
                    decision = "REVISION_MANUAL" 
                    motivo = "Monto excede 5 salarios, requiere revisión manual"
            else:
                decision = "RECHAZADO"
                motivo = "Capacidad de pago insuficiente"
            
            # Generar plan de pagos detallado si es aprobado
            plan_pagos = []
            if decision == "APROBADO":
                plan_pagos = self.generar_plan_pagos(monto, tasa, plazo)
            
            # Generar respuesta Lambda
            lambda_response = {
                "solicitudId": body.get('solicitud_id'),
                "decision": decision,
                "email": body.get('email', 'test@example.com'),
                "nombreCompleto": body.get('nombre_completo', 'Usuario Test'),
                "motivo": motivo,
                "capacidadDisponible": int(capacidad_disponible),
                "cuotaCalculada": int(cuota),
                "montoAprobado": int(monto) if decision == "APROBADO" else 0,
                "tasaInteresAnual": tasa,
                "plazoMeses": plazo,
                "cuotaMensual": int(cuota) if decision == "APROBADO" else 0,
                "planPagos": plan_pagos
            }
            
            logger.info(f"✅ Decisión: {decision} - Cuota: {int(cuota)} - Capacidad: {int(capacidad_disponible)}")
            
            # Enviar respuesta a cola de resultados
            self.send_lambda_response(lambda_response)
            
            # Eliminar mensaje procesado de la cola
            self.delete_message(message)
            
        except Exception as e:
            logger.error(f"Error procesando solicitud: {e}")
    
    def create_queue_if_not_exists(self, queue_name):
        """Crea cola SQS si no existe"""
        try:
            # Usar endpoint raíz de LocalStack para crear colas
            url = f"{self.localstack_url}/"
            headers = {"Content-Type": "application/x-www-form-urlencoded"}
            data = f"Action=CreateQueue&QueueName={queue_name}&Version=2012-11-05"
            
            response = requests.post(url, headers=headers, data=data)
            if response.status_code == 200:
                logger.info(f"✅ Cola {queue_name} creada/verificada")
            else:
                logger.warning(f"⚠️ Respuesta cola {queue_name}: {response.status_code}")
        except Exception as e:
            logger.warning(f"⚠️ Error creando cola {queue_name}: {e}")
    
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
                logger.info(f"📤 Respuesta Lambda enviada exitosamente")
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
            from datetime import datetime, timedelta
            fecha_vencimiento = (datetime.now() + timedelta(days=30*mes)).strftime("%Y-%m-%d")
            
            plan.append({
                "cuota": mes,
                "fecha_vencimiento": fecha_vencimiento,
                "capital": round(capital_mes, 2),
                "interes": round(interes_mes, 2),
                "saldo": round(max(0, saldo_pendiente), 2)
            })
        
        return plan
    
    def delete_message(self, message):
        """Elimina mensaje procesado de la cola (deshabilitado en dev)"""
        # En desarrollo, no eliminamos mensajes para poder re-procesarlos
        logger.info("🗑️ Eliminación de mensaje deshabilitada en desarrollo")
                
    def run(self):
        """Ejecuta el simulador en loop continuo"""
        logger.info("🚀 Iniciando Lambda...")
        logger.info("📧 Presione Ctrl+C para detener")
        
        # Crear colas al inicio
        self.create_queue_if_not_exists(self.solicitudes_queue)
        self.create_queue_if_not_exists(self.resultados_queue)
        logger.info("✅ Colas SQS inicializadas")
        
        try:
            while self.running:
                # Verificar cola de solicitudes
                message = self.check_solicitudes_queue()
                
                if message:
                    self.process_solicitud(message)
                else:
                    logger.info("⏳ No hay mensajes pendientes...")
                
                time.sleep(15)  # Esperar 15 segundos antes del siguiente ciclo
                
        except KeyboardInterrupt:
            logger.info("🛑 Simulador detenido por el usuario")
        except Exception as e:
            logger.error(f"Error en simulador: {e}")

if __name__ == "__main__":
    simulator = AutoLambdaSimulator()
    simulator.run()
