import json
import time
from datetime import datetime
from locust import HttpUser, task, between
from kafka import KafkaProducer
from kafka.errors import KafkaError
import uuid
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class KafkaLoadUser(HttpUser):
    wait_time = between(0.001, 0.001)  # 1ms between requests for high TPS
    
    def on_start(self):
        """Initialize Kafka producer when user starts"""
        self.producer = KafkaProducer(
            bootstrap_servers=['kafka:9092'],  # Use docker network
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
            key_serializer=str.encode,
            # High performance configurations
            batch_size=32768,
            linger_ms=1,  # Very low latency
            compression_type='snappy',
            acks=1,  # Fast acknowledgment
            retries=3,
            buffer_memory=67108864,
            max_in_flight_requests_per_connection=5
        )
        self.message_count = 0
        
    def on_stop(self):
        """Clean up Kafka producer when user stops"""
        if hasattr(self, 'producer'):
            self.producer.close()
    
    @task(100)  # Weight 100 - primary task
    def send_payment_message(self):
        """Send payment message to Kafka with timestamp header"""
        self.message_count += 1
        
        # Generate unique payment data
        payment_id = f"payment-{uuid.uuid4().hex[:8]}-{self.message_count}"
        amount = (self.message_count % 10000) + 100
        
        # Current timestamp in nanoseconds for high precision
        send_timestamp_ns = time.time_ns()
        send_timestamp_iso = datetime.utcnow().isoformat() + 'Z'
        
        message = {
            "id": payment_id,
            "amount": amount,
            "currency": "USD",
            "timestamp": send_timestamp_iso,
            "user_id": f"user-{self.message_count % 1000}",
            "batch": f"locust-batch-{int(time.time())}",
            "sequence": self.message_count
        }
        
        # Headers with timestamp for latency tracking
        headers = {
            'send_timestamp_ns': str(send_timestamp_ns).encode('utf-8'),
            'send_timestamp_iso': send_timestamp_iso.encode('utf-8'),
            'source': 'locust-load-test'.encode('utf-8'),
            'message_id': payment_id.encode('utf-8'),
            'sequence': str(self.message_count).encode('utf-8')
        }
        
        try:
            # Send to Kafka with timing tracking
            start_send = time.time()
            
            future = self.producer.send(
                topic='payment-requests',
                key=payment_id,
                value=message,
                headers=list(headers.items())
            )
            
            # Get result to ensure message was sent
            record_metadata = future.get(timeout=1.0)
            
            send_duration_ms = (time.time() - start_send) * 1000
            
            logger.info(f"Message {payment_id} sent to partition {record_metadata.partition} "
                       f"at offset {record_metadata.offset}, send_time: {send_duration_ms:.2f}ms")
            
            # Success event for Locust statistics
            self.environment.events.request.fire(
                request_type="KAFKA",
                name="send_payment_message",
                response_time=send_duration_ms,
                response_length=len(json.dumps(message)),
                exception=None,
                context={}
            )
            
        except KafkaError as e:
            logger.error(f"Failed to send message {payment_id}: {e}")
            
            # Failure event for Locust statistics
            self.environment.events.request.fire(
                request_type="KAFKA",
                name="send_payment_message",
                response_time=0,
                response_length=0,
                exception=e,
                context={}
            )
            
        except Exception as e:
            logger.error(f"Unexpected error sending message {payment_id}: {e}")
            
            self.environment.events.request.fire(
                request_type="KAFKA",
                name="send_payment_message",
                response_time=0,
                response_length=0,
                exception=e,
                context={}
            )
    
    @task(1)  # Weight 1 - health check task (low frequency)
    def health_check(self):
        """Periodic health check of the orchestrator application"""
        try:
            with self.client.get("/actuator/health", timeout=2) as response:
                if response.status_code == 200:
                    logger.debug("Health check passed")
                else:
                    logger.warning(f"Health check failed with status: {response.status_code}")
        except Exception as e:
            logger.error(f"Health check error: {e}")


# Custom configuration for high TPS load testing
class WebsiteUser(HttpUser):
    """Alternative user class for HTTP-only testing"""
    wait_time = between(1, 2)
    
    @task
    def health_check(self):
        self.client.get("/actuator/health")
        
    @task
    def metrics_check(self):
        self.client.get("/actuator/metrics")