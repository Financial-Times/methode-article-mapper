package com.ft.methodearticletransformer.methode;

import java.util.UUID;

public class SourceNotEligibleForPublishException extends MethodeContentNotEligibleForPublishException {

	private final String source;

	public SourceNotEligibleForPublishException(UUID uuid, String source) {
		super(uuid, String.format("Source [%s] is not eligible for publishing.", source));
		this.source = source;
	}

	public String getSource() {
		return source;
	}
}
