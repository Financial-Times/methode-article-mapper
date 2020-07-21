package com.ft.methodearticlemapper.exception;

import com.ft.methodearticlemapper.transformation.TransformationMode;

@SuppressWarnings("serial")
public class UnsupportedTransformationModeException extends RuntimeException {

  private final String uuid;

  public UnsupportedTransformationModeException(String uuid, TransformationMode mode) {
    super(String.format("Transformation mode %s is not available", mode));
    this.uuid = uuid;
  }

  public String getUuid() {
    return uuid;
  }
}
