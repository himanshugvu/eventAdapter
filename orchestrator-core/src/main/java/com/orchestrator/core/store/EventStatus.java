package com.orchestrator.core.store;

public enum EventStatus {
    RECEIVED,
    PROCESSING,
    SUCCESS,
    FAILED,
    RETRY
}