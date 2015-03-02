package com.ft.methodearticletransformer.transformation;

import static com.ft.methodearticletransformer.transformation.MethodeLinksBodyProcessor.AssetCharacter.existsInContentStore;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.TransactionIdBodyProcessingContext;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodearticletransformer.methode.SemanticReaderUnavailableException;
import com.ft.methodearticletransformer.methode.SupportedTypeResolver;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class MethodeLinksBodyProcessor implements BodyProcessor {

	private static final String CONTENT_TAG = "content";
	public static final String ARTICLE_TYPE = "http://www.ft.com/ontology/content/Article";
    private static final String UUID_REGEX = ".*([0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}).*";
    private static final Pattern REGEX_PATTERN = Pattern.compile(UUID_REGEX);

	private static final String ANCHOR_PREFIX = "#";
    public static final String FT_COM_WWW_URL = "http://www.ft.com/";
	public static final String TYPE = "type";

	private final com.ft.methodearticletransformer.methode.MethodeFileService methodeFileService;
	private ResilientClient semanticStoreContentReaderClient;

	public MethodeLinksBodyProcessor(com.ft.methodearticletransformer.methode.MethodeFileService methodeFileService, ResilientClient semanticStoreContentReaderClient) {
        this.methodeFileService = methodeFileService;
		this.semanticStoreContentReaderClient = semanticStoreContentReaderClient;
	}

    @Override
    public String process(String body, BodyProcessingContext bodyProcessingContext) throws BodyProcessingException {
        if (body != null && !body.trim().isEmpty()) {

            final Document document;
            try {
                final DocumentBuilder documentBuilder = getDocumentBuilder();
                document = documentBuilder.parse(new InputSource(new StringReader(body)));
            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new BodyProcessingException(e);
            }

            final List<Node> aTagsToCheck = new ArrayList<>();
            final XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                final NodeList aTags = (NodeList) xpath.evaluate("//a", document, XPathConstants.NODESET);
                for (int i = 0; i < aTags.getLength(); i++) {
                    final Node aTag = aTags.item(i);
                    final String href = getHref(aTag);
                    if (isRemovable(href)) {
                    	removeATag(aTag);
                    } else {
                        if (shouldCheckTypeWithMethode(href)) {
                            aTagsToCheck.add(aTag);
                        } 
                    }
                }
            } catch (XPathExpressionException e) {
                throw new BodyProcessingException(e);
            }

            final Set<String> idsToCheck = extractIds(aTagsToCheck);

			if (bodyProcessingContext instanceof TransactionIdBodyProcessingContext) {
				TransactionIdBodyProcessingContext transactionIdBodyProcessingContext =
						(TransactionIdBodyProcessingContext) bodyProcessingContext;
				final Map<String, AssetCharacter> assetTypes = getAssetTypes(idsToCheck,
						transactionIdBodyProcessingContext.getTransactionId());

				processATags(aTagsToCheck, assetTypes);

				final String modifiedBody = serializeBody(document);
				return modifiedBody;
			} else {
				IllegalStateException up = new IllegalStateException("bodyProcessingContext should provide transaction id.");
				throw up;
			}
        }
        return body;
    }

    /**
     * We remove blank hrefs (which are either invalid, or refer to the current document)
     * and hrefs that contain only a fragment identifier (a part of the current document).
     * @param href
     * @return true if removable, otherwise false.
     */
	private boolean isRemovable(final String href) {

        if(isNullOrEmpty(href)) {
            return true;
        }

		return href.startsWith(ANCHOR_PREFIX);
	}

	private Map<String, AssetCharacter> getAssetTypes(Set<String> idsToCheck, String transactionId) {
        if(idsToCheck.isEmpty()) {
            return Collections.emptyMap();
        }

		Map<String, AssetCharacter> assetCharacterMap = new HashMap<>();

		Set<String> idsToCheckStill = new HashSet<>();
		for (String idToCheck: idsToCheck) {
			if (doesNotExistInSemanticStore(idToCheck, transactionId)) {
				idsToCheckStill.add(idToCheck);
			} else {
				assetCharacterMap.put(idToCheck, existsInContentStore(idToCheck));
			}
		}

		if (idsToCheckStill.isEmpty()) {
			return assetCharacterMap;
		}

		Map<String, EomAssetType> eomAssetTypes = methodeFileService.assetTypes(new HashSet<>(idsToCheckStill), transactionId);
        for (EomAssetType eomAssetType: eomAssetTypes.values()) {
			assetCharacterMap.put(eomAssetType.getUuid(), new AssetCharacter(eomAssetType));
		}

		return assetCharacterMap;
    }

	private boolean doesNotExistInSemanticStore(String idToCheck, String transactionId) {
		ClientResponse clientResponse;
		URI contentUrl = contentUrlBuilder().build(idToCheck);
		try {
			clientResponse = semanticStoreContentReaderClient.resource(contentUrl)
					.accept(MediaType.APPLICATION_JSON_TYPE)
					.header(TransactionIdUtils.TRANSACTION_ID_HEADER, transactionId)
					.get(ClientResponse.class);
		} catch (ClientHandlerException che) {
			Throwable cause = che.getCause();
			if(cause instanceof IOException) {
				throw new SemanticReaderUnavailableException(che);
			}
			throw che;
		}

		int responseStatusCode = clientResponse.getStatus();
		int responseStatusFamily = responseStatusCode / 100;

        try {
            clientResponse.getEntityInputStream().close(); // So that the connection does not stay open.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		return responseStatusFamily != 2;
	}

	private UriBuilder contentUrlBuilder() {
		return UriBuilder.fromPath("content")
				.path("{uuid}")
				.scheme("http")
				.host(semanticStoreContentReaderClient.getDefaultHost())
				.port(semanticStoreContentReaderClient.getDefaultPort());
	}

	private String serializeBody(Document document) {
//        final DOMImplementationLS implementation = (DOMImplementationLS) document.getImplementation();
//        final LSSerializer serializer = implementation.createLSSerializer();
//        final String result = serializer.writeToString(document);
//        return result;
        final DOMSource domSource = new DOMSource(document);
        final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        final TransformerFactory tf = TransformerFactory.newInstance();
        try {
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            transformer.setOutputProperty("standalone", "yes");
            transformer.transform(domSource, result);
            writer.flush();
            final String body = writer.toString();
            return body;
        } catch (TransformationException | TransformerException e) {
            throw new BodyProcessingException(e);
        }
    }

    private void processATags(List<Node> aTagsToCheck, Map<String, AssetCharacter> assetTypes) {
    	for(Node node : aTagsToCheck){
    		Optional<String> assetId = extractId(node);
    		if(assetId.isPresent()){
	    		String uuid = assetId.get();
	    		if(assetTypes.containsKey(uuid) && isValidInternalLink(assetTypes.get(uuid))){
	    			transformInternalLink(assetTypes, node, uuid);
	    		}
    		}
    	}
    }

	private void transformInternalLink(Map<String, AssetCharacter> assetTypes, Node node, String uuid) {
		if(isInternalLink(assetTypes.get(uuid))) {
			replaceInternalLink(node, assetTypes.get(uuid).getUuid());
		} else {
            transformLinkToAssetOnFtCom(node, uuid);
		}
	}

	private void replaceInternalLink(Node node, String uuid) {
		Element newElement = node.getOwnerDocument().createElement(CONTENT_TAG);
		newElement.setAttribute("id", uuid);
		newElement.setAttribute("type", ARTICLE_TYPE);
		Optional<String> nodeValue = getTitleAttributeIfExists(node);
		if(nodeValue.isPresent()){
			newElement.setAttribute("title", nodeValue.get());
		}
		newElement.setTextContent(node.getTextContent());
		node.getParentNode().replaceChild(newElement, node);
	}

	private Optional<String> getTitleAttributeIfExists(Node node) {
		if(getAttribute(node, "title") != null){
			String nodeValue = getAttribute(node, "title").getNodeValue();
			return Optional.fromNullable(nodeValue);
		}
		return Optional.absent();
	}

	private void transformLinkToAssetOnFtCom(Node aTag, String uuid) {

        String oldHref = getHref(aTag);
        String newHref;

        if(oldHref.startsWith(FT_COM_WWW_URL)) {

            URI ftAssetUri = URI.create(oldHref);

            String path = ftAssetUri.getPath();

            if(path.startsWith("/intl")) {
                newHref =  ftAssetUri.resolve(path.substring(5)).toString();
            } else {
                if (isSlideshowUrl(aTag)) {
                    newHref = oldHref;
                } else {
                    // do this to get rid of query params and fragment identifiers from the url
                    newHref =  ftAssetUri.resolve(path).toString();                    
                }
            }

        } else {
            newHref = "http://www.ft.com/cms/s/"+ uuid + ".html";
        }

        getAttribute(aTag, "href").setNodeValue(newHref);

		// We might have added a type attribute to identify the type of content this links to.
		// If so, it should be removed, because it is not HTML5 compliant.
		removeTypeAttributeIfPresent(aTag);
	}

	private void removeTypeAttributeIfPresent(Node aTag) {
		if (getAttribute(aTag, TYPE) != null) {
			aTag.getAttributes().removeNamedItem(TYPE);
		}
	}

	private boolean isSlideshowUrl(Node aTag) {
		return getAttribute(aTag, TYPE) != null && getAttribute(aTag, TYPE).getNodeValue().equals("slideshow");
    }

	private Node getAttribute(Node aTag, String attributeName) {
		return aTag.getAttributes().getNamedItem(attributeName);
	}

	private Optional<String> extractId(Node node) {
        return extractId(getHref(node));
	}

    private Optional<String> extractId(String href) {
        Matcher matcher = REGEX_PATTERN.matcher(href);
        if(matcher.matches()){
            return Optional.fromNullable(matcher.group(1));
        }
        return Optional.absent();
    }

    private boolean isInternalLink(AssetCharacter assetCharacter){
    	return assetCharacter.existsInContentStore() || new SupportedTypeResolver(assetCharacter.getUnderlyingType()).isASupportedType();
    }
    
    private boolean isValidInternalLink(AssetCharacter assetCharacter){
    	return "".equals(assetCharacter.getErrorMessage());
    }

    private String getHref(Node aTag) {
        final NamedNodeMap attributes = aTag.getAttributes();
        final Node hrefAttr = attributes.getNamedItem("href");
        return hrefAttr == null ? null : hrefAttr.getNodeValue();
    }
 
    private void removeATag(Node aTag) {
    	Node parentNode = aTag.getParentNode();
    	if (aTag.hasChildNodes()) {
    		parentNode.replaceChild(aTag.getFirstChild(), aTag);	
    	} else {
    		parentNode.removeChild(aTag);
    	}
	}

    private Set<String> extractIds(List<Node> aTagsToCheck) {
        final List<String> ids = new ArrayList<>(aTagsToCheck.size());

        for (Node node : aTagsToCheck) {
            Optional<String> optionalId = extractId(node);
            if(optionalId.isPresent())
				ids.add(optionalId.get());
        }

        return new HashSet<>(ids);
    }

    private boolean shouldCheckTypeWithMethode(String href) {
        return extractId(href).isPresent();
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        return documentBuilderFactory.newDocumentBuilder();
    }

	static class AssetCharacter {

		private String uuid;
		private String errorMessage = "";
		private String underlyingType;
		private boolean existsInContentStore;

		AssetCharacter(EomAssetType eomAssetType) {
			this.uuid = eomAssetType.getUuid();
			this.errorMessage = eomAssetType.getErrorMessage();
			this.underlyingType = eomAssetType.getType();
		}

		private AssetCharacter(String uuid, boolean existsInContentStore) {
			this.uuid = uuid;
			this.existsInContentStore = existsInContentStore;
		}

		static AssetCharacter existsInContentStore(String uuid) {
			return new AssetCharacter(uuid, true);
		}

		public String getUuid() {
			return uuid;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public String getUnderlyingType() {
			return underlyingType;
		}

		public boolean existsInContentStore() {
			return existsInContentStore;
		}
	}
}
