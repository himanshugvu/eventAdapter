package com.orchestrator.core.transformer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(MessageTransformer.class)
public class DefaultMessageTransformer implements MessageTransformer {
    
    @Override
    public String transform(String input) {
        return input;
    }
}