package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.google.common.collect.ImmutableMap;
import org.codehaus.stax2.XMLEventReader2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(value = MockitoJUnitRunner.class)
public class RetainWithSpecificAttributesXMLEventHandlerTest extends BaseXMLEventHandlerTest {

    private RetainWithSpecificAttributesXMLEventHandler eventHandler;

    @Mock
    private XMLEventReader2 mockXmlEventReader;
    @Mock
    private BodyWriter eventWriter;
    @Mock
    private BodyProcessingContext mockBodyProcessingContext;

    @Before
    public void setup() throws Exception {
        eventHandler = new RetainWithSpecificAttributesXMLEventHandler("href");
        Characters text = getCharacters("text");
        when(mockXmlEventReader.peek()).thenReturn(text);
        when(mockXmlEventReader.next()).thenReturn(text);
    }

    @Test
    public void startElementShouldBeOutput() throws Exception {
        StartElement startElement = getStartElement("a");
        ImmutableMap<String, String> expectedMap = ImmutableMap.of();
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        verify(eventWriter).writeStartTag(startElement.getName().getLocalPart(), expectedMap);
    }

    @Test
    public void startElementShouldBeOutputWithSpecificAttributes() throws Exception {
        ImmutableMap<String, String> attributesMap = ImmutableMap.of("key", "value", "href", "value2");
        ImmutableMap<String, String> expectedMap = ImmutableMap.of("href", "value2");

        StartElement startElement = getStartElementWithAttributes("a", attributesMap);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        verify(eventWriter).writeStartTag(startElement.getName().getLocalPart(), expectedMap);
    }

    @Test
    public void endElementShouldBeOutput() throws Exception {
        EndElement endElement = getEndElement("a");
        eventHandler.handleEndElementEvent(endElement, mockXmlEventReader, eventWriter);
        verify(eventWriter).writeEndTag(endElement.getName().getLocalPart());
    }
}