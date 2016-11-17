package com.ft.methodearticlemapper.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.ft.messaging.standards.message.v1.Message;
import com.ft.messaging.standards.message.v1.SystemId;
import com.ft.methodearticlemapper.exception.MethodeArticleMapperException;
import com.ft.methodearticlemapper.model.EomFile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Date;

import javax.ws.rs.core.UriBuilder;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NativeCmsPublicationEventsListenerTest {

    private static final String SYSTEM_CODE = "foobar";
    private static final String TX_ID = "tid_foo";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MessageProducingArticleMapper mapper;
    @Mock
    private MessageBuilder messageBuilder;
    @Mock
    private UriBuilder contentUriBuilder;

    private NativeCmsPublicationEventsListener listener;

    @Before
    public void setUp() {
        listener = new NativeCmsPublicationEventsListener(
                objectMapper,
                mapper,
                SYSTEM_CODE);
    }

    @Test
    public void thatMessageIsIgnoredIfUnexpectedSystemIDHeaderFound() throws Exception {
        Message msg = new Message();
        msg.setOriginSystemId(SystemId.systemIdFromCode("foobaz"));
        msg.setMessageTimestamp(new Date());
        msg.setMessageBody(objectMapper.writeValueAsString(new EomFile.Builder().build()));

        listener.onMessage(msg, TX_ID);

        verify(mapper, never()).mapArticle(Matchers.any(), anyString(), Matchers.any());
    }

    @Test
    public void thatMessageIsIgnoredIfNotSupportedContentTypeDetected() throws Exception {
        Message msg = new Message();
        msg.setOriginSystemId(SystemId.systemIdFromCode(SYSTEM_CODE));
        msg.setMessageTimestamp(new Date());
        msg.setMessageBody(
                objectMapper.writeValueAsString(
                        new EomFile.Builder()
                                .withType("foobaz")
                                .build()
                )
        );

        listener.onMessage(msg, TX_ID);

        verify(mapper, never()).mapArticle(Matchers.any(), anyString(), Matchers.any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void thatMessageIsIgnoredIfIOExceptionIsThrownDuringTypeFiltering() throws Exception {
        Message mockMsg = mock(Message.class);
        when(mockMsg.getOriginSystemId()).thenReturn(SystemId.systemIdFromCode(SYSTEM_CODE));

        ObjectReader mockReader = mock(ObjectReader.class);
        when(mockReader.readValue(mockMsg.getMessageBody())).thenThrow(IOException.class);

        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        when(mockObjectMapper.reader(EomFile.class)).thenReturn(mockReader);

        NativeCmsPublicationEventsListener listener =
                new NativeCmsPublicationEventsListener(mockObjectMapper, mapper, SYSTEM_CODE);

        listener.onMessage(mockMsg, TX_ID);

        verify(mapper, never()).mapArticle(Matchers.any(), anyString(), Matchers.any());
    }

    @Test (expected = MethodeArticleMapperException.class)
    public void thatServiceExceptionIsThrownIfIOExceptionIsThrownDuringPreMapping() throws Exception {
        EomFile mockEomFile = mock(EomFile.class);
        when(mockEomFile.getType()).thenReturn("EOM::Story");

        Message mockMsg = mock(Message.class);
        when(mockMsg.getOriginSystemId()).thenReturn(SystemId.systemIdFromCode(SYSTEM_CODE));

        ObjectReader mockReader = mock(ObjectReader.class);
        when(mockReader.readValue(mockMsg.getMessageBody())).thenReturn(mockEomFile).thenThrow(IOException.class);

        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        when(mockObjectMapper.reader(EomFile.class)).thenReturn(mockReader);

        NativeCmsPublicationEventsListener listener =
                new NativeCmsPublicationEventsListener(mockObjectMapper, mapper, SYSTEM_CODE);

        listener.onMessage(mockMsg, TX_ID);

        verify(mapper, never()).mapArticle(Matchers.any(), anyString(), Matchers.any());
    }

    @Test
    public void thatMessageIsMappedIfCorrectSystemIDAndContentTypeIsCompoundStory() throws Exception {
        Message msg = new Message();
        msg.setOriginSystemId(SystemId.systemIdFromCode(SYSTEM_CODE));
        msg.setMessageTimestamp(new Date());
        msg.setMessageBody(
                objectMapper.writeValueAsString(
                        new EomFile.Builder()
                                .withType("EOM::CompoundStory")
                                .build()
                )
        );

        listener.onMessage(msg, TX_ID);

        verify(mapper).mapArticle(Matchers.any(), eq(TX_ID), Matchers.any());
    }

    @Test
    public void thatMessageIsMappedIfCorrectSystemIDAndContentTypeIsSimpleStory() throws Exception {
        Message msg = new Message();
        msg.setOriginSystemId(SystemId.systemIdFromCode(SYSTEM_CODE));
        msg.setMessageTimestamp(new Date());
        msg.setMessageBody(
                objectMapper.writeValueAsString(
                        new EomFile.Builder()
                                .withType("EOM::Story")
                                .build()
                )
        );

        listener.onMessage(msg, TX_ID);

        verify(mapper).mapArticle(Matchers.any(), eq(TX_ID), Matchers.any());
    }
}