package com.ft.methodearticletransformer.methode;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.UUID;

public class EmbargoDateInTheFutureException extends MethodeContentNotEligibleForPublishException {

	private final Date embargoDate;

	public EmbargoDateInTheFutureException(UUID uuid, Date embargoDate) {
		super(uuid, String.format("Embargo date is in future: [%s].", checkNotNull(embargoDate, "embargoDate cannot be null")));
		this.embargoDate = new Date(embargoDate.getTime());
	}

	public Date getEmbargoDate() {
		return new Date(embargoDate.getTime());
	}
}
