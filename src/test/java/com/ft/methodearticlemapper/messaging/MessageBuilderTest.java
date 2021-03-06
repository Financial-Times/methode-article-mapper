package com.ft.methodearticlemapper.messaging;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.content.model.Content;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.methodearticlemapper.exception.MethodeArticleMapperException;
import com.ft.methodearticlemapper.exception.MissingMappingForContentTypeException;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.UriBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageBuilderTest {

  private static final UUID UUID =
      java.util.UUID.fromString("4a319c8a-7b8f-4bdb-bb5f-f7fe70872de2");
  private static final String SYSTEM_ID = "foobar";
  private static final String PUBLISH_REFERENCE = "junit";
  private static final String ARTICLE_CONTENT_TYPE_MAPPING = "application/vnd.ft-upp-article+json";
  private static final String ARTICLE_CONTENT_TYPE = "Article";

  @Mock private UriBuilder contentUriBuilder;
  @Mock private ObjectMapper objectMapper;
  private Map<String, String> contentTypeMappings;

  private MessageBuilder messageBuilder;

  @Before
  public void setUp() {
    contentTypeMappings = new HashMap<>();
    contentTypeMappings.put(ARTICLE_CONTENT_TYPE, ARTICLE_CONTENT_TYPE_MAPPING);
    messageBuilder =
        new MessageBuilder(contentUriBuilder, SYSTEM_ID, objectMapper, contentTypeMappings);
  }

  @Test
  public void thatMsgHeadersAreSet() throws JsonProcessingException {
    Content content =
        new Content.Builder()
            .withUuid(UUID)
            .withLastModified(new Date())
            .withType(ARTICLE_CONTENT_TYPE)
            .withPublishReference(PUBLISH_REFERENCE)
            .build();
    when(contentUriBuilder.build(content.getUuid())).thenReturn(URI.create("foobar"));
    when(objectMapper.writeValueAsString(anyMap())).thenReturn("\"foo\":\"bar\"");

    Map<String, String> uppHeaders = new HashMap<>();
    uppHeaders.put("UPP-foo", "12345");
    uppHeaders.put("UPP-bar", "qwerty");

    Message msg = messageBuilder.buildMessage(content, uppHeaders);

    assertThat(msg.getCustomMessageHeader("X-Request-Id"), equalTo(PUBLISH_REFERENCE));
    assertThat(msg.getOriginSystemId().toString(), containsString(SYSTEM_ID));
    assertThat(msg.getContentType().toString(), equalTo(ARTICLE_CONTENT_TYPE_MAPPING));

    for (Map.Entry<String, String> en : uppHeaders.entrySet()) {
      assertThat(msg.getCustomMessageHeader(en.getKey()), equalTo(en.getValue()));
    }
  }

  @Test
  public void thatMsgBodyIsCorrect() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    UriBuilder contentUriBuilder = mock(UriBuilder.class);
    messageBuilder =
        new MessageBuilder(contentUriBuilder, SYSTEM_ID, objectMapper, contentTypeMappings);

    String lastModified = "2016-11-02T07:59:24.715Z";
    Date lastModifiedDate = Date.from(Instant.parse(lastModified));
    Content content =
        new Content.Builder()
            .withUuid(UUID)
            .withType(ARTICLE_CONTENT_TYPE)
            .withLastModified(lastModifiedDate)
            .withPublishReference(PUBLISH_REFERENCE)
            .build();

    URI contentUri = URI.create("foobar");
    when(contentUriBuilder.build(UUID.toString())).thenReturn(contentUri);

    Message msg = messageBuilder.buildMessage(content, Collections.emptyMap());

    Map<String, Object> msgContent = objectMapper.reader(Map.class).readValue(msg.getMessageBody());
    assertThat(msgContent.get("contentUri"), equalTo(contentUri.toString()));
    assertThat(msgContent.get("lastModified"), equalTo(lastModified));
    assertThat(msgContent.get("payload"), instanceOf(Map.class));
  }

  @Test
  public void thatMessageForDeletedContentIsCorrect() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    UriBuilder contentUriBuilder = mock(UriBuilder.class);
    messageBuilder =
        new MessageBuilder(contentUriBuilder, SYSTEM_ID, objectMapper, contentTypeMappings);

    URI contentUri = URI.create("foobar");
    when(contentUriBuilder.build(UUID.toString())).thenReturn(contentUri);

    String lastModified = "2016-11-02T07:59:24.715Z";
    Date lastModifiedDate = Date.from(Instant.parse(lastModified));

    Message msg =
        messageBuilder.buildMessageForDeletedMethodeContent(
            UUID.toString(), "tid", lastModifiedDate, ARTICLE_CONTENT_TYPE, Collections.emptyMap());

    assertThat(msg.getContentType().toString(), equalTo(ARTICLE_CONTENT_TYPE_MAPPING));

    Map<String, Object> msgContent = objectMapper.reader(Map.class).readValue(msg.getMessageBody());
    assertThat(msgContent.get("contentUri"), equalTo(contentUri.toString()));
    assertThat(msgContent.get("lastModified"), equalTo(lastModified));
    assertNull(msgContent.get("payload"));
  }

  @Test(expected = MethodeArticleMapperException.class)
  public void thatMethodeArticleMapperExceptionIsThrownIfMarshallingToStringFails()
      throws JsonProcessingException {
    Content list =
        new Content.Builder()
            .withUuid(UUID)
            .withType(ARTICLE_CONTENT_TYPE)
            .withLastModified(new Date())
            .withPublishReference(PUBLISH_REFERENCE)
            .build();
    when(contentUriBuilder.build(list.getUuid())).thenReturn(URI.create("foobar"));
    when(objectMapper.writeValueAsString(anyMap())).thenThrow(new JsonMappingException("oh-oh"));

    messageBuilder.buildMessage(list, Collections.emptyMap());
  }

  @Test(expected = MissingMappingForContentTypeException.class)
  public void thatMissingMappingForContentTypeExceptionIsThrownIfNoMappingIsFoundForContentType()
      throws JsonProcessingException {
    Content list =
        new Content.Builder()
            .withUuid(UUID)
            .withType("Invalid ContentType")
            .withLastModified(new Date())
            .withPublishReference(PUBLISH_REFERENCE)
            .build();
    when(contentUriBuilder.build(list.getUuid())).thenReturn(URI.create("foobar"));
    when(objectMapper.writeValueAsString(anyMap())).thenThrow(new JsonMappingException("oh-oh"));

    messageBuilder.buildMessage(list, Collections.emptyMap());
  }
}
