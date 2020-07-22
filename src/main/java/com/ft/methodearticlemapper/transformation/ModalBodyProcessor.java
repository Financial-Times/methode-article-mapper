package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.BodyProcessor;
import java.util.EnumSet;

public class ModalBodyProcessor implements BodyProcessor {
  private BodyProcessor processor;
  private EnumSet<TransformationMode> allowedModes;

  public ModalBodyProcessor(BodyProcessor toWrap, EnumSet<TransformationMode> inModes) {
    this.processor = toWrap;
    this.allowedModes = inModes;
  }

  @Override
  public String process(String body, BodyProcessingContext bodyProcessingContext)
      throws BodyProcessingException {
    if (!(bodyProcessingContext instanceof ModalBodyProcessingContext)) {
      throw new IllegalArgumentException(
          "Using a ModalBodyProcessor requires a ModalBodyProcessingContext.");
    }

    TransformationMode mode =
        ((ModalBodyProcessingContext) bodyProcessingContext).getTransformationMode();
    return allowedModes.contains(mode) ? processor.process(body, bodyProcessingContext) : body;
  }
}
