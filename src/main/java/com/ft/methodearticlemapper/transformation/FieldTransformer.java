package com.ft.methodearticlemapper.transformation;

import java.util.Map;

public interface FieldTransformer {

    String transform(
        String originalField,
        String transactionId,
        TransformationMode mode,
        Map.Entry<String, Object>... contextData);

}
