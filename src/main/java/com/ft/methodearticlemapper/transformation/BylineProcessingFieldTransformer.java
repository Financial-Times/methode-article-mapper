package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessorChain;
import java.util.Map;

public class BylineProcessingFieldTransformer implements FieldTransformer {

  private final BodyProcessorChain bodyProcessorChain;

  public BylineProcessingFieldTransformer(BodyProcessorChain bodyProcessorChain) {
    this.bodyProcessorChain = bodyProcessorChain;
  }

  @Override
  public String transform(
      String originalBody,
      String transactionId,
      TransformationMode mode,
      Map.Entry<String, Object>... contextData) {
    return bodyProcessorChain.process(
        originalBody, new MappedDataBodyProcessingContext(transactionId, mode, contextData));
  }
}
