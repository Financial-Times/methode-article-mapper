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

    public MessageBuilder(UriBuilder contentUriBuilder, String systemId, ObjectMapper objectMapper) {
        this.contentUriBuilder = contentUriBuilder;
        this.systemId = SystemId.systemIdFromCode(systemId);
        this.objectMapper = objectMapper;
    }

    Message buildMessage(Content content, Map<String,String> headers) {
        MessageBody msgBody = new MessageBody(
                content,
                contentUriBuilder.build(content.getUuid()).toString(),
                RFC3339_FMT.format(OffsetDateTime.ofInstant(content.getLastModified().toInstant(), UTC))
        );

        return buildMessage(content.getUuid(), content.getPublishReference(), headers, msgBody);
    }

    Message buildMessageForDeletedMethodeContent(String uuid, String publishReference, Date lastModified, Map<String,String> headers) {
        MessageBody msgBody = new MessageBody(
                null,
                contentUriBuilder.build(uuid).toString(),
                RFC3339_FMT.format(OffsetDateTime.ofInstant(lastModified.toInstant(), UTC))
        );
        return buildMessage(uuid, publishReference, headers, msgBody);
    }

    private Message buildMessage(String uuid, String publishReference, Map<String,String> headers, MessageBody msgBody) {
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
            for (Map.Entry<String,String> en : headers.entrySet()) {
                msg.addCustomMessageHeader(en.getKey(), en.getValue());
            }
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

        MessageBody(
                @JsonProperty("payload")
                Content payload,
                @JsonProperty("contentUri")
                String contentUri,
                @JsonProperty("lastModified")
                String lastModified) {
            this.contentUri = contentUri;
            this.payload = payload;
            this.lastModified = lastModified;
        }
    }
}
