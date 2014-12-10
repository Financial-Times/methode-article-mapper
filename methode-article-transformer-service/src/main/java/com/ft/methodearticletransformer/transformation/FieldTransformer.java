package com.ft.methodearticletransformer.transformation;

import com.ft.content.model.Brand;

public interface FieldTransformer {

    String transform(String originalField, String transactionId);

}
