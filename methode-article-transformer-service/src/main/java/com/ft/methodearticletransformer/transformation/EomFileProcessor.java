package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.content.model.Brand;
import com.ft.content.model.Comments;
import com.ft.content.model.Content;
import com.ft.content.model.Identifier;
import com.ft.content.model.Standout;
import com.ft.methodearticletransformer.methode.EmbargoDateInTheFutureException;
import com.ft.methodearticletransformer.methode.MethodeMarkedDeletedException;
import com.ft.methodearticletransformer.methode.MethodeMissingBodyException;
import com.ft.methodearticletransformer.methode.MethodeMissingFieldException;
import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.SourceNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.SupportedTypeResolver;
import com.ft.methodearticletransformer.methode.UnsupportedTypeException;
import com.ft.methodearticletransformer.methode.UntransformableMethodeContentException;
import com.ft.methodearticletransformer.methode.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.util.ImageSetUuidGenerator;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedSet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.UUID;

public class EomFileProcessor {

    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";

    private static final Logger log = LoggerFactory.getLogger(EomFileProcessor.class);
    public static final String METHODE = "http://api.ft.com/system/FTCOM-METHODE";
    private static final String DEFAULT_IMAGE_ATTRIBUTE_DATA_EMBEDDED = "data-embedded";
    private static final String IMAGE_SET_TYPE = "http://www.ft.com/ontology/content/ImageSet";
    private static final String NO_PICTURE_FLAG = "No picture";

    private static final String HEADLINE_XPATH = "/doc/lead/lead-headline/headline/ln";
    private static final String BYLINE_XPATH = "/doc/story/text/byline";
    private static final String BODY_TAG_XPATH = "/doc/story/text/body";
    private static final String START_BODY = "<body";
    private static final String END_BODY = "</body>";
    private static final String EOM_STORY_INITIAL_PUBLISH_DATE_XPATH = "/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomInitialPublication";
    private static final String EOM_STORY_TYPE ="EOM::Story";
    private static final String WORKFLOW_STATUS_ENFORCE_DATE_AS_STRING ="20110601000000";
    
   private static final List<String> allowedWorkflowsPreEnforeceDate=Arrays.asList("",EomFile.WEB_READY, EomFile.WEB_REVISE, "FTContentMove/Ready", "FTContentMove/Editing_Methode", "FTContentMove/Revise", "FTContentMove/Subbing_Methode", "FTContentMove/Released", "Stories/Ready", "Stories/Released", "Stories/Revise", "Stories/Sub");

    private final FieldTransformer bodyTransformer;
    private final FieldTransformer bylineTransformer;
    private final Brand financialTimesBrand;

    public EomFileProcessor(FieldTransformer bodyTransformer, FieldTransformer bylineTransformer,
                            Brand financialTimesBrand) {
        this.bodyTransformer = bodyTransformer;
        this.bylineTransformer = bylineTransformer;
        this.financialTimesBrand = financialTimesBrand;
    }

    public Content processPreview(EomFile eomFile, String transactionId) {
        UUID uuid = UUID.fromString(eomFile.getUuid());

        if (!new SupportedTypeResolver(eomFile.getType()).isASupportedType()) {
            throw new UnsupportedTypeException(uuid, eomFile.getType());
        }

        try {
            final DocumentBuilder documentBuilder = getDocumentBuilder();
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final Document attributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getAttributes())));
            final Document eomFileDocument = documentBuilder.parse(new ByteArrayInputStream(eomFile.getValue()));
            final String headline = xpath.evaluate(HEADLINE_XPATH, eomFileDocument);
            final String transformedByline = transformField(retrieveField(xpath, BYLINE_XPATH, eomFileDocument),
                    bylineTransformer, transactionId); //byline is optional

            //body transformation
            String postProcessedBody = "<body></body>";
            String transformedBody = postProcessedBody;

            String rawBody = retrieveField(xpath, BODY_TAG_XPATH, eomFileDocument);
            if (!Strings.isNullOrEmpty(rawBody)) {
                transformedBody = transformField(rawBody, bodyTransformer, transactionId);
            }
            final String mainImage = generateMainImageUuid(xpath, eomFileDocument);
            postProcessedBody = putMainImageReferenceInBodyXml(xpath, attributesDocument, mainImage, transformedBody);

            return Content.builder()
                    .withUuid(uuid)
                    .withTitle(headline)
                    .withXmlBody(postProcessedBody)
                    .withByline(transformedByline)
                    .withMainImage(mainImage)
                    .withBrands(new TreeSet<>(Collections.singletonList(financialTimesBrand)))
                    .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
                    .withComments(Comments.builder().withEnabled(isDiscussionEnabled(xpath, attributesDocument)).build())
                    .withPublishReference(transactionId)
                    .withLastModified(eomFile.getLastModified())
                    .build();

        } catch (ParserConfigurationException | SAXException | XPathExpressionException | TransformerException | IOException e) {
            throw new TransformationException(e);
        }
    }

    public Content processPublication(EomFile eomFile, String transactionId) {
        UUID uuid = UUID.fromString(eomFile.getUuid());

        if (!new SupportedTypeResolver(eomFile.getType()).isASupportedType()) {
            throw new UnsupportedTypeException(uuid, eomFile.getType());
        }

		if (!workflowStatusEligibleForPublishing(eomFile)) {
			    throw new WorkflowStatusNotEligibleForPublishException(uuid, eomFile.getWorkflowStatus());
		}
	
        try {
            return transformEomFileToContent(uuid, eomFile, transactionId);
        } catch (ParserConfigurationException | SAXException | XPathExpressionException | TransformerException | IOException e) {
            throw new TransformationException(e);
        }
    }

    private Content transformEomFileToContent(UUID uuid, EomFile eomFile, String transactionId)
            throws SAXException, IOException, XPathExpressionException, TransformerException, ParserConfigurationException {

        final DocumentBuilder documentBuilder = getDocumentBuilder();
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Document attributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getAttributes())));
        verifyEmbargoDate(xpath, attributesDocument, uuid);
        verifySource(uuid, xpath, attributesDocument);
        verifyNotMarkedDeleted(uuid, xpath, attributesDocument);

        final Document systemAttributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getSystemAttributes())));

        verifyChannel(uuid, xpath, systemAttributesDocument);

        final Document eomFileDocument = documentBuilder.parse(new ByteArrayInputStream(eomFile.getValue()));

        final String headline = xpath.evaluate(HEADLINE_XPATH, eomFileDocument);

        final String lastPublicationDateAsString = xpath
                .evaluate("/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomLastPublication", attributesDocument);

        final boolean discussionEnabled = isDiscussionEnabled(xpath, attributesDocument);

        final String mainImage = generateMainImageUuid(xpath, eomFileDocument);

        verifyLastPublicationDatePresent(uuid, lastPublicationDateAsString);

        String rawBody = retrieveField(xpath, BODY_TAG_XPATH, eomFileDocument);
        verifyBodyPresent(uuid, rawBody);
        String transformedBody = transformField(rawBody, bodyTransformer, transactionId);
        
        if (Strings.isNullOrEmpty(unwrapBody(transformedBody))) {
            throw new UntransformableMethodeContentException(uuid.toString(), "Not a valid Methode article for publication - transformed article body is blank");
          }
        

        String postProcessedBody = putMainImageReferenceInBodyXml(xpath, attributesDocument, mainImage, transformedBody);

        final String transformedByline = transformField(retrieveField(xpath, BYLINE_XPATH, eomFileDocument),
                bylineTransformer, transactionId); //byline is optional

        return Content.builder()
                .withUuid(uuid)
                .withTitle(headline)
                .withXmlBody(postProcessedBody)
                .withByline(transformedByline)
                .withMainImage(mainImage)
                .withBrands(new TreeSet<>(Collections.singletonList(financialTimesBrand)))
                .withPublishedDate(toDate(lastPublicationDateAsString, DATE_TIME_FORMAT))
                .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
                .withComments(Comments.builder().withEnabled(discussionEnabled).build())
                .withStandout(buildStandoutSection(xpath, attributesDocument))
                .withPublishReference(transactionId)
                .withLastModified(eomFile.getLastModified())
                .build();
    }

    private Standout buildStandoutSection(final XPath xpath, final Document attributesDocument) throws XPathExpressionException {
        boolean editorsChoice = xpath.evaluate("/ObjectMetadata/OutputChannels/DIFTcom/editorsPick", attributesDocument).toLowerCase().equals("yes");
        boolean exclusive = xpath.evaluate("/ObjectMetadata/OutputChannels/DIFTcom/exclusive", attributesDocument).toLowerCase().equals("yes");
        boolean scoop = xpath.evaluate("/ObjectMetadata/OutputChannels/DIFTcom/scoop", attributesDocument).toLowerCase().equals("yes");
        return new Standout(editorsChoice, exclusive, scoop);
    }

    private boolean isDiscussionEnabled(final XPath xpath, final Document attributesDocument) throws XPathExpressionException {
        return Boolean.valueOf(xpath.evaluate("/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomDiscussion", attributesDocument).toLowerCase());
    }

    private String putMainImageReferenceInBodyXml(XPath xpath, Document attributesDocument, String mainImage, String body) throws XPathExpressionException,
            TransformerException, ParserConfigurationException, SAXException, IOException {

        if (mainImage != null) {

            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(body));

            Element bodyNode = getDocumentBuilder()
                    .parse(inputSource)
                    .getDocumentElement();
            final String flag = xpath.evaluate("/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomArticleImage", attributesDocument);
            if (!NO_PICTURE_FLAG.equalsIgnoreCase(flag)) {
                return putMainImageReferenceInBodyNode(bodyNode, mainImage);
            }
        }
        return body;
    }

    private String putMainImageReferenceInBodyNode(Node bodyNode, String mainImage) throws TransformerException {
        Element newElement = bodyNode.getOwnerDocument().createElement("content");
        newElement.setAttribute("id", mainImage);
        newElement.setAttribute("type", IMAGE_SET_TYPE);
        newElement.setAttribute(DEFAULT_IMAGE_ATTRIBUTE_DATA_EMBEDDED, "true");
        bodyNode.insertBefore(newElement, bodyNode.getFirstChild());
        return getNodeAsHTML5String(bodyNode);
    }

    private String generateMainImageUuid(XPath xpath, Document eomFileDocument) throws XPathExpressionException {
        final String imageUuid = StringUtils.substringAfter(xpath.evaluate("/doc/lead/lead-images/web-master/@fileref", eomFileDocument), "uuid=");
        if (!Strings.isNullOrEmpty(imageUuid)) {
            return ImageSetUuidGenerator.fromImageUuid(UUID.fromString(imageUuid)).toString();
        }
        return null;
    }

    private void verifyLastPublicationDatePresent(UUID uuid, String lastPublicationDateAsString) {
        if (Strings.isNullOrEmpty(lastPublicationDateAsString)) {
            throw new MethodeMissingFieldException(uuid, "publishedDate");
        }
    }

	private boolean workflowStatusEligibleForPublishing(EomFile eomFile) {
		String workflowStatus = eomFile.getWorkflowStatus();
		if (EOM_STORY_TYPE.equals(eomFile.getType()) && isBeforeWorkflowStatusEnforced(eomFile)) {
			return isWorkflowStatusEligiblePreEnforceDate(workflowStatus);
		}
		return EomFile.WEB_REVISE.equals(workflowStatus) || EomFile.WEB_READY.equals(workflowStatus);
	}

	private boolean isWorkflowStatusEligiblePreEnforceDate(String workflowStatus) {
		if (allowedWorkflowsPreEnforeceDate.contains(workflowStatus)) {
			return true;
		}
		return false;
	}

	private void verifySource(UUID uuid, XPath xpath, Document attributesDocument) throws XPathExpressionException {
        final String sourceCode = xpath.evaluate("/ObjectMetadata//EditorialNotes/Sources/Source/SourceCode", attributesDocument);
        if (!"FT".equals(sourceCode)) {
            throw new SourceNotEligibleForPublishException(uuid, sourceCode);
        }
    }

    private void verifyChannel(UUID uuid, XPath xpath, Document systemAttributesDocument) throws SAXException, IOException, XPathExpressionException {
        final String channel = xpath.evaluate("/props/productInfo/name", systemAttributesDocument);
        if (notWebChannel(channel)) {
            throw new NotWebChannelException(uuid);
        }
    }

    private void verifyNotMarkedDeleted(UUID uuid, XPath xpath, Document attributesDocument) throws XPathExpressionException {
        final String markedDeletedString = xpath.evaluate("/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomMarkDeleted", attributesDocument);
        if (!Strings.isNullOrEmpty(markedDeletedString) && markedDeletedString.equals("True")) {
            throw new MethodeMarkedDeletedException(uuid);
        }
    }

    private void verifyBodyPresent(UUID uuid, String body) {
        if (Strings.isNullOrEmpty(body)) {
            throw new MethodeMissingBodyException(uuid);
        }
    }

    private boolean notWebChannel(String channel) {
        return !EomFile.WEB_CHANNEL.equals(channel);
    }

    private String retrieveField(XPath xpath, String expression, Document eomFileDocument) throws TransformerException, XPathExpressionException {
        final Node node = (Node) xpath.evaluate(expression, eomFileDocument, XPathConstants.NODE);
        return getNodeAsString(node);
    }

    private String transformField(String originalFieldAsString, FieldTransformer transformer, String transactionId) {
        String transformedField = "";
        if (!Strings.isNullOrEmpty(originalFieldAsString)) {
            transformedField = transformer.transform(originalFieldAsString, transactionId);
        }
        return transformedField;
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        return documentBuilderFactory.newDocumentBuilder();
    }

    private String getNodeAsString(Node node) throws TransformerException {
        return convertNodeToStringReturningEmptyIfNull(node);
    }


    private String getNodeAsHTML5String(Node node) throws TransformerException {
        Html5SelfClosingTagBodyProcessor processor = new Html5SelfClosingTagBodyProcessor();
        String nodeAsString = convertNodeToStringReturningEmptyIfNull(node);
        return processor.process(nodeAsString, null);
    }

    private String convertNodeToStringReturningEmptyIfNull(Node node) throws TransformerException {
        StringWriter writer = new StringWriter();
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

    private static Date toDate(String dateString, String format) {
        if (dateString == null || dateString.equals("")) {
            return null;
        }
        try {
            DateFormat dateFormat = new SimpleDateFormat(format);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            log.warn("Error parsing date " + dateString, e);
            return null;
        }
    }

    private void verifyEmbargoDate(XPath xpath, Document attributesDocument, UUID uuid) throws XPathExpressionException {
        final String embargoDateAsAString = xpath
                .evaluate("/ObjectMetadata/EditorialNotes/EmbargoDate", attributesDocument);
        if (!Strings.isNullOrEmpty(embargoDateAsAString)) {
            Date embargoDate = toDate(embargoDateAsAString, DATE_TIME_FORMAT);
            if (embargoDate.after(new Date())) {
                throw new EmbargoDateInTheFutureException(uuid, embargoDate);
            }
        }
    }
    
        
	private boolean isBeforeWorkflowStatusEnforced(EomFile eomFile) {
		final String initialPublicationDateAsAString;
		final Date workFlowStatusEnforceDate;
		try {
			final DocumentBuilder documentBuilder = getDocumentBuilder();
			final XPath xpath = XPathFactory.newInstance().newXPath();
			final Document attributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getAttributes())));
			workFlowStatusEnforceDate= toDate(WORKFLOW_STATUS_ENFORCE_DATE_AS_STRING, DATE_TIME_FORMAT);
			initialPublicationDateAsAString = xpath.evaluate(EOM_STORY_INITIAL_PUBLISH_DATE_XPATH,attributesDocument);
		} catch (ParserConfigurationException | SAXException | XPathExpressionException | IOException e) {
			throw new TransformationException(e);
		}
			if (!Strings.isNullOrEmpty(initialPublicationDateAsAString)) {
				Date initialPublicationDate = toDate(initialPublicationDateAsAString, DATE_TIME_FORMAT);
				if (initialPublicationDate.before(workFlowStatusEnforceDate)) {
					return true;
				}
			}
		
		return false;
	}
    
    
    private String unwrapBody(String wrappedBody) {
        if (!(wrappedBody.startsWith(START_BODY) && wrappedBody.endsWith(END_BODY))) {
          throw new IllegalArgumentException("can't unwrap a string that is not a wrapped body");
        }
        
        int index = wrappedBody.indexOf('>', START_BODY.length()) + 1;
        return wrappedBody.substring(index, wrappedBody.length() - END_BODY.length()).trim();
      }
}
