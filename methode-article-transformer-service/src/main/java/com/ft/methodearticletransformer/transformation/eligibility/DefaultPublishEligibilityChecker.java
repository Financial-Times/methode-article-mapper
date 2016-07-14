package com.ft.methodearticletransformer.transformation.eligibility;

import java.util.UUID;

import javax.xml.xpath.XPathExpressionException;

import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticletransformer.model.EomFile;

public class DefaultPublishEligibilityChecker
    extends PublishEligibilityChecker {

  public DefaultPublishEligibilityChecker(EomFile eomFile, UUID uuid, String transactionId) {
    super(eomFile, uuid, transactionId);
  }
  
  @Override
  protected void checkWorkflowStatus() {
    String workflowStatus = eomFile.getWorkflowStatus();
    if (!isValidWorkflowStatusForWebPublication(workflowStatus)) {
      throw new WorkflowStatusNotEligibleForPublishException(uuid, workflowStatus);
    }
  }

  @Override
  protected void checkChannel()
      throws XPathExpressionException {
    
    String channel = xpath.evaluate(CHANNEL_SYSTEM_ATTR_XPATH, systemAttributesDocument);
    if (!isWebChannel(channel)) {
      throw new NotWebChannelException(uuid);
    }
  }
}
