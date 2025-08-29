package com.orchestrator.core.transformer;

public interface MessageTransformer {
    
    String transform(String input);
    
    default boolean isEnabled() {
        return true;
    }
}