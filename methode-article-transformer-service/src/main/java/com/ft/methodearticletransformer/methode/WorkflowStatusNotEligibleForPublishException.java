package com.ft.methodearticletransformer.methode;

import java.util.UUID;

public class WorkflowStatusNotEligibleForPublishException extends MethodeContentNotEligibleForPublishException {

	private final String workflowStatus;

	public WorkflowStatusNotEligibleForPublishException(UUID uuid, String workflowStatus) {
		super(uuid, String.format("Workflow status [%s] is not eligible for publishing.", workflowStatus));
		this.workflowStatus = workflowStatus;
	}

	public String getWorkflowStatus() {
		return workflowStatus;
	}
}
