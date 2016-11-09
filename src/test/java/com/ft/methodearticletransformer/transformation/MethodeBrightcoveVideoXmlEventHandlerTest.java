package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MethodeBrightcoveVideoXmlEventHandlerTest extends BaseXMLEventHandlerTest {

    private MethodeBrightcoveVideoXmlEventHandler eventHandler;
    @Mock private XMLEventHandler fallbackHandler;

    @Mock private XMLEventReader mockXmlEventReader;
    @Mock private BodyWriter mockBodyWriter;
    @Mock private BodyProcessingContext mockBodyProcessingContext;

    private static final String VIDEO_PLAYER_ELEMENT = "videoPlayer";
    private static final String VIDEO_ID_ATTRIBUTE_NAME = "videoID";
    private static final String VIDEO_ID = "3920663836001";
    private static final String DATA_EMBEDDED = "data-embedded";
    private static final String VIDEO_TYPE = "http://www.ft.com/ontology/content/MediaResource";
    private static final String CONTENT_TAG = "content";
    private static final String ID="id";
    private static final String TYPE="type";

    @Before
    public void setup() throws Exception  {
        eventHandler = new MethodeBrightcoveVideoXmlEventHandler(VIDEO_ID_ATTRIBUTE_NAME, fallbackHandler);
    }

    @Test
    public void shouldUseFallbackHandlerIfStartElementVideoIdAttributeValuesAreNull() throws Exception {
        StartElement startElement = getStartElement(VIDEO_PLAYER_ELEMENT);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(fallbackHandler).handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldUseFallbackHandlerIfStartElementVideoIdAttributeValuesAreEmpty() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("", "");
        StartElement startElement = getStartElementWithAttributes(VIDEO_PLAYER_ELEMENT, attributes);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(fallbackHandler).handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldWriteTransformedElementsToWriter() throws Exception {
        Map<String, String> videoPlayerAttributes = new HashMap<>();
        videoPlayerAttributes.put(VIDEO_ID_ATTRIBUTE_NAME, VIDEO_ID);
        Map<String, String> transformedAttributes = new HashMap<>();
        transformedAttributes.put(ID, UUID.nameUUIDFromBytes(VIDEO_ID.getBytes()).toString());
        transformedAttributes.put(DATA_EMBEDDED, Boolean.TRUE.toString());
        transformedAttributes.put(TYPE, VIDEO_TYPE);
        StartElement startElement = getStartElementWithAttributes(VIDEO_PLAYER_ELEMENT, videoPlayerAttributes);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(CONTENT_TAG, transformedAttributes);
        verify(mockBodyWriter).writeEndTag(CONTENT_TAG);
    }

}
