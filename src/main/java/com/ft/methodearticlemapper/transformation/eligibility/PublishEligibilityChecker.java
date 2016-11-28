package com.ft.methodearticlemapper.transformation.eligibility;

import com.google.common.base.Strings;

import com.ft.methodearticlemapper.exception.EmbargoDateInTheFutureException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.SourceNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.UnsupportedObjectTypeException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.ParsedEomFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public abstract class PublishEligibilityChecker {

  protected static final String METHODE_XML_DATE_TIME_FORMAT = "yyyyMMddHHmmss";
  
  protected static final String CHANNEL_SYSTEM_ATTR_XPATH = "/props/productInfo/name";
  protected static final String SOURCE_ATTR_XPATH =
      "/ObjectMetadata//EditorialNotes/Sources/Source/SourceCode";
  
  private static final Logger LOG = LoggerFactory.getLogger(PublishEligibilityChecker.class);
  
  private static final String BODY_TAG_XPATH = "/doc/story/text/body";
  
  private static final String MARKED_DELETED_ATTR_XPATH =
      "/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomMarkDeleted";
  private static final String EMBARGO_ATTR_XPATH = "/ObjectMetadata/EditorialNotes/EmbargoDate";
  private static final String LAST_PUB_DATE_ATTR_XPATH =
      "/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomLastPublication";
  private static final String OBJECT_LOCATION_ATTR_XPATH = "//ObjectMetadata/EditorialNotes/ObjectLocation";

  protected final XPath xpath = XPathFactory.newInstance().newXPath();
  protected final EomFile eomFile;
  protected final UUID uuid;
  protected final String transactionId;
  protected String rawBody;
  protected Document attributesDocument;
  protected Document systemAttributesDocument;

  public PublishEligibilityChecker(EomFile eomFile, UUID uuid, String transactionId) {
    this.eomFile = eomFile;
    this.uuid = uuid;
    this.transactionId = transactionId;
  }
  
  protected static Date toDate(String dateString, String format) {
    if (dateString == null || dateString.equals("")) {
      return null;
    }

    try {
      DateFormat dateFormat = new SimpleDateFormat(format);
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      return dateFormat.parse(dateString);
    } catch (ParseException e) {
      LOG.warn("Error parsing date " + dateString, e);
      return null;
    }
  }
  
  public static PublishEligibilityChecker forEomFile(EomFile eomFile, UUID uuid, String transactionId) {
    String type = eomFile.getType();

    if (EOMStoryPublishEligibilityChecker.TYPE.equals(type)) {
      return new EOMStoryPublishEligibilityChecker(eomFile, uuid, transactionId);
    }

    return new EOMCompoundStoryPublishEligibilityChecker(eomFile, uuid, transactionId);
  }

  private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
      final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

      return documentBuilderFactory.newDocumentBuilder();
  }

  private String retrieveField(XPath xpath, String expression, Document eomFileDocument) throws TransformerException, XPathExpressionException {
      final Node node = (Node) xpath.evaluate(expression, eomFileDocument, XPathConstants.NODE);
      return getNodeAsString(node);
  }

  private String getNodeAsString(Node node) throws TransformerException {
      return convertNodeToStringReturningEmptyIfNull(node);
  }

  private String convertNodeToStringReturningEmptyIfNull(Node node) throws TransformerException {
      StringWriter writer = new StringWriter();
      final TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(node), new StreamResult(writer));
      return writer.toString();
  }
  
  public final ParsedEomFile getEligibleContentForPublishing()
      throws ParserConfigurationException, SAXException, IOException,
             XPathExpressionException, TransformerException {
    
    final Document eomFileDocument;
    
    try {
      final DocumentBuilder documentBuilder = getDocumentBuilder();
      
      attributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getAttributes())));
      systemAttributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getSystemAttributes())));
      eomFileDocument = documentBuilder.parse(new ByteArrayInputStream(eomFile.getValue()));
      rawBody = retrieveField(xpath, BODY_TAG_XPATH, eomFileDocument);
    } finally {
      checkEomType();
      checkWorkflowStatus();
    }

    checkNotEmbargoed();
    checkSource();
    checkObjectType();
    checkNotDeleted();
    checkChannel();
    checkPublicationDate();
    checkBody();
    
    ParsedEomFile parsedEomFile = new ParsedEomFile(uuid, eomFileDocument, rawBody,
            attributesDocument, eomFile.getWebUrl());
    
    return parsedEomFile;
  }

  public final ParsedEomFile getEligibleContentForPreview()
      throws ParserConfigurationException, SAXException, IOException,
             XPathExpressionException, TransformerException {

    final Document eomFileDocument;

    try {
      final DocumentBuilder documentBuilder = getDocumentBuilder();

      attributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getAttributes())));
      eomFileDocument = documentBuilder.parse(new ByteArrayInputStream(eomFile.getValue()));
      rawBody = retrieveField(xpath, BODY_TAG_XPATH, eomFileDocument);
    } finally {
      checkEomType();
    }

    ParsedEomFile parsedEomFile = new ParsedEomFile(uuid, eomFileDocument, rawBody,
            attributesDocument, eomFile.getWebUrl());

    return parsedEomFile;
  }

  public abstract void checkEomType();

  protected abstract void checkWorkflowStatus() throws XPathExpressionException;

  protected final void checkObjectType() throws XPathExpressionException {
    String fileType = xpath.evaluate(OBJECT_LOCATION_ATTR_XPATH, attributesDocument);
    if (Strings.isNullOrEmpty(fileType) || !fileType.toLowerCase().endsWith(".xml")) {
      throw new UnsupportedObjectTypeException(uuid, fileType);
    }
  }

  protected final void checkNotEmbargoed()
      throws XPathExpressionException {
    
    String embargoDateAsAString = xpath.evaluate(EMBARGO_ATTR_XPATH, attributesDocument);
    if (!Strings.isNullOrEmpty(embargoDateAsAString)) {
      Date embargoDate = toDate(embargoDateAsAString, METHODE_XML_DATE_TIME_FORMAT);
      if (embargoDate.after(new Date())) {
        throw new EmbargoDateInTheFutureException(uuid, embargoDate);
      }
    }
  }

  protected void checkSource() throws XPathExpressionException {
    String sourceCode = xpath.evaluate(SOURCE_ATTR_XPATH, attributesDocument);
    if (!isFTSource(sourceCode)) {
      throw new SourceNotEligibleForPublishException(uuid, sourceCode);
    }
  }

  protected final void checkNotDeleted()
      throws XPathExpressionException {
    
    String markedDeletedString = xpath.evaluate(MARKED_DELETED_ATTR_XPATH, attributesDocument);
    if (Boolean.parseBoolean(markedDeletedString)) {
      throw new MethodeMarkedDeletedException(uuid);
    }
  }
  
  protected abstract void checkChannel() throws XPathExpressionException;
  
  protected final void checkPublicationDate()
      throws XPathExpressionException {
    
    String lastPublicationDateAsString = xpath.evaluate(LAST_PUB_DATE_ATTR_XPATH, attributesDocument);
    if (Strings.isNullOrEmpty(lastPublicationDateAsString)) {
      throw new MethodeMissingFieldException(uuid, "publishedDate");
    }
  }

  protected final void checkBody() {
    if (Strings.isNullOrEmpty(rawBody)) {
      throw new MethodeMissingBodyException(uuid);
    }
  }

  protected final boolean isValidWorkflowStatusForWebPublication(String workflowStatus) {
    return EomFile.WEB_REVISE.equals(workflowStatus) || EomFile.WEB_READY.equals(workflowStatus);
  }
  
  protected final boolean isFTSource(String source) {
    return "FT".equals(source);
  }
  
  protected final boolean isWebChannel(String channel) {
      return EomFile.WEB_CHANNEL.equals(channel);
  }
}
