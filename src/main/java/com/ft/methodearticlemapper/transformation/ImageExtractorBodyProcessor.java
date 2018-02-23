package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.BodyProcessor;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class ImageExtractorBodyProcessor implements BodyProcessor {

    private static final String A_TAG = "a";
    private static final String IMAGE_SET = "//p/image-set";
    private static final String WEB_MASTER = "//p/web-master";
    private static final String WEB_INLINE_PICTURE = "//p/web-inline-picture";
    private static final String IMG_EMPTY_SRC = "//img[@src[not(string())]]";
    private static final String IMG_MISSING_SRC = "//img[not(@src)]";
    private static final String IMG_INSIDE_A_TAG = "//p/a/img";
    private static final String IMG = "//p/img";

    @Override
    public String process(String body, BodyProcessingContext bodyProcessingContext) throws BodyProcessingException {
        if (StringUtils.isBlank(body)) {
            return body;
        }

        try {
            DocumentBuilder documentBuilder = getDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new StringReader(body)));
            XPath xPath = XPathFactory.newInstance().newXPath();

            simpleParagraphImageExtract(IMAGE_SET, xPath, document);
            simpleParagraphImageExtract(WEB_MASTER, xPath, document);
            simpleParagraphImageExtract(WEB_INLINE_PICTURE, xPath, document);
            deleteNodeIncludingParentATag(IMG_EMPTY_SRC, xPath, document);
            deleteNodeIncludingParentATag(IMG_MISSING_SRC, xPath, document);
            simpleParagraphImageExtract(IMG, xPath, document);
            paragraphImageExtractWithATagDeletion(xPath, document);

            body = serializeBody(document);
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException | XPathExpressionException e) {
            throw new BodyProcessingException(e);
        }
        return body;
    }

    private void simpleParagraphImageExtract(String expression, XPath xPath, Document document) throws XPathExpressionException {
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node imgNode = nodeList.item(i);
            Node paragraphNode = imgNode.getParentNode();
            Node paragraphParentNode = paragraphNode.getParentNode();
            Node imageNodeCopy = imgNode.cloneNode(true);
            paragraphParentNode.insertBefore(imageNodeCopy, paragraphNode);
            paragraphNode.removeChild(imgNode);
        }
    }

    private void deleteNodeIncludingParentATag(String expression, XPath xPath, Document document) throws XPathExpressionException {
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node imgNode = nodeList.item(i);
            Node parentNode = imgNode.getParentNode();
            if (A_TAG.equals(parentNode.getNodeName())) {
                Node aTagParent = parentNode.getParentNode();
                aTagParent.removeChild(parentNode);
            } else {
                parentNode.removeChild(imgNode);
            }
        }
    }

    private void paragraphImageExtractWithATagDeletion(XPath xPath, Document document) throws XPathExpressionException {
        NodeList nodeList = (NodeList) xPath.compile(IMG_INSIDE_A_TAG).evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node imgNode = nodeList.item(i);
            Node aTagNode = imgNode.getParentNode();
            Node paragraphNode = aTagNode.getParentNode();
            Node paragraphParentNode = paragraphNode.getParentNode();
            Node imageNodeCopy = imgNode.cloneNode(true);
            paragraphParentNode.insertBefore(imageNodeCopy, paragraphNode);
            paragraphNode.removeChild(aTagNode);
        }
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        return builderFactory.newDocumentBuilder();
    }

    private String serializeBody(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");

        DOMSource domSource = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        transformer.transform(domSource, result);

        writer.flush();
        return writer.toString();
    }
}
