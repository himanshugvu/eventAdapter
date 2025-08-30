package com.orchestrator.core.transformer;

/**
 * Interface for message transformation logic.
 * Implementations provide custom business logic for transforming messages.
 */
public interface MessageTransformer {
    
    /**
     * Transform input message to output message
     * @param input Original message payload
     * @return Transformed message payload
     */
    String transform(String input);
    
    /**
     * Validate if the input message is valid for processing
     * @param input Message payload to validate
     * @return true if valid, false otherwise
     */
    default boolean isValidMessage(String input) {
        return input != null && !input.trim().isEmpty();
    }
    
    /**
     * Get transformer name for logging and metrics
     * @return Transformer identifier
     */
    default String getTransformerName() {
        return this.getClass().getSimpleName();
    }
}