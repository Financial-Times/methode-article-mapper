package com.ft.methodearticlemapper.exception;

import static com.ft.methodearticlemapper.methode.EomFileType.EOMCompoundStory;

import java.util.UUID;

public class UnsupportedEomTypeException extends MethodeContentNotEligibleForPublishException {
  public UnsupportedEomTypeException(UUID uuid, String type) {
    super(uuid, String.format("[%s] not an %s.", type, EOMCompoundStory.getTypeName()));
  }
}
