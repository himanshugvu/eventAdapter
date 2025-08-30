package com.orchestrator.example.inventory.transformer;

import com.orchestrator.core.transformer.MessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Inventory-specific message transformer
 * Demonstrates custom transformation logic for inventory management
 */
@Component
public class InventoryMessageTransformer implements MessageTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryMessageTransformer.class);
    
    @Override
    public String transform(String input) {
        logger.debug("Transforming inventory message: {}", input);
        
        // Inventory-specific transformation logic
        // Add inventory tracking metadata
        String transformed = String.format(
            "{\"inventory_updated\": true, \"original_message\": %s, \"updated_at\": %d, \"processor\": \"inventory-orchestrator\", \"version\": \"1.0\"}", 
            input, System.currentTimeMillis()
        );
        
        logger.debug("Transformed inventory message: {}", transformed);
        return transformed;
    }
}