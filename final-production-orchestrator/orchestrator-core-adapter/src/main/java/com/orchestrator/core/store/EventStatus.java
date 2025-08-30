package com.orchestrator.core.store;

/**
 * Event processing status enumeration
 */
public enum EventStatus {
    RECEIVED,   // Event received from Kafka topic
    PROCESSING, // Event is being processed/transformed
    SUCCESS,    // Event successfully processed and published
    FAILED,     // Event processing failed
    RETRYING    // Event is being retried
}