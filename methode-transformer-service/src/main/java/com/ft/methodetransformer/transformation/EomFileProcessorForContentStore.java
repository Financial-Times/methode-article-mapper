package com.ft.methodetransformer.transformation;

import static com.ft.methodetransformer.methode.EomFileType.EOMCompoundStory;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ft.content.model.Content;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodetransformer.methode.MethodeContentNotEligibleForPublishException;
import com.ft.methodetransformer.methode.SupportedTypeResolver;
import com.google.common.base.Strings;

public class EomFileProcessorForContentStore {

	private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";

	private static final Logger log = LoggerFactory.getLogger(EomFileProcessorForContentStore.class);

	private final FieldTransformer bodyTransformer;
	private final FieldTransformer bylineTransformer;

    public EomFileProcessorForContentStore(FieldTransformer bodyTransformer, FieldTransformer bylineTransformer) {
        this.bodyTransformer = bodyTransformer;
        this.bylineTransformer = bylineTransformer;
    }

	public Content process(EomFile eomFile, String transactionId) {
		UUID uuid = UUID.fromString(eomFile.getUuid());

        if(!new SupportedTypeResolver(eomFile.getType()).isASupportedType()) {
            throw new MethodeContentNotEligibleForPublishException(uuid, "not an " + EOMCompoundStory.getTypeName());
        }
		
		try {
            final DocumentBuilder documentBuilder = getDocumentBuilder();

			final XPath xpath = XPathFactory.newInstance().newXPath();
			
            Content content = transformEomFileToContent(uuid, eomFile, documentBuilder, xpath, transactionId);
            return content;       	     
			
		} catch (ParserConfigurationException | SAXException | XPathExpressionException | TransformerException | IOException e) {
            throw new TransformationException(e);
        } 	
	}

	private Content transformEomFileToContent(UUID uuid, EomFile eomFile, DocumentBuilder documentBuilder, XPath xpath,
											  String transactionId)
			throws SAXException, IOException, XPathExpressionException, TransformerException {

        final Document eomFileDocument = documentBuilder.parse(new ByteArrayInputStream(eomFile.getValue()));

        final String headline = xpath
                .evaluate("/doc/lead/lead-headline/headline/ln", eomFileDocument);

        final String attributes = eomFile.getAttributes();
        final Document attributesDocument = documentBuilder.parse(new InputSource(new StringReader(attributes)));

        final String lastPublicationDateAsString = xpath
                .evaluate("/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomLastPublication", attributesDocument);
        
        final String body = retrieveField(xpath, "/doc/story/text/body", uuid, "body", eomFileDocument);
        
        final String transformedBody = transformField(body,	bodyTransformer, transactionId);

        final String transformedByline = transformField(retrieveField(xpath, "/doc/story/text/byline", uuid, "byline", eomFileDocument),
				bylineTransformer, transactionId); //byline is optional

        return Content.builder()
                .withUuid(uuid)
                .withHeadline(headline)
                .withSource("methode")
                .withXmlBody(transformedBody)
                .withByline(transformedByline)
				.withLastPublicationDate(toDate(lastPublicationDateAsString, DATE_TIME_FORMAT))
                .build();

	}
	
	private String retrieveField(XPath xpath, String expression, UUID uuid, String fieldName, Document eomFileDocument) throws TransformerException, XPathExpressionException {
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
		// if node is null, this returns ""
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

}
