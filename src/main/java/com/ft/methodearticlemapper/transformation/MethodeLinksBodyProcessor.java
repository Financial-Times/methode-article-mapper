package com.ft.methodearticlemapper.transformation;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.TransactionIdBodyProcessingContext;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodearticlemapper.exception.DocumentStoreApiUnavailableException;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MethodeLinksBodyProcessor implements BodyProcessor {

    public static final String FT_COM_WWW_URL = "http://www.ft.com/";
    public static final String ARTICLE_TYPE = "http://www.ft.com/ontology/content/Article";
    public static final String TYPE = "type";
    public static final List<String> WHITELISTED_TAGS = Collections.singletonList("recommended");

	private static final String CONTENT_TAG = "content";
    private static final String UUID_REGEX = ".*([0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}).*";
    private static final Pattern UUID_REGEX_PATTERN = Pattern.compile(UUID_REGEX);
    private static final String UUID_PARAM_REGEX = ".*uuid=" + UUID_REGEX;
    private static final Pattern UUID_PARAM_REGEX_PATTERN = Pattern.compile(UUID_PARAM_REGEX);
    private static final String ANCHOR_PREFIX = "#";

    private ResilientClient documentStoreApiClient;
    private URI uri;

	public MethodeLinksBodyProcessor(ResilientClient documentStoreApiClient, URI uri) {
		this.documentStoreApiClient = documentStoreApiClient;
        this.uri = uri;
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
                final NodeList aTags = (NodeList) xpath.evaluate("//a[count(ancestor::promo-link)=0]", document, XPathConstants.NODESET);
                for (int i = 0; i < aTags.getLength(); i++) {
                    final Element aTag = (Element)aTags.item(i);

                    if (isInsideWhitelistedTag(aTag)) {
                        continue;
                    }

                    if (isRemovable(aTag)) {
                        removeATag(aTag);
                    } else if (containsUuid(getHref(aTag))) {
                        aTagsToCheck.add(aTag);
                    }
                }
            } catch (XPathExpressionException e) {
                throw new BodyProcessingException(e);
            }

            final Set<String> uuidsToCheck = extractUuids(aTagsToCheck);

			if (bodyProcessingContext instanceof TransactionIdBodyProcessingContext) {
				TransactionIdBodyProcessingContext transactionIdBodyProcessingContext =
						(TransactionIdBodyProcessingContext) bodyProcessingContext;
				
				final List<String> uuidsPresentInContentStore = getUuidsPresentInContentStore(uuidsToCheck, transactionIdBodyProcessingContext.getTransactionId());

				processATags(aTagsToCheck, uuidsPresentInContentStore);

				final String modifiedBody = serializeBody(document);
				return modifiedBody;
			} else {
				IllegalStateException up = new IllegalStateException("bodyProcessingContext should provide transaction id.");
				throw up;
			}
        }
        return body;
    }

    private boolean isInsideWhitelistedTag(Element aTag) {
        Node parent = aTag.getParentNode();
        while (parent != null) {
            String nodeName = parent.getNodeName();
            if (WHITELISTED_TAGS.contains(nodeName)) {
                return true;
            }
            parent = parent.getParentNode();
        }
        return false;
    }

    /**
     * We remove <code>&lt;a&gt;</code> tags
     * <ul>
     * <li>with blank hrefs (which are either invalid, or refer to the current document);</li>
     * <li>with hrefs that contain only a fragment identifier (a part of the current document);</li>
     * <li>with no non-whitespace content <i>and</i> no <code>data-*</code> attributes.</li>
     * </ul>
     * @param aTag the tag
     * @return true if removable, otherwise false.
     */
	private boolean isRemovable(final Node aTag) {
        final String href = getHref(aTag);
        
        if (href.isEmpty() || href.startsWith(ANCHOR_PREFIX)) {
            return true;
        }
        
        NodeList children = aTag.getChildNodes();
        int len = children.getLength();
        
        StringBuilder textContent = new StringBuilder();
        for (int i = 0; i < len; i++) {
            Node n = children.item(i);
            
            switch (n.getNodeType()) {
                case Node.TEXT_NODE:
                case Node.CDATA_SECTION_NODE:
                    textContent.append(n.getTextContent());
                    break;
                    
                case Node.COMMENT_NODE:
                    break;
                    
                default: // any other node type
                    return false;
                
            }
        }
        
        if (!textContent.toString().trim().isEmpty()) {
            return false;
        }
        
        NamedNodeMap attributes = aTag.getAttributes();
        len = attributes.getLength();
        
        for (int i = 0; i < len; i++) {
            if (attributes.item(i).getNodeName().startsWith("data-")) {
                return false;
            }
        }
        
        return true;
	}
	
    private List<String> getUuidsPresentInContentStore(Set<String> uuidsToCheck, String transactionId) {
        if(uuidsToCheck.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> uuidsPresentInContentStore = new ArrayList<>();
        
        for (String uuidToCheck: uuidsToCheck) {
            if (existsInDocumentStore(uuidToCheck, transactionId)) {
                uuidsPresentInContentStore.add(uuidToCheck);
            }
        }
        return uuidsPresentInContentStore;
    }

	private boolean existsInDocumentStore(String idToCheck, String transactionId) {
        int responseStatusCode;
        ClientResponse clientResponse = null;
		URI contentUrl = contentUrlBuilder().build(idToCheck);
		try {
			clientResponse = documentStoreApiClient.resource(contentUrl)
					.accept(MediaType.APPLICATION_JSON_TYPE)
					.header(TransactionIdUtils.TRANSACTION_ID_HEADER, transactionId)
                    .header("Host", "document-store-api")
					.get(ClientResponse.class);
            
            responseStatusCode = clientResponse.getStatus();
        }
        catch (ClientHandlerException che) {
			Throwable cause = che.getCause();
			if(cause instanceof IOException) {
				throw new DocumentStoreApiUnavailableException(che);
			}
			throw che;
		}
        finally {
            if (clientResponse != null) {
                clientResponse.close();
            }
        }

		int responseStatusFamily = responseStatusCode / 100;
        
        if (responseStatusFamily == 5) {
            // can't tell whether it exists
            String msg = String.format("Document store API returned %s", responseStatusCode);
            throw new DocumentStoreApiUnavailableException(msg);
        }

		return responseStatusFamily == 2;
	}

	private UriBuilder contentUrlBuilder() {
		return UriBuilder.fromUri(uri).path("{uuid}");

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
	


    private void processATags(List<Node> aTagsToCheck, List<String> uuidsPresentInContentStore) {
        for(Node node : aTagsToCheck){
            Optional<String> assetUuid = extractUuid(node);
            if(assetUuid.isPresent()){
                String uuid = assetUuid.get();
                if (uuidsPresentInContentStore.contains(uuid)) {
                    replaceLinkToContentPresentInDocumentStore(node, uuid);
                } else if (isConvertableToAssetOnFtCom(node)){
                    transformLinkToAssetOnFtCom(node, uuid); // e.g slideshow galleries
                } else {
                    // leave it alone, we don't know what to do with it
                }
            }
        }
    }
	
	private boolean isConvertableToAssetOnFtCom(Node node) {
		String href = getHref(node);
		if (href.startsWith(FT_COM_WWW_URL)) {
			return true;
		} else if (href.startsWith("/")) { // i.e. it's a relative path in Methode with a UUID param		
			Matcher matcher = UUID_PARAM_REGEX_PATTERN.matcher(href);
			if(matcher.matches()){
				return true;
			}
		}
        return false;
		
	}

	private void replaceLinkToContentPresentInDocumentStore(Node node, String uuid) {
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

	private Optional<String> extractUuid(Node node) {
        return extractUuid(getHref(node));
	}

    private Optional<String> extractUuid(String href) {
        Matcher matcher = UUID_REGEX_PATTERN.matcher(href);
        if(matcher.matches()){
            return Optional.fromNullable(matcher.group(1));
        }
        return Optional.absent();
    }

    private String getHref(Node aTag) {
        final NamedNodeMap attributes = aTag.getAttributes();
        final Node hrefAttr = attributes.getNamedItem("href");
        return hrefAttr == null ? "" : hrefAttr.getNodeValue();
    }
    
    /** Strips out a tag.
     *  If the child content of the tag is empty or only whitespace, it is removed;
     *  any other child content of the tag is preserved in place.
     *  @param aTag the tag
     */
    private void removeATag(Element aTag) {
    	Node parentNode = aTag.getParentNode();
    	
    	if (!aTag.getTextContent().trim().isEmpty()) {
    	    NodeList children = aTag.getChildNodes();
            Node n = children.item(0);
    	    while (n != null) {
    	        aTag.removeChild(n);
    	        parentNode.insertBefore(n, aTag);
                n = children.item(0);
    	    }
    	}
        parentNode.removeChild(aTag);
	}
    
    private Set<String> extractUuids(List<Node> aTagsToCheck) {
        final List<String> uuids = new ArrayList<>(aTagsToCheck.size());

        for (Node node : aTagsToCheck) {
            Optional<String> optionalUuid = extractUuid(node);
            if(optionalUuid.isPresent())
				uuids.add(optionalUuid.get());
        }

        return new HashSet<>(uuids);
    }

    private boolean containsUuid(String href) {
        return extractUuid(href).isPresent();
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        return documentBuilderFactory.newDocumentBuilder();
    }

}
