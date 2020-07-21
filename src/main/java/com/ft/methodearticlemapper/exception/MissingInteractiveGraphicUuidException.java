package com.ft.methodearticlemapper.exception;

import java.util.UUID;

public class MissingInteractiveGraphicUuidException
    extends MethodeContentNotEligibleForPublishException {
  public MissingInteractiveGraphicUuidException(UUID uuid, String message) {
    super(uuid, message);
  }
}
