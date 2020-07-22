package com.ft.methodearticlemapper.exception;

import java.util.UUID;

public abstract class MethodeContentNotEligibleForPublishException extends RuntimeException {
  private static final long serialVersionUID = 6346781796023465981L;
  private final UUID uuid;

  public MethodeContentNotEligibleForPublishException(UUID uuid, String message) {
    super(message);
    this.uuid = uuid;
  }

  public UUID getUuid() {
    return uuid;
  }
}
