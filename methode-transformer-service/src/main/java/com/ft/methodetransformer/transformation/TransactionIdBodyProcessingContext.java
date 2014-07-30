package com.ft.methodetransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;

public interface TransactionIdBodyProcessingContext extends BodyProcessingContext {

	String getTransactionId();

}
