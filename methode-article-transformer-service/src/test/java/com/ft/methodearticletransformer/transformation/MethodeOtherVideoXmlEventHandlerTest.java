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
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MethodeOtherVideoXmlEventHandlerTest extends BaseXMLEventHandlerTest {

    private MethodeOtherVideoXmlEventHandler eventHandler;

    private static final String NEW_ELEMENT = "p";
    private static final String TRANSFORMED_ELEMENT = "a";
    private static final String NEW_ELEMENT_ATTRIBUTE = "href";
    private static final String INCORRECT_NEW_ELEMENT = "table";
    private static final String TARGETED_CLASS = "channel";
    private static final String TARGETED_CLASS_ATTRIBUTE = "ft.com";
    private static final String SECONDARY_ELEMENT_NAME = "iframe";
    private static final String SECONDARY_ELEMENT_ATTRIBUTE_NAME = "src";
    private static final String SECONDARY_ELEMENT_ATTRIBUTE_VALUE_YOUTUBE = "http://www.youtube.com/embed/OQzJR3BqS7o";
    private static final String SECONDARY_ELEMENT_ATTRIBUTE_VALUE_VIMEO = "http://player.vimeo.com/video/40234826";

    @Mock private XMLEventHandler mockFallbackHandler;
    @Mock private XMLEventReader mockXmlEventReader;
    @Mock private BodyWriter mockBodyWriter;
    @Mock private BodyProcessingContext mockBodyProcessingContext;

    @Before
    public void setup() {
        eventHandler = new MethodeOtherVideoXmlEventHandler(TARGETED_CLASS, mockFallbackHandler);
    }

    @Test
    public void shouldUseFallbackHandlerIfTargetedClassIsNotPresent() throws Exception {
        StartElement startElement = getStartElement(INCORRECT_NEW_ELEMENT);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockFallbackHandler).handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldExitIfVideoDoesNotMatchRegex() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(TARGETED_CLASS, TARGETED_CLASS_ATTRIBUTE);
        StartElement startElement = getStartElementWithAttributes(NEW_ELEMENT, attributes);
        EndElement endElement = getEndElement(NEW_ELEMENT);
        when(mockXmlEventReader.hasNext()).thenReturn(true);
        when(mockXmlEventReader.nextEvent()).thenReturn(endElement);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter, times(0)).writeStartTag(NEW_ELEMENT_ATTRIBUTE, attributes);
    }

    @Test
    public void shouldWriteTransformedVimeoContentToWriter() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(TARGETED_CLASS, TARGETED_CLASS_ATTRIBUTE);
        Map<String, String> secondAttributes = new HashMap<>();
        secondAttributes.put(SECONDARY_ELEMENT_ATTRIBUTE_NAME, SECONDARY_ELEMENT_ATTRIBUTE_VALUE_VIMEO);
        Map<String, String> finalAttributes = new HashMap<>();
        finalAttributes.put(NEW_ELEMENT_ATTRIBUTE, SECONDARY_ELEMENT_ATTRIBUTE_VALUE_VIMEO);

        StartElement firstElement = getStartElementWithAttributes(NEW_ELEMENT, firstAttributes);
        StartElement secondaryElement = getStartElementWithAttributes(SECONDARY_ELEMENT_NAME, secondAttributes);
        EndElement secondEndElement = getEndElement(SECONDARY_ELEMENT_NAME);
        EndElement firstEndElement = getEndElement(NEW_ELEMENT);

        when(mockXmlEventReader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(true);
        when(mockXmlEventReader.nextEvent()).thenReturn(firstElement).thenReturn(secondaryElement).thenReturn(secondEndElement).thenReturn(firstEndElement);
        eventHandler.handleStartElementEvent(firstElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(NEW_ELEMENT, null);
        verify(mockBodyWriter).writeStartTag(TRANSFORMED_ELEMENT, finalAttributes);
        verify(mockBodyWriter).writeEndTag(TRANSFORMED_ELEMENT);
        verify(mockBodyWriter).writeEndTag(NEW_ELEMENT);
    }

    @Test
    public void shouldWriteTransformedYoutubeContentToWriter() throws Exception {
        Map<String, String> firstAttributes = new HashMap<>();
        firstAttributes.put(TARGETED_CLASS, TARGETED_CLASS_ATTRIBUTE);
        Map<String, String> secondAttributes = new HashMap<>();
        secondAttributes.put(SECONDARY_ELEMENT_ATTRIBUTE_NAME, SECONDARY_ELEMENT_ATTRIBUTE_VALUE_YOUTUBE);
        Map<String, String> finalAttributes = new HashMap<>();
        finalAttributes.put(NEW_ELEMENT_ATTRIBUTE, SECONDARY_ELEMENT_ATTRIBUTE_VALUE_YOUTUBE);

        StartElement firstElement = getStartElementWithAttributes(NEW_ELEMENT, firstAttributes);
        StartElement secondaryElement = getStartElementWithAttributes(SECONDARY_ELEMENT_NAME, secondAttributes);
        EndElement secondEndElement = getEndElement(SECONDARY_ELEMENT_NAME);
        EndElement firstEndElement = getEndElement(NEW_ELEMENT);

        when(mockXmlEventReader.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(true);
        when(mockXmlEventReader.nextEvent()).thenReturn(firstElement).thenReturn(secondaryElement).thenReturn(secondEndElement).thenReturn(firstEndElement);
        eventHandler.handleStartElementEvent(firstElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(NEW_ELEMENT, null);
        verify(mockBodyWriter).writeStartTag(TRANSFORMED_ELEMENT, finalAttributes);
        verify(mockBodyWriter).writeEndTag(TRANSFORMED_ELEMENT);
        verify(mockBodyWriter).writeEndTag(NEW_ELEMENT);
    }
}
