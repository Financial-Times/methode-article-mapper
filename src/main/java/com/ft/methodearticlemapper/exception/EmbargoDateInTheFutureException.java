package com.ft.methodearticlemapper.exception;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.UUID;

public class EmbargoDateInTheFutureException extends MethodeContentNotEligibleForPublishException {

	public EmbargoDateInTheFutureException(UUID uuid, Date embargoDate) {
		super(uuid, String.format("Embargo date [%s] is in the future",
				checkNotNull(embargoDate, "embargoDate cannot be null")));
	}
}
