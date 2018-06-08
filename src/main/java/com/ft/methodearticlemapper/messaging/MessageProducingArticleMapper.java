package com.ft.methodearticlemapper.messaging;

import com.ft.messagequeueproducer.MessageProducer;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.SourceNotEligibleForPublishException;
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;

import com.ft.methodearticlemapper.util.DetermineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static com.ft.methodearticlemapper.model.EomFile.SOURCE_ATTR_XPATH;

public class MessageProducingArticleMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProducingArticleMapper.class);

    private final MessageBuilder messageBuilder;
    private final MessageProducer producer;
    private final EomFileProcessor articleMapper;

    public MessageProducingArticleMapper(
            MessageBuilder messageBuilder,
            MessageProducer producer,
            EomFileProcessor articleMapper) {

        this.messageBuilder = messageBuilder;
        this.producer = producer;
        this.articleMapper = articleMapper;
    }

    void mapArticle(EomFile methodeContent, String transactionId, Date messageTimestamp) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Message message;
        final DocumentBuilder documentBuilder = getDocumentBuilder();
        final XPath xpath = XPathFactory.newInstance().newXPath();
        Document attributesDocument = documentBuilder.parse(new InputSource(new StringReader(methodeContent.getAttributes())));
        try {
            message = messageBuilder.buildMessage(
                    articleMapper.process(methodeContent, TransformationMode.PUBLISH, transactionId, messageTimestamp)
            );
        } catch (MethodeMarkedDeletedException e) {
            LOGGER.info("Article {} is marked as deleted.", methodeContent.getUuid());
            message = messageBuilder.buildMessageForDeletedMethodeContent(
                    methodeContent.getUuid(), transactionId, messageTimestamp, DetermineType.determineType(xpath,attributesDocument,processSourceForPublish(UUID.fromString(methodeContent.getUuid()),methodeContent))
            );
        }
        producer.send(Collections.singletonList(message));
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        return documentBuilderFactory.newDocumentBuilder();
    }

    protected ContentSource processSourceForPublish(UUID uuid, EomFile eomFile) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        final DocumentBuilder documentBuilder = getDocumentBuilder();
        final XPath xpath = XPathFactory.newInstance().newXPath();
        Document attributesDocument = documentBuilder.parse(new InputSource(new StringReader(eomFile.getAttributes())));
        String sourceCode = retrieveSourceCode(xpath,attributesDocument);
        ContentSource contentSource = ContentSource.getByCode(sourceCode);
        if (contentSource == null) {
            throw new SourceNotEligibleForPublishException(uuid, sourceCode);
        }
        return contentSource;
    }

    private String retrieveSourceCode(XPath xpath, Document attributesDocument) throws XPathExpressionException {
        return xpath.evaluate(SOURCE_ATTR_XPATH, attributesDocument);
    }
}
