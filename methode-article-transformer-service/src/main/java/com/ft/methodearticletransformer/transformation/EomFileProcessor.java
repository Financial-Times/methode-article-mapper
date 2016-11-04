package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.content.model.AlternativeTitles;
import com.ft.content.model.Brand;
import com.ft.content.model.Comments;
import com.ft.content.model.Content;
import com.ft.content.model.Identifier;
import com.ft.content.model.Standout;
import com.ft.content.model.Syndication;
import com.ft.methodearticletransformer.methode.UntransformableMethodeContentException;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.transformation.eligibility.PublishEligibilityChecker;
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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeSet;
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

public class EomFileProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EomFileProcessor.class);

    enum TransformationMode {
        PUBLISH,
        PREVIEW
    }

    enum ContributorRights {
        FIFTY_FIFTY_NEW("1"),
        FIFTY_FIFTY_OLD("2"),
        ALL_NO_PAYMENT("3"),
        CONTRACT("4"),
        NO_RIGHTS("5");

        private String value;

        ContributorRights(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        static ContributorRights fromString(String value) throws ContributorRightsException {
            if (value != null) {
                for (ContributorRights rights : ContributorRights.values()) {
                    if (value.equals(rights.getValue())) {
                        return rights;
                    }
                }
            }
            throw new ContributorRightsException("Unmatched type=" + value);
        }
    }

    public static final String METHODE = "http://api.ft.com/system/FTCOM-METHODE";
    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
    private static final Logger log = LoggerFactory.getLogger(EomFileProcessor.class);
    private static final String DEFAULT_IMAGE_ATTRIBUTE_DATA_EMBEDDED = "data-embedded";
    private static final String IMAGE_SET_TYPE = "http://www.ft.com/ontology/content/ImageSet";
    private static final String NO_PICTURE_FLAG = "No picture";
    private static final String HEADLINE_XPATH = "/doc/lead/lead-headline/headline/ln";
    private static final String ALT_TITLE_PROMO_TITLE_XPATH = "/doc/lead/web-index-headline/ln";
    private static final String STANDFIRST_XPATH = "/doc/lead/web-stand-first";
    private static final String BYLINE_XPATH = "/doc/story/text/byline";
    private static final String START_BODY = "<body";
    private static final String END_BODY = "</body>";
    private final FieldTransformer bodyTransformer;
    private final FieldTransformer bylineTransformer;
    private final Brand financialTimesBrand;

    public EomFileProcessor(FieldTransformer bodyTransformer, FieldTransformer bylineTransformer,
                            Brand financialTimesBrand) {
        this.bodyTransformer = bodyTransformer;
        this.bylineTransformer = bylineTransformer;
        this.financialTimesBrand = financialTimesBrand;
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

    public Content processPreview(EomFile eomFile, String transactionId) {
        UUID uuid = UUID.fromString(eomFile.getUuid());

        PublishEligibilityChecker eligibilityChecker =
            PublishEligibilityChecker.forEomFile(eomFile, uuid, transactionId);


        try {
          ParsedEomFile parsedEomFile = eligibilityChecker.getEligibleContentForPreview();

          return transformEomFileToContent(uuid, parsedEomFile, TransformationMode.PREVIEW, transactionId);
        } catch (ParserConfigurationException | SAXException | XPathExpressionException | TransformerException | IOException e) {
            throw new TransformationException(e);
        }
    }

    public Content processPublication(EomFile eomFile, String transactionId) {
        UUID uuid = UUID.fromString(eomFile.getUuid());

        try {
            ParsedEomFile parsedEomFile = getEligibleContentForPublishing(eomFile, uuid, transactionId);

            return transformEomFileToContent(uuid, parsedEomFile, TransformationMode.PUBLISH, transactionId);
        } catch (ParserConfigurationException | SAXException | XPathExpressionException | TransformerException | IOException e) {
            throw new TransformationException(e);
        }
    }

    private ParsedEomFile getEligibleContentForPublishing(EomFile eomFile, UUID uuid, String transactionId)
        throws SAXException, XPathExpressionException,
               ParserConfigurationException, TransformerException, IOException {

        PublishEligibilityChecker eligibilityChecker =
          PublishEligibilityChecker.forEomFile(eomFile, uuid, transactionId);

        return eligibilityChecker.getEligibleContentForPublishing();
    }
    
    private Content transformEomFileToContent(UUID uuid, ParsedEomFile eomFile, TransformationMode mode, String transactionId)
            throws SAXException, IOException, XPathExpressionException, TransformerException, ParserConfigurationException {

        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Document doc = eomFile.getValue();

        final String headline = Strings.nullToEmpty(xpath.evaluate(HEADLINE_XPATH, doc)).trim();
        final AlternativeTitles altTitles = buildAlternativeTitles(doc, xpath);

        final String lastPublicationDateAsString = xpath
                .evaluate("/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomLastPublication", eomFile.getAttributes());

        final boolean discussionEnabled = isDiscussionEnabled(xpath, eomFile.getAttributes());

        String standfirst = Strings.nullToEmpty(xpath.evaluate(STANDFIRST_XPATH, doc)).trim();

        String transformedBody = transformField(eomFile.getBody(), bodyTransformer, transactionId);
        switch (mode) {
          case PREVIEW:
            if (Strings.isNullOrEmpty(transformedBody)) {
              transformedBody = "<body></body>";
            }
            break;
          default:
            if (Strings.isNullOrEmpty(unwrapBody(transformedBody))) {
              throw new UntransformableMethodeContentException(uuid.toString(), "Not a valid Methode article for publication - transformed article body is blank");
            }
        }

        final String mainImage = generateMainImageUuid(xpath, eomFile.getValue());
        String postProcessedBody = putMainImageReferenceInBodyXml(xpath, eomFile.getAttributes(), mainImage, transformedBody);

        final String transformedByline = transformField(retrieveField(xpath, BYLINE_XPATH, eomFile.getValue()),
                bylineTransformer, transactionId); //byline is optional

        final Syndication canBeSyndicated = getSyndication(xpath, eomFile.getAttributes());

        return Content.builder()
                .withUuid(uuid)
                .withTitle(headline)
                .withAlternativeTitles(altTitles)
                .withStandfirst(standfirst)
                .withXmlBody(postProcessedBody)
                .withByline(transformedByline)
                .withMainImage(mainImage)
                .withBrands(new TreeSet<>(Collections.singletonList(financialTimesBrand)))
                .withPublishedDate(toDate(lastPublicationDateAsString, DATE_TIME_FORMAT))
                .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
                .withComments(Comments.builder().withEnabled(discussionEnabled).build())
                .withStandout(buildStandoutSection(xpath, eomFile.getAttributes()))
                .withWebUrl(eomFile.getWebUrl())
                .withPublishReference(transactionId)
                .withLastModified(eomFile.getLastModified())
                .withCanBeSyndicated(canBeSyndicated)
                .build();
    }

    private Syndication getSyndication(final XPath xpath, final Document attributesDocument) throws XPathExpressionException {
        String cmsContributorRights = xpath.evaluate("/ObjectMetadata/EditorialNotes/CCMS/CCMSContributorRights", attributesDocument);

        if (cmsContributorRights.isEmpty()) {
            return Syndication.VERIFY;
        }

        ContributorRights contributorRights;
        try {
             contributorRights = ContributorRights.fromString(cmsContributorRights);
        } catch (ContributorRightsException e) {
            LOGGER.warn("Found invalid CCMSContributorRights={} in article", cmsContributorRights, e);
            return Syndication.VERIFY;
        }

        switch (contributorRights) {
            case ALL_NO_PAYMENT:
                return Syndication.YES;
            case NO_RIGHTS:
                return Syndication.NO;
            default:
                return Syndication.VERIFY;
        }
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

    private String unwrapBody(String wrappedBody) {
        if (!(wrappedBody.startsWith(START_BODY) && wrappedBody.endsWith(END_BODY))) {
          throw new IllegalArgumentException("can't unwrap a string that is not a wrapped body");
        }

        int index = wrappedBody.indexOf('>', START_BODY.length()) + 1;
        return wrappedBody.substring(index, wrappedBody.length() - END_BODY.length()).trim();
      }

    private AlternativeTitles buildAlternativeTitles(Document doc, XPath xpath)
        throws XPathExpressionException {

        AlternativeTitles.Builder builder = AlternativeTitles.builder();

        String promotionalTitle = Strings.nullToEmpty(xpath.evaluate(ALT_TITLE_PROMO_TITLE_XPATH, doc)).trim();
      if (!promotionalTitle.isEmpty()) {
          builder = builder.withPromotionalTitle(promotionalTitle);
      }

        return builder.build();
    }
}
