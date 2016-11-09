package com.ft.methodearticlemapper.methode;

import java.util.UUID;

import static com.ft.methodearticlemapper.methode.EomFileType.EOMCompoundStory;

public class UnsupportedTypeException extends MethodeContentNotEligibleForPublishException {
	public UnsupportedTypeException(UUID uuid, String type) {
		super(uuid, String.format("[%s] not an %s.", type, EOMCompoundStory.getTypeName()));
	}
}
