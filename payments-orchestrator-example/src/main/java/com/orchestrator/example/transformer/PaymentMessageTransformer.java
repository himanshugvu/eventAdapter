package com.orchestrator.example.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orchestrator.core.transformer.MessageTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageTransformer implements MessageTransformer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentMessageTransformer.class);
    
    private final ObjectMapper objectMapper;
    
    public PaymentMessageTransformer() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String transform(String input) {
        try {
            JsonNode originalPayment = objectMapper.readTree(input);
            ObjectNode enrichedPayment = originalPayment.deepCopy();
            
            enrichedPayment.put("processedAt", System.currentTimeMillis());
            enrichedPayment.put("orchestratorVersion", "1.0.0");
            
            if (originalPayment.has("amount") && originalPayment.has("currency")) {
                double amount = originalPayment.get("amount").asDouble();
                String currency = originalPayment.get("currency").asText();
                
                enrichedPayment.put("formattedAmount", formatCurrency(amount, currency));
                
                if (amount > 10000) {
                    enrichedPayment.put("requiresApproval", true);
                    enrichedPayment.put("approvalThreshold", "HIGH_VALUE");
                } else if (amount > 1000) {
                    enrichedPayment.put("requiresApproval", true);
                    enrichedPayment.put("approvalThreshold", "MEDIUM_VALUE");
                } else {
                    enrichedPayment.put("requiresApproval", false);
                    enrichedPayment.put("approvalThreshold", "AUTO_APPROVE");
                }
            }
            
            if (originalPayment.has("paymentMethod")) {
                String paymentMethod = originalPayment.get("paymentMethod").asText();
                enrichedPayment.put("processingFee", calculateProcessingFee(paymentMethod));
            }
            
            String result = objectMapper.writeValueAsString(enrichedPayment);
            logger.debug("Transformed payment message: {} -> {}", input, result);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to transform payment message: {}", input, e);
            return input;
        }
    }
    
    private String formatCurrency(double amount, String currency) {
        return String.format("%.2f %s", amount, currency.toUpperCase());
    }
    
    private double calculateProcessingFee(String paymentMethod) {
        return switch (paymentMethod.toLowerCase()) {
            case "credit_card" -> 2.9;
            case "debit_card" -> 1.5;
            case "bank_transfer" -> 0.5;
            case "digital_wallet" -> 2.0;
            default -> 3.0;
        };
    }
}