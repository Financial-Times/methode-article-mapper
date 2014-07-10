package com.ft.methodetransformer.methode;

import java.util.UUID;

public class NotWebChannelException extends MethodeContentNotEligibleForPublishException {

	public NotWebChannelException(UUID uuid) {
		super(uuid, "This is not a web story.");
	}
}
