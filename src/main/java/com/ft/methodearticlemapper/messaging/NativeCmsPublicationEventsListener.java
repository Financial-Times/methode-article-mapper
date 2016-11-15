package com.ft.methodearticlemapper.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.message.consumer.MessageListener;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.messaging.standards.message.v1.SystemId;
import com.ft.methodearticlemapper.methode.EomFileType;
import com.ft.methodearticlemapper.exception.MethodeArticleMapperException;
import com.ft.methodearticlemapper.model.EomFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Predicate;

public class NativeCmsPublicationEventsListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(NativeCmsPublicationEventsListener.class);

    private final MessageProducingArticleMapper msgProducingArticleMapper;
    private final ObjectMapper objectMapper;
    private final Predicate<Message> messageFilter;

    public NativeCmsPublicationEventsListener(ObjectMapper objectMapper, MessageProducingArticleMapper msgProducingArticleMapper, String systemCode) {
        this.objectMapper = objectMapper;
        this.msgProducingArticleMapper = msgProducingArticleMapper;

        this.messageFilter = systemIDFilter(systemCode).and(contentTypeFilter(objectMapper));
    }

    @Override
    public boolean onMessage(Message message, String transactionId) {
        if (!messageFilter.test(message)) {
            LOG.info("Skip message");
            LOG.debug("Skip message {}", message);
            return true;
        }

        LOG.info("Process message");
        try {
            EomFile methodeContent = objectMapper.reader(EomFile.class).readValue(message.getMessageBody());
            msgProducingArticleMapper.mapArticle(methodeContent, transactionId, message.getMessageTimestamp());
        } catch (IOException e) {
            throw new MethodeArticleMapperException("Unable to process message", e);
        }
        return true;
    }

    private Predicate<Message> systemIDFilter(String systemCode) {
        return msg -> (SystemId.systemIdFromCode(systemCode).equals(msg.getOriginSystemId()));
    }

    private Predicate<Message> contentTypeFilter(ObjectMapper objectMapper) {
        return msg -> {
            EomFile eomFile = null;
            try {
                eomFile = objectMapper.reader(EomFile.class).readValue(msg.getMessageBody());
            } catch (IOException e) {
                LOG.warn("Message filter failure", e);
                return false;
            }
            return isValidType(eomFile.getType());
        };
    }

    private boolean isValidType(String type) {
        return (EomFileType.EOMCompoundStory.getTypeName().equals(type)) ||
                (EomFileType.EOMStory.getTypeName().equals((type)));
    }
}
