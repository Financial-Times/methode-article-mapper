package com.ft.methodearticlemapper.transformation.eligibility;

import java.util.UUID;

import javax.xml.xpath.XPathExpressionException;

import com.ft.methodearticlemapper.methode.EomFileType;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.UnsupportedTypeException;
import com.ft.methodearticlemapper.exception.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticlemapper.model.EomFile;

public class EOMCompoundStoryPublishEligibilityChecker
    extends PublishEligibilityChecker {
  
  public static final String TYPE = EomFileType.EOMCompoundStory.getTypeName();
  
  public EOMCompoundStoryPublishEligibilityChecker(EomFile eomFile, UUID uuid, String transactionId) {
    super(eomFile, uuid, transactionId);
  }
  
  @Override
  public void checkType() {
    if (!TYPE.equals(eomFile.getType())) {
      throw new UnsupportedTypeException(uuid, eomFile.getType());
    }
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
