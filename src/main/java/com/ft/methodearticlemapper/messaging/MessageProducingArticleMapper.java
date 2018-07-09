package com.ft.methodearticlemapper.messaging;

import com.ft.messagequeueproducer.MessageProducer;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

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

    void mapArticle(EomFile methodeContent, String transactionId, Date messageTimestamp) {
        Message message;
        try {
            message = messageBuilder.buildMessage(
                    articleMapper.process(methodeContent, TransformationMode.PUBLISH, transactionId, messageTimestamp)
            );
        } catch (MethodeMarkedDeletedException e) {
            LOGGER.info("Article {} is marked as deleted.", methodeContent.getUuid());
            message = messageBuilder.buildMessageForDeletedMethodeContent(
                    methodeContent.getUuid(), transactionId, messageTimestamp, e.getType()
            );
        }
        producer.send(Collections.singletonList(message));
    }
}
