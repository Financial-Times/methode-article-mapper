package com.ft.methodearticlemapper.messaging;

import com.ft.content.model.Content;
import com.ft.messagequeueproducer.MessageProducer;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageProducingListMapperTest {

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
        when(mapper.processPublication(any(), eq("tid"), eq(lastModified))).thenReturn(mappedArticle);

        msgProducingArticleMapper.mapArticle(new EomFile.Builder().build(), "tid", lastModified);

        verify(messageBuilder).buildMessage(mappedArticle);
    }

    @Test
    public void thatCreatedMessageIsSentToQueue() {
        Content mockedContent = mock(Content.class);
        Message mockedMessage = mock(Message.class);
        when(mapper.processPublication(any(), anyString(), any())).thenReturn(mockedContent);
        when(messageBuilder.buildMessage(mockedContent)).thenReturn(mockedMessage);

        msgProducingArticleMapper.mapArticle(new EomFile.Builder().build(), "tid", new Date());

        verify(producer).send(Collections.singletonList(mockedMessage));
    }
}