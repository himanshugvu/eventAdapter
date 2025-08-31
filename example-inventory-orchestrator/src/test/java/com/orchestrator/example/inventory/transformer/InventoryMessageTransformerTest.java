package com.orchestrator.example.inventory.transformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMessageTransformerTest {

    private InventoryMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new InventoryMessageTransformer();
    }

    @Test
    void testTransformValidJson() {
        String input = "{\"productId\":\"PROD123\",\"quantity\":50,\"operation\":\"ADD\",\"warehouseId\":\"WH001\"}";
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"productId\":\"PROD123\""));
        assertTrue(result.contains("\"adjustedQuantity\":50"));
        assertTrue(result.contains("\"operation\":\"ADD\""));
        assertTrue(result.contains("\"warehouseId\":\"WH001\""));
        assertTrue(result.contains("\"status\":\"INVENTORY_UPDATED\""));
        assertTrue(result.contains("\"processedAt\":"));
    }

    @Test
    void testTransformRemoveOperation() {
        String input = "{\"productId\":\"PROD123\",\"quantity\":25,\"operation\":\"REMOVE\",\"warehouseId\":\"WH001\"}";
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"adjustedQuantity\":-25"));
        assertTrue(result.contains("\"operation\":\"REMOVE\""));
    }

    @Test
    void testTransformInvalidJson() {
        String input = "invalid json";
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"error\":\"Invalid inventory format\""));
        assertTrue(result.contains("\"originalMessage\":\"invalid json\""));
        assertTrue(result.contains("\"status\":\"ERROR\""));
    }

    @Test
    void testTransformNullInput() {
        assertThrows(RuntimeException.class, () -> transformer.transform(null));
    }

    @Test
    void testTransformEmptyInput() {
        assertThrows(RuntimeException.class, () -> transformer.transform(""));
    }

    @Test
    void testTransformMissingFields() {
        String input = "{\"productId\":\"PROD123\"}"; // Missing quantity and operation
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"error\":\"Missing required fields\""));
        assertTrue(result.contains("\"status\":\"ERROR\""));
    }

    @Test
    void testTransformInvalidOperation() {
        String input = "{\"productId\":\"PROD123\",\"quantity\":50,\"operation\":\"INVALID\",\"warehouseId\":\"WH001\"}";
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"error\":\"Invalid operation\""));
        assertTrue(result.contains("\"status\":\"ERROR\""));
    }

    @Test
    void testGetTransformerName() {
        assertEquals("InventoryMessageTransformer", transformer.getTransformerName());
    }

    @Test
    void testIsValidMessage() {
        assertTrue(transformer.isValidMessage("valid message"));
        assertFalse(transformer.isValidMessage(null));
        assertFalse(transformer.isValidMessage(""));
        assertFalse(transformer.isValidMessage("   "));
    }
}