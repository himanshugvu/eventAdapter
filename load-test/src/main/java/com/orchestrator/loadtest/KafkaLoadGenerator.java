package com.orchestrator.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@CommandLine.Command(name = "kafka-load-generator", description = "Generate high-volume Kafka load for orchestrator testing")
public class KafkaLoadGenerator implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaLoadGenerator.class);
    
    @CommandLine.Option(names = {"--bootstrap-servers"}, defaultValue = "localhost:9092")
    private String bootstrapServers;
    
    @CommandLine.Option(names = {"--topic"}, defaultValue = "payment-requests")
    private String topic;
    
    @CommandLine.Option(names = {"--records"}, defaultValue = "1000000")
    private long totalRecords;
    
    @CommandLine.Option(names = {"--batch-size"}, defaultValue = "1000")
    private int batchSize;
    
    @CommandLine.Option(names = {"--threads"}, defaultValue = "10")
    private int threads;
    
    @CommandLine.Option(names = {"--rate-limit"}, defaultValue = "10000")
    private int rateLimit;
    
    @CommandLine.Option(names = {"--test-duration"}, defaultValue = "0")
    private int testDurationMinutes;
    
    @CommandLine.Option(names = {"--results-file"}, defaultValue = "/app/results/load-test-results.csv")
    private String resultsFile;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Faker faker = new Faker();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    
    private Counter sentCounter;
    private Counter successCounter;
    private Counter errorCounter;
    private Timer sendTimer;
    private AtomicLong recordsSent = new AtomicLong(0);
    private AtomicLong errors = new AtomicLong(0);
    
    public static void main(String[] args) {
        // Override with environment variables if available
        String bootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String records = System.getenv().getOrDefault("LOAD_TEST_RECORDS", "1000000");
        String batchSize = System.getenv().getOrDefault("LOAD_TEST_BATCH_SIZE", "1000");
        String threads = System.getenv().getOrDefault("LOAD_TEST_THREADS", "10");
        String rateLimit = System.getenv().getOrDefault("LOAD_TEST_RATE_LIMIT", "10000");
        
        String[] enrichedArgs = {
            "--bootstrap-servers", bootstrapServers,
            "--records", records,
            "--batch-size", batchSize,
            "--threads", threads,
            "--rate-limit", rateLimit
        };
        
        int exitCode = new CommandLine(new KafkaLoadGenerator()).execute(enrichedArgs);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        logger.info("Starting Kafka Load Generator");
        logger.info("Target: {} records to {} at {} records/sec using {} threads", 
                   totalRecords, bootstrapServers, rateLimit, threads);
        
        initializeMetrics();
        
        long startTime = System.currentTimeMillis();
        
        try (KafkaProducer<String, String> producer = createProducer()) {
            
            if (testDurationMinutes > 0) {
                runTimeBoundedTest(producer, testDurationMinutes);
            } else {
                runRecordBoundedTest(producer, totalRecords);
            }
            
        } catch (Exception e) {
            logger.error("Load test failed", e);
            return 1;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        generateReport(startTime, duration);
        
        return 0;
    }
    
    private void initializeMetrics() {
        sentCounter = Counter.builder("kafka.records.sent")
            .description("Total records sent")
            .register(meterRegistry);
            
        successCounter = Counter.builder("kafka.records.success")
            .description("Successfully sent records")
            .register(meterRegistry);
            
        errorCounter = Counter.builder("kafka.records.error")
            .description("Failed records")
            .register(meterRegistry);
            
        sendTimer = Timer.builder("kafka.send.duration")
            .description("Time to send records")
            .register(meterRegistry);
    }
    
    private KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // Performance optimizations for load testing
        props.put(ProducerConfig.ACKS_CONFIG, "1"); // Lower latency for load testing
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        return new KafkaProducer<>(props);
    }
    
    private void runRecordBoundedTest(KafkaProducer<String, String> producer, long targetRecords) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Semaphore rateLimiter = new Semaphore(rateLimit);
        
        long recordsPerThread = targetRecords / threads;
        CountDownLatch latch = new CountDownLatch(threads);
        
        logger.info("Starting {} producer threads, {} records each", threads, recordsPerThread);
        
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            final long startRecord = i * recordsPerThread;
            final long endRecord = (i == threads - 1) ? targetRecords : (i + 1) * recordsPerThread;
            
            executor.submit(() -> {
                try {
                    runProducerThread(producer, threadId, startRecord, endRecord, rateLimiter);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
    
    private void runTimeBoundedTest(KafkaProducer<String, String> producer, int durationMinutes) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Semaphore rateLimiter = new Semaphore(rateLimit);
        
        long endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        
        logger.info("Starting {} minute duration test with {} threads", durationMinutes, threads);
        
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> runTimeBoundedProducerThread(producer, threadId, endTime, rateLimiter));
        }
        
        executor.shutdown();
        executor.awaitTermination(durationMinutes + 1, TimeUnit.MINUTES);
    }
    
    private void runProducerThread(KafkaProducer<String, String> producer, int threadId, 
                                 long startRecord, long endRecord, Semaphore rateLimiter) {
        
        logger.info("Thread {} starting: records {} to {}", threadId, startRecord, endRecord);
        
        for (long i = startRecord; i < endRecord; i++) {
            try {
                rateLimiter.acquire();
                
                String paymentMessage = generatePaymentMessage(i);
                String key = "payment-" + (i % 1000); // Distribute across partitions
                
                Timer.Sample sample = Timer.start(meterRegistry);
                
                producer.send(new ProducerRecord<>(topic, key, paymentMessage), 
                    new LoadTestCallback(sample, sendTimer, sentCounter, successCounter, errorCounter));
                
                recordsSent.incrementAndGet();
                
                if (i % 10000 == 0) {
                    logger.info("Thread {} sent {} records", threadId, i - startRecord);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                errors.incrementAndGet();
                logger.error("Error sending record {}", i, e);
            }
        }
        
        logger.info("Thread {} completed", threadId);
    }
    
    private void runTimeBoundedProducerThread(KafkaProducer<String, String> producer, int threadId, 
                                            long endTime, Semaphore rateLimiter) {
        
        logger.info("Thread {} starting time-bounded test", threadId);
        long recordCount = 0;
        
        while (System.currentTimeMillis() < endTime && !Thread.currentThread().isInterrupted()) {
            try {
                rateLimiter.acquire();
                
                String paymentMessage = generatePaymentMessage(recordCount);
                String key = "payment-" + (recordCount % 1000);
                
                Timer.Sample sample = Timer.start(meterRegistry);
                
                producer.send(new ProducerRecord<>(topic, key, paymentMessage), 
                    new LoadTestCallback(sample, sendTimer, sentCounter, successCounter, errorCounter));
                
                recordsSent.incrementAndGet();
                recordCount++;
                
                if (recordCount % 10000 == 0) {
                    logger.info("Thread {} sent {} records", threadId, recordCount);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                errors.incrementAndGet();
                logger.error("Error in thread {} at record {}", threadId, recordCount, e);
            }
        }
        
        logger.info("Thread {} completed with {} records", threadId, recordCount);
    }
    
    private String generatePaymentMessage(long sequence) throws Exception {
        PaymentMessage payment = new PaymentMessage();
        payment.paymentId = "load-test-" + sequence;
        payment.amount = faker.number().randomDouble(2, 10, 50000);
        payment.currency = faker.options().option("USD", "EUR", "GBP", "JPY", "CAD");
        payment.paymentMethod = faker.options().option("credit_card", "debit_card", "bank_transfer", "digital_wallet");
        payment.merchantId = "merchant-" + faker.number().numberBetween(1, 1000);
        payment.customerId = "customer-" + faker.number().numberBetween(1, 100000);
        payment.timestamp = System.currentTimeMillis();
        payment.description = faker.commerce().productName();
        payment.metadata = new PaymentMetadata();
        payment.metadata.ipAddress = faker.internet().ipV4Address();
        payment.metadata.userAgent = faker.internet().userAgentAny();
        payment.metadata.sessionId = faker.internet().uuid();
        
        return objectMapper.writeValueAsString(payment);
    }
    
    private void generateReport(long startTime, long duration) throws IOException {
        long totalSent = recordsSent.get();
        long totalErrors = errors.get();
        double throughput = (totalSent * 1000.0) / duration;
        double errorRate = (totalErrors * 100.0) / totalSent;
        
        String report = String.format("""
            === KAFKA LOAD TEST RESULTS ===
            Start Time: %s
            Duration: %d ms (%.2f minutes)
            Records Sent: %d
            Errors: %d (%.2f%%)
            Throughput: %.2f records/second
            Average Send Time: %.2f ms
            =============================
            """,
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            duration, duration / 60000.0,
            totalSent, totalErrors, errorRate,
            throughput,
            sendTimer.mean(TimeUnit.MILLISECONDS)
        );
        
        logger.info(report);
        
        // Write detailed CSV results
        try (FileWriter writer = new FileWriter(resultsFile)) {
            writer.write("metric,value\n");
            writer.write("start_time," + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            writer.write("duration_ms," + duration + "\n");
            writer.write("records_sent," + totalSent + "\n");
            writer.write("errors," + totalErrors + "\n");
            writer.write("error_rate_percent," + errorRate + "\n");
            writer.write("throughput_records_per_second," + throughput + "\n");
            writer.write("average_send_time_ms," + sendTimer.mean(TimeUnit.MILLISECONDS) + "\n");
            writer.write("max_send_time_ms," + sendTimer.max(TimeUnit.MILLISECONDS) + "\n");
            writer.write("p95_send_time_ms," + sendTimer.percentile(0.95, TimeUnit.MILLISECONDS) + "\n");
            writer.write("p99_send_time_ms," + sendTimer.percentile(0.99, TimeUnit.MILLISECONDS) + "\n");
        }
        
        logger.info("Results written to: {}", resultsFile);
    }
    
    private static class LoadTestCallback implements Callback {
        private final Timer.Sample sample;
        private final Timer sendTimer;
        private final Counter sentCounter;
        private final Counter successCounter;
        private final Counter errorCounter;
        
        public LoadTestCallback(Timer.Sample sample, Timer sendTimer, Counter sentCounter, Counter successCounter, Counter errorCounter) {
            this.sample = sample;
            this.sendTimer = sendTimer;
            this.sentCounter = sentCounter;
            this.successCounter = successCounter;
            this.errorCounter = errorCounter;
        }
        
        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            sample.stop(sendTimer);
            sentCounter.increment();
            
            if (exception == null) {
                successCounter.increment();
            } else {
                errorCounter.increment();
            }
        }
    }
    
    private static class PaymentMessage {
        public String paymentId;
        public double amount;
        public String currency;
        public String paymentMethod;
        public String merchantId;
        public String customerId;
        public long timestamp;
        public String description;
        public PaymentMetadata metadata;
    }
    
    private static class PaymentMetadata {
        public String ipAddress;
        public String userAgent;
        public String sessionId;
    }
}