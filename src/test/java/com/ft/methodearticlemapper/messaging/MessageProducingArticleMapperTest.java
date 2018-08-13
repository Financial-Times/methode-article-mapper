package com.ft.methodearticlemapper.messaging;

import com.ft.content.model.Content;
import com.ft.messagequeueproducer.MessageProducer;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageProducingArticleMapperTest {

    @Mock
    private MessageBuilder messageBuilder;
    @Mock
    private MessageProducer producer;
    @Mock
    private EomFileProcessor mapper;

    private MessageProducingArticleMapper msgProducingArticleMapper;

    @Before
    public void setUp() {
        msgProducingArticleMapper = new MessageProducingArticleMapper (
                messageBuilder,
                producer,
                mapper
        );
    }

    @Test
    public void thatMessageIsCreatedFromMappedArticle() {
        Date lastModified = new Date();
        Content mappedArticle = new Content.Builder()
                .withUuid(UUID.randomUUID())
                .build();
        when(mapper.process(any(), eq(TransformationMode.PUBLISH), eq("tid"), eq(lastModified))).thenReturn(mappedArticle);

        Map<String,String> uppHeaders = Collections.singletonMap("UPP-foo", "bar");
        msgProducingArticleMapper.mapArticle(new EomFile.Builder().build(), "tid", lastModified, uppHeaders);

        verify(messageBuilder).buildMessage(mappedArticle, uppHeaders);
    }

    @Test
    public void thatMessageWithContentIsSentToQueue() {
        Content mockedContent = mock(Content.class);
        Message mockedMessage = mock(Message.class);
        when(mapper.process(any(), eq(TransformationMode.PUBLISH), anyString(), any())).thenReturn(mockedContent);
        when(messageBuilder.buildMessage(mockedContent, Collections.emptyMap())).thenReturn(mockedMessage);

        msgProducingArticleMapper.mapArticle(new EomFile.Builder().build(), "tid", new Date(), Collections.emptyMap());

        verify(producer).send(Collections.singletonList(mockedMessage));
    }

    @Test
    public void thatMessageWithContentMarkedAsDeletedIsSentToQueue() {
        String tid = "tid";
        Date date = new Date();
        String uuid = UUID.randomUUID().toString();
        Message deletedContentMsg = mock(Message.class);

        when(mapper.process(any(), eq(TransformationMode.PUBLISH), anyString(), any())).thenThrow(MethodeMarkedDeletedException.class);
        when(messageBuilder.buildMessageForDeletedMethodeContent(uuid, tid, date, Collections.emptyMap())).thenReturn(deletedContentMsg);

        msgProducingArticleMapper.mapArticle(new EomFile.Builder().withUuid(uuid).build(), tid, date, Collections.emptyMap());

        verify(producer).send(Collections.singletonList(deletedContentMsg));
    }
}