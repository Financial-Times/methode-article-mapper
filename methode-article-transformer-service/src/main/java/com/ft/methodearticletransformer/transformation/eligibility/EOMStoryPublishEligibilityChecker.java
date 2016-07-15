package com.ft.methodearticletransformer.transformation.eligibility;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.xpath.XPathExpressionException;

import com.ft.methodearticletransformer.methode.EomFileType;
import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.UnsupportedTypeException;
import com.ft.methodearticletransformer.methode.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.transformation.TransformationException;
import com.google.common.base.Strings;

public class EOMStoryPublishEligibilityChecker extends PublishEligibilityChecker {
  public static final String TYPE = EomFileType.EOMStory.getTypeName();
  
  private static final String EOM_STORY_INITIAL_PUBLISH_DATE_XPATH =
      "/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomInitialPublication";

  private static final List<String> ALLOWED_WORKFLOWS_PRE_ENFORCE_DATE =
      Arrays.asList("", EomFile.WEB_READY, EomFile.WEB_REVISE,
          "FTContentMove/Ready", "FTContentMove/Editing_Methode", "FTContentMove/Revise",
          "FTContentMove/Subbing_Methode", "FTContentMove/Released",
          "Stories/Ready", "Stories/Released", "Stories/Revise", "Stories/Sub");
  
  private static final Date WORKFLOW_STATUS_ENFORCE_DATE =
      toDate("20110601000000", METHODE_XML_DATE_TIME_FORMAT);
  
  public EOMStoryPublishEligibilityChecker(EomFile eomFile, UUID uuid, String transactionId) {
    super(eomFile, uuid, transactionId);
  }
  
  @Override
  public void checkType() {
    if (!TYPE.equals(eomFile.getType())) {
      throw new UnsupportedTypeException(uuid, eomFile.getType());
    }
  }
  
  @Override
  protected void checkWorkflowStatus() throws XPathExpressionException {
    String channel = xpath.evaluate(CHANNEL_SYSTEM_ATTR_XPATH, systemAttributesDocument);
    if (isFinancialTimesChannel(channel)) {
      return;
    }
    
    String workflowStatus = eomFile.getWorkflowStatus();
    if (isBeforeWorkflowStatusEnforced(eomFile)
        && ALLOWED_WORKFLOWS_PRE_ENFORCE_DATE.contains(workflowStatus)) {
      return;
    }
    
    if (!isValidWorkflowStatusForWebPublication(workflowStatus)) {
      throw new WorkflowStatusNotEligibleForPublishException(uuid, workflowStatus);
    }
  }
  
  private boolean isBeforeWorkflowStatusEnforced(EomFile eomFile) {
    final String initialPublicationDateAsAString;
    try {
      initialPublicationDateAsAString = xpath.evaluate(EOM_STORY_INITIAL_PUBLISH_DATE_XPATH, attributesDocument);
    } catch (XPathExpressionException e) {
      throw new TransformationException(e);
    }
    
    if (!Strings.isNullOrEmpty(initialPublicationDateAsAString)) {
      Date initialPublicationDate = toDate(initialPublicationDateAsAString, METHODE_XML_DATE_TIME_FORMAT);
      return initialPublicationDate.before(WORKFLOW_STATUS_ENFORCE_DATE);
    }
    
    return false;
  }

  @Override
  protected void checkChannel() throws XPathExpressionException {
    String channel = xpath.evaluate(CHANNEL_SYSTEM_ATTR_XPATH, systemAttributesDocument);
    if (!(isWebChannel(channel) || isFinancialTimesChannel(channel))) {
      throw new NotWebChannelException(uuid);
    }
  }
  
  private boolean isFinancialTimesChannel(String channel) {
    return ("Financial Times".equals(channel));
  }
}
