package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;

public interface ModalBodyProcessingContext extends BodyProcessingContext {
    TransformationMode getTransformationMode();
}
