package com.orchestrator.example.payment.transformer;

import com.orchestrator.core.transformer.MessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Payment-specific message transformer
 * Demonstrates custom transformation logic for payment processing
 */
@Component
public class PaymentMessageTransformer implements MessageTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentMessageTransformer.class);
    
    @Override
    public String transform(String input) {
        logger.debug("Transforming payment message: {}", input);
        
        // Payment-specific transformation logic
        // Add payment processing metadata and timestamp
        String transformed = String.format(
            "{\"payment_processed\": true, \"original_message\": %s, \"processed_at\": %d, \"processor\": \"payment-orchestrator\"}", 
            input, System.currentTimeMillis()
        );
        
        logger.debug("Transformed payment message: {}", transformed);
        return transformed;
    }
}