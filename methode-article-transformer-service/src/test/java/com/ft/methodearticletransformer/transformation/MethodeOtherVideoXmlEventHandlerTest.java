package com.ft.methodearticletransformer.transformation;

import static org.mockito.Mockito.verify;

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
    private static final String IFRAME = "iframe";
    private static final String SRC = "src";
    private static final String DATA_EMBEDDED = "data-embedded";
    private static final String TRUE = "true";
    private static final String DATA_ASSET_TYPE = "data-asset-type";
    private static final String VIDEO = "video";
    private static final String NOT_VIDEO_LINK = "http://www.bbc.co.uk/news";
    private static final String YOUTUBE = "http://www.youtube.com/embed/OQzJR3BqS7o";
    private static final String STANDARDIZED_YOUTUBE = "https://www.youtube.com/watch?v=OQzJR3BqS7o";
    private static final String VIMEO = "//player.vimeo.com/video/40234826";
    private static final String STANDARDIZED_VIMEO = "http://www.vimeo.com/40234826";
    private static final String DAILYMOTION = "http://www.dailymotion.com/video/x2gsis0_the-best-of-the-2015-grammys_lifestyle";

    @Mock private XMLEventHandler fallbackHandler;
    @Mock private XMLEventReader mockXmlEventReader;
    @Mock private BodyWriter mockBodyWriter;
    @Mock private BodyProcessingContext mockBodyProcessingContext;

    @Before
    public void setup() {
        eventHandler = new MethodeOtherVideoXmlEventHandler(fallbackHandler);
    }

    @Test
    public void shouldFallBackIfElementHasNoSourceAttribute() throws Exception {
        StartElement startElement = getStartElement(IFRAME);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(fallbackHandler).handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldFallBackIfSrcIsNotVideo() throws Exception {
        Map<String, String> startElementAttributes = new HashMap<>();
        startElementAttributes.put(SRC, NOT_VIDEO_LINK);
        StartElement startElement = getStartElementWithAttributes(IFRAME, startElementAttributes);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(fallbackHandler).handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldFallbackIfVideoDoesNotMatchYoutubeAndVimeo() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(SRC, DAILYMOTION);

        StartElement startElement = getStartElementWithAttributes(IFRAME, firstAttributes);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(fallbackHandler).handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldWriteTransformedVimeoContentToWriter() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(SRC, VIMEO);

        Map<String, String> finalAttributes = new HashMap<>();
        finalAttributes.put(HREF_ATTRIBUTE, STANDARDIZED_VIMEO);
        finalAttributes.put(DATA_EMBEDDED, TRUE);
        finalAttributes.put(DATA_ASSET_TYPE, VIDEO);

        StartElement firstElement = getStartElementWithAttributes(IFRAME, firstAttributes);
        eventHandler.handleStartElementEvent(firstElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(TRANSFORMED_ELEMENT, finalAttributes);
        verify(mockBodyWriter).writeEndTag(TRANSFORMED_ELEMENT);
    }

    @Test
    public void shouldWriteTransformedYoutubeContentToWriter() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(SRC, YOUTUBE);

        Map<String, String> finalAttributes = new HashMap<>();
        finalAttributes.put(HREF_ATTRIBUTE, STANDARDIZED_YOUTUBE);
        finalAttributes.put(DATA_EMBEDDED, TRUE);
        finalAttributes.put(DATA_ASSET_TYPE, VIDEO);

        StartElement firstElement = getStartElementWithAttributes(IFRAME, firstAttributes);

        eventHandler.handleStartElementEvent(firstElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(TRANSFORMED_ELEMENT, finalAttributes);
        verify(mockBodyWriter).writeEndTag(TRANSFORMED_ELEMENT);
    }
}
