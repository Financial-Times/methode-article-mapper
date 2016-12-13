package com.ft.methodearticlemapper.exception;

import java.util.UUID;

import static com.ft.methodearticlemapper.methode.EomFileType.EOMCompoundStory;

public class UnsupportedEomTypeException extends MethodeContentNotEligibleForPublishException {
	public UnsupportedEomTypeException(UUID uuid, String type) {
		super(uuid, String.format("[%s] not an %s.", type, EOMCompoundStory.getTypeName()));
	}
}
