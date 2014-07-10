package com.ft.methodetransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessorChain;

public class BodyProcessingFieldTransformer implements FieldTransformer {

    private final BodyProcessorChain bodyProcessorChain;

    public BodyProcessingFieldTransformer(BodyProcessorChain bodyProcessorChain) {
        this.bodyProcessorChain = bodyProcessorChain;
    }

    @Override
    public String transform(String originalBody, String transactionId) {
        BodyProcessingContext bodyProcessingContext = new DefaultTransactionIdBodyProcessingContext(transactionId);
        return bodyProcessorChain.process(originalBody, bodyProcessingContext);
    }

}
