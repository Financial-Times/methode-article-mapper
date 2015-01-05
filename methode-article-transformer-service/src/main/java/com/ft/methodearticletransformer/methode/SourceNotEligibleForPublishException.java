package com.ft.methodearticletransformer.methode;

import java.util.UUID;

public class SourceNotEligibleForPublishException extends MethodeContentNotEligibleForPublishException {

	public SourceNotEligibleForPublishException(UUID uuid, String source) {
		super(uuid, String.format("Source [%s] not eligible for publishing", source));
	}
}
