package com.orchestrator.payment.transformer;

import com.orchestrator.core.transformer.MessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Payment-specific message transformer
 * Demonstrates custom transformation logic for this orchestrator
 */
@Component
public class PaymentMessageTransformer implements MessageTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentMessageTransformer.class);
    
    @Override
    public String transform(String input) {
        logger.debug("Transforming payment message: {}", input);
        
        // Payment-specific transformation logic
        // For demo purposes, we'll add a payment prefix and timestamp
        String transformed = String.format("{\"payment_processed\": true, \"original_message\": %s, \"processed_at\": %d}", 
                                         input, System.currentTimeMillis());
        
        logger.debug("Transformed message: {}", transformed);
        return transformed;
    }
}