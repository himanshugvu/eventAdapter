#!/bin/bash

# Load test script to generate 1M messages through Kafka console producer
TOTAL_RECORDS=1000000
BATCH_SIZE=10000
TOPIC="payment-requests"
BOOTSTRAP_SERVERS="kafka:9092"

echo "Starting load test: $TOTAL_RECORDS records to topic $TOPIC"
echo "Start time: $(date)"

START_TIME=$(date +%s)

# Generate messages in batches
for ((batch=1; batch<=100; batch++))
do
    echo "Processing batch $batch/100..."
    
    # Generate batch of messages
    {
        for ((i=1; i<=BATCH_SIZE; i++))
        do
            record_id=$((($batch - 1) * $BATCH_SIZE + $i))
            amount=$((record_id * 10 + 100))
            echo "{\"id\":\"payment-$record_id\",\"amount\":$amount,\"currency\":\"USD\",\"timestamp\":\"$(date -Iseconds)\",\"batch\":$batch}"
        done
    } | kafka-console-producer --bootstrap-server $BOOTSTRAP_SERVERS --topic $TOPIC
    
    if [ $((batch % 10)) -eq 0 ]; then
        echo "Completed $((batch * BATCH_SIZE)) records so far..."
    fi
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo "Load test completed!"
echo "End time: $(date)"
echo "Total records: $TOTAL_RECORDS"
echo "Duration: ${DURATION} seconds"
echo "Throughput: $((TOTAL_RECORDS / DURATION)) records/second"