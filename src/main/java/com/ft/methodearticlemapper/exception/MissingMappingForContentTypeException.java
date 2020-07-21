package com.ft.methodearticlemapper.exception;

public class MissingMappingForContentTypeException extends RuntimeException {

  public MissingMappingForContentTypeException(String contentType) {
    super(String.format("There is no mapping configured for content type %s", contentType));
  }
}
