package com.orchestrator.example.payment.transformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMessageTransformerTest {

    private PaymentMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new PaymentMessageTransformer();
    }

    @Test
    void testTransformValidJson() {
        String input = "{\"amount\":100.50,\"currency\":\"USD\",\"accountId\":\"12345\"}";
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"payment_processed\":true"));
        assertTrue(result.contains("\"original_message\":" + input));
        assertTrue(result.contains("\"processor\":\"payment-orchestrator\""));
        assertTrue(result.contains("\"processed_at\":"));
    }

    @Test
    void testTransformInvalidJson() {
        String input = "invalid json";
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"error\":\"Invalid payment format\""));
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
        String input = "{\"amount\":100.50}"; // Missing currency and accountId
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"error\":\"Missing required fields\""));
        assertTrue(result.contains("\"status\":\"ERROR\""));
    }

    @Test
    void testTransformNegativeAmount() {
        String input = "{\"amount\":-100.50,\"currency\":\"USD\",\"accountId\":\"12345\"}";
        String result = transformer.transform(input);
        
        assertNotNull(result);
        assertTrue(result.contains("\"error\":\"Invalid amount\""));
        assertTrue(result.contains("\"status\":\"ERROR\""));
    }

    @Test
    void testGetTransformerName() {
        assertEquals("PaymentMessageTransformer", transformer.getTransformerName());
    }

    @Test
    void testIsValidMessage() {
        assertTrue(transformer.isValidMessage("valid message"));
        assertFalse(transformer.isValidMessage(null));
        assertFalse(transformer.isValidMessage(""));
        assertFalse(transformer.isValidMessage("   "));
    }
}