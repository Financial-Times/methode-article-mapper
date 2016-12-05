package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessorChain;
import com.ft.bodyprocessing.DefaultTransactionIdBodyProcessingContext;

public class BylineProcessingFieldTransformer implements FieldTransformer {

    private final BodyProcessorChain bodyProcessorChain;

    public BylineProcessingFieldTransformer(BodyProcessorChain bodyProcessorChain) {
        this.bodyProcessorChain = bodyProcessorChain;
    }

    @Override
    public String transform(String originalBody, String transactionId) {
        BodyProcessingContext bodyProcessingContext = new DefaultTransactionIdBodyProcessingContext(transactionId);
        return bodyProcessorChain.process(originalBody, bodyProcessingContext);
    }

}