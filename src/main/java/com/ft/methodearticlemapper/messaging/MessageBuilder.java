package com.ft.methodearticlemapper.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.content.model.Content;
import com.ft.messagequeueproducer.model.KeyedMessage;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.messaging.standards.message.v1.SystemId;
import com.ft.methodearticlemapper.exception.MethodeArticleMapperException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import static com.ft.api.util.transactionid.TransactionIdUtils.TRANSACTION_ID_HEADER;
import static java.time.ZoneOffset.UTC;

public class MessageBuilder {

    private static final String CMS_CONTENT_PUBLISHED = "cms-content-published";
    private static final DateTimeFormatter RFC3339_FMT =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.STRICT);

    private final UriBuilder contentUriBuilder;
    private final SystemId systemId;
    private final ObjectMapper objectMapper;
    private final Map<String, String> contentTypeHeader;

    public MessageBuilder(UriBuilder contentUriBuilder, String systemId, ObjectMapper objectMapper, Map<String, String> contentTypeHeader) {
        this.contentUriBuilder = contentUriBuilder;
        this.systemId = SystemId.systemIdFromCode(systemId);
        this.objectMapper = objectMapper;
        this.contentTypeHeader = contentTypeHeader;
    }

    Message buildMessage(Content content) {
        MessageBody msgBody = new MessageBody(
                content,
                contentUriBuilder.build(content.getUuid()).toString(),
                RFC3339_FMT.format(OffsetDateTime.ofInstant(content.getLastModified().toInstant(), UTC)), contentTypeHeader
        );

        return buildMessage(content.getUuid(), content.getPublishReference(), msgBody);
    }

    Message buildMessageForDeletedMethodeContent(String uuid, String publishReference, Date lastModified) {
        MessageBody msgBody = new MessageBody(
                null,
                contentUriBuilder.build(uuid).toString(),
                RFC3339_FMT.format(OffsetDateTime.ofInstant(lastModified.toInstant(), UTC)), contentTypeHeader
        );
        return buildMessage(uuid, publishReference, msgBody);
    }

    private Message buildMessage(String uuid, String publishReference, MessageBody msgBody) {
        Message msg;
        try {
            String messageBody = objectMapper.writeValueAsString(msgBody);
            msg = new Message.Builder().withMessageId(UUID.randomUUID())
                    .withMessageType(CMS_CONTENT_PUBLISHED)
                    .withMessageTimestamp(new Date())
                    .withOriginSystemId(systemId)
                    .withContentType("application/json")
                    .withMessageBody(messageBody)
                    .build();

            msg.addCustomMessageHeader(TRANSACTION_ID_HEADER, publishReference);
            msg = KeyedMessage.forMessageAndKey(msg, uuid);
        } catch (JsonProcessingException e) {
            throw new MethodeArticleMapperException("unable to write JSON for message", e);
        }
        return msg;
    }

    public static class MessageBody {
        @JsonProperty("payload")
        public final Content payload;
        @JsonProperty("contentUri")
        public final String contentUri;
        @JsonProperty("lastModified")
        public final String lastModified;
        @JsonProperty("contentTypeHeader")
        public final Map<String, String> contentTypeHeader;

        MessageBody(
                @JsonProperty("payload")
                Content payload,
                @JsonProperty("contentUri")
                String contentUri,
                @JsonProperty("lastModified")
                String lastModified,
                @JsonProperty("contentTypeHeader")
                Map<String, String> contentTypeHeader) {
            this.contentUri = contentUri;
            this.payload = payload;
            this.lastModified = lastModified;
            this.contentTypeHeader = contentTypeHeader;
        }
    }
}
