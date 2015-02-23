package com.ft.methodearticletransformer.transformation;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MethodeOtherVideoXmlEventHandlerTest extends BaseXMLEventHandlerTest {

    private MethodeOtherVideoXmlEventHandler eventHandler;

    private static final String TRANSFORMED_ELEMENT = "a";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String STARTING_ELEMENT_NAME = "iframe";
    private static final String ELEMENT_ATTRIBUTE_NAME = "src";
    private static final String DATA_EMBEDDED = "data-embedded";
    private static final String DATA_EMBEDDED_VALUE = "true";
    private static final String DATA_ASSET_TYPE = "data-asset-type";
    private static final String DATA_ASSET_TYPE_VALUE = "video";
    private static final String YOUTUBE = "http://www.youtube.com/embed/OQzJR3BqS7o";
    private static final String VIMEO = "http://player.vimeo.com/video/40234826";
    private static final String DAILYMOTION = "http://www.dailymotion.com/video/x2gsis0_the-best-of-the-2015-grammys_lifestyle";

    @Mock private XMLEventHandler mockFallbackHandler;
    @Mock private XMLEventReader mockXmlEventReader;
    @Mock private BodyWriter mockBodyWriter;
    @Mock private BodyProcessingContext mockBodyProcessingContext;

    @Before
    public void setup() {
        eventHandler = new MethodeOtherVideoXmlEventHandler(mockFallbackHandler);
    }

    @Test
    public void shouldExitIfVideoDoesNotMatchYoutubeAndVimeo() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(ELEMENT_ATTRIBUTE_NAME, DAILYMOTION);

        Map<String, String> finalAttributes = new HashMap<>();
        finalAttributes.put(HREF_ATTRIBUTE, DAILYMOTION);

        StartElement firstElement = getStartElementWithAttributes(STARTING_ELEMENT_NAME, firstAttributes);

        when(mockXmlEventReader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true);
        eventHandler.handleStartElementEvent(firstElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter, times(0)).writeStartTag(HREF_ATTRIBUTE, finalAttributes);
    }

    @Test
    public void shouldWriteTransformedVimeoContentToWriter() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(ELEMENT_ATTRIBUTE_NAME, VIMEO);

        Map<String, String> finalAttributes = new HashMap<>();
        finalAttributes.put(HREF_ATTRIBUTE, VIMEO);
        finalAttributes.put(DATA_EMBEDDED, DATA_EMBEDDED_VALUE);
        finalAttributes.put(DATA_ASSET_TYPE, DATA_ASSET_TYPE_VALUE);

        StartElement firstElement = getStartElementWithAttributes(STARTING_ELEMENT_NAME, firstAttributes);

        when(mockXmlEventReader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true);
        eventHandler.handleStartElementEvent(firstElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(TRANSFORMED_ELEMENT, finalAttributes);
        verify(mockBodyWriter).writeEndTag(TRANSFORMED_ELEMENT);
    }

    @Test
    public void shouldWriteTransformedYoutubeContentToWriter() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(ELEMENT_ATTRIBUTE_NAME, YOUTUBE);

        Map<String, String> finalAttributes = new HashMap<>();
        finalAttributes.put(HREF_ATTRIBUTE, YOUTUBE);
        finalAttributes.put(DATA_EMBEDDED, DATA_EMBEDDED_VALUE);
        finalAttributes.put(DATA_ASSET_TYPE, DATA_ASSET_TYPE_VALUE);

        StartElement firstElement = getStartElementWithAttributes(STARTING_ELEMENT_NAME, firstAttributes);

        when(mockXmlEventReader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true);

        eventHandler.handleStartElementEvent(firstElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(TRANSFORMED_ELEMENT, finalAttributes);
        verify(mockBodyWriter).writeEndTag(TRANSFORMED_ELEMENT);
    }
}
