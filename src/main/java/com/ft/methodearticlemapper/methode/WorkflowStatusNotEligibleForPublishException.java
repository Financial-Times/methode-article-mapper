package com.ft.methodearticlemapper.methode;

import java.util.UUID;

public class WorkflowStatusNotEligibleForPublishException extends MethodeContentNotEligibleForPublishException {

	public WorkflowStatusNotEligibleForPublishException(UUID uuid, String workflowStatus) {
		super(uuid, String.format("Workflow status [%s] not eligible for publishing", workflowStatus));
	}
}
