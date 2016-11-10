package com.ft.methodearticlemapper.messaging;

import com.ft.content.model.Content;
import com.ft.messagequeueproducer.MessageProducer;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;

import java.util.Collections;
import java.util.Date;

public class MessageProducingArticleMapper {

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

    public void mapList(EomFile methodeContent, String transactionId, Date messageTimestamp) {
        Content article = articleMapper.processPublication(methodeContent, transactionId, messageTimestamp);
        producer.send(Collections.singletonList(messageBuilder.buildMessage(article)));
    }
}
