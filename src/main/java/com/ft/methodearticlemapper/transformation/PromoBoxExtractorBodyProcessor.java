package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.BodyProcessor;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PromoBoxExtractorBodyProcessor implements BodyProcessor {

  private static final String PROMO_BOX = "//promo-box[ancestor::p]";

  @Override
  public String process(String body, BodyProcessingContext bodyProcessingContext)
      throws BodyProcessingException {
    if (StringUtils.isBlank(body)) {
      return body;
    }

    try {
      DocumentBuilder documentBuilder = getDocumentBuilder();
      Document document = documentBuilder.parse(new InputSource(new StringReader(body)));
      XPath xPath = XPathFactory.newInstance().newXPath();

      paragraphPromoBoxExtract(document, xPath);

      body = serializeBody(document);
    } catch (ParserConfigurationException
        | IOException
        | SAXException
        | TransformerException
        | XPathExpressionException e) {
      e.printStackTrace();
    }

    return body;
  }

  private void paragraphPromoBoxExtract(Document document, XPath xPath)
      throws XPathExpressionException {
    NodeList nodeList =
        (NodeList) xPath.compile(PROMO_BOX).evaluate(document, XPathConstants.NODESET);
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node promoBoxNode = nodeList.item(i);
      Node promoBoxParentNode = promoBoxNode.getParentNode();
      Node parent = promoBoxParentNode;
      Node paragraphNode = promoBoxParentNode;

      while (parent.getParentNode() != null) {
        if (parent.getNodeName().equals("p")) {
          paragraphNode = parent;
        }
        parent = parent.getParentNode();
      }

      Node paragraphParentNode = paragraphNode.getParentNode();
      Node promoBoxNodeCopy = promoBoxNode.cloneNode(true);
      paragraphParentNode.insertBefore(promoBoxNodeCopy, paragraphNode);
      promoBoxParentNode.removeChild(promoBoxNode);
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
