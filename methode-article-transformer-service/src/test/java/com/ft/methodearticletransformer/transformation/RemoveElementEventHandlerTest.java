package com.ft.methodearticletransformer.transformation;


import static com.ft.methodearticletransformer.transformation.StrikeoutEventHandlerRegistry.attributeNameMatcher;
import static com.ft.methodetesting.xml.XmlMatcher.identicalXmlTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import org.codehaus.stax2.XMLEventReader2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;

@RunWith(value=MockitoJUnitRunner.class)
public class RemoveElementEventHandlerTest extends BaseXMLEventHandlerTest {
	private BaseXMLEventHandler eventHandler;

	@Mock private XMLEventReader2 mockXmlEventReader;
	@Mock private BodyWriter eventWriter;
	@Mock private BodyProcessingContext mockBodyProcessingContext;
	@Mock private XMLEventHandler fallbackEventHandler;
    @Mock private FieldTransformer mockTransformer;

    private static final String TRANSACTION_ID = "tid_test";

	@Before
	public void setup() throws Exception {
		eventHandler = new RemoveElementEventHandler(fallbackEventHandler, attributeNameMatcher("channel"));
	}

    @Test
    public void shouldUseFallbackHandlerForStartTagIfAttributesDoNotMatch() throws Exception {
        ImmutableMap<String,String> attributesMap = ImmutableMap.of("title", "!");
        StartElement startElement = getStartElementWithAttributes("p", attributesMap);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        verify(fallbackEventHandler).handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldUseFallbackHandlerForStartTagIfNoAttributes() throws Exception {
        StartElement startElement = getStartElement("p");
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        verify(fallbackEventHandler).handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldAlwaysUseFallBackHandlerForEndTag() throws Exception {
        EndElement endElement = getEndElement("p");
        eventHandler.handleEndElementEvent(endElement, mockXmlEventReader, eventWriter);
        verify(fallbackEventHandler).handleEndElementEvent(endElement, mockXmlEventReader, eventWriter);
    }

	@Test
	public void shouldRemoveStartTagAndContentsUpToMatchingEndTagIfStrikeoutAttributePresent() throws Exception {
		ImmutableMap<String,String> attributesMap = ImmutableMap.of("channel", "!");
		StartElement startElement = getStartElementWithAttributes("span", attributesMap);
		when(mockXmlEventReader.hasNext()).thenReturn(true, true, true, true, true, true, true);
		//"<span channel="!">Some text in <i>italics</i> and some not</span>"
		when(mockXmlEventReader.nextEvent()).thenReturn(getCharacters("Some text in "),
                getStartElement("i"), getCharacters("italics"), getEndElement("i"),
				getCharacters(" and some not"), getEndElement("span"));
		when(mockXmlEventReader.peek()).thenReturn(getCharacters("Some text in "),
                getStartElement("i"), getCharacters("italics"), getEndElement("i"),
				getCharacters(" and some not"), getEndElement("span"));
		eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        verifyZeroInteractions(eventWriter);
	}

    @Test
    public void shouldRemoveStartTagAndContentsUpToMatchingEndTagIfPTagWithChannelAttibuteButNoNestedIFrameElementPresent() throws Exception {
        ImmutableMap<String, String> attributesMap = ImmutableMap.of("channel", "!");
        StartElement startElement = getStartElementWithAttributes("p", attributesMap);
        when(mockXmlEventReader.hasNext()).thenReturn(true, true, true);
        //"<p channel="!">Look here for content</p>"
        when(mockXmlEventReader.nextEvent()).thenReturn(getCharacters("Look here for content"), getEndElement("p"));
        when(mockXmlEventReader.peek()).thenReturn(getCharacters("Look here for content"), getEndElement("p"));
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        verifyZeroInteractions(eventWriter);
    }

    @Test
    public void shouldRemoveStartTagAndContentsUpToMatchingEndTagIfChannelAttibuteAndNestedIFrameElementPresentButHasNoOuterPTag() throws Exception {
        ImmutableMap<String, String> attributesMap = ImmutableMap.of("channel", "!");
        StartElement startElement = getStartElementWithAttributes("span", attributesMap);
        when(mockXmlEventReader.hasNext()).thenReturn(true, true, true);
        //"<span channel="!">Look here <iframe></iframe> for content</span>"
        when(mockXmlEventReader.nextEvent()).thenReturn(getCharacters("Look here "), getStartElement("iframe"), getEndElement("iframe"),getCharacters(" for content"), getEndElement("span"));
        when(mockXmlEventReader.peek()).thenReturn(getCharacters("Look here "), getStartElement("iframe"), getEndElement("iframe"),getCharacters(" for content"), getEndElement("span"));
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        verifyZeroInteractions(eventWriter);
    }


/*    @Test
    public void shouldRetainStartTagAndContentsUpToMatchingEndTagIfPTagWithChannelAttibuteAndNestedIFrameElementPresent() throws Exception {
        ImmutableMap<String, String> attributesMap = ImmutableMap.of("channel", "!");
        StartElement pElement = getStartElementWithAttributes("p", attributesMap);
        StartElement iFrameElement = getStartElement("iframe");
        when(mockXmlEventReader.hasNext()).thenReturn(true, true, true, true, true);
        //"<p channel="!">Look here <iframe></iframe> for content</p>"
        when(mockXmlEventReader.nextEvent()).thenReturn(getCharacters("Look here "),
                getStartElementWithAttributes(iFrameElement.getName().getLocalPart(), Collections.singletonMap("src", "www.youtube.com")), getEndElement(iFrameElement.getName().getLocalPart()),
                getCharacters(" for content"), getEndElement("p"));
        when(mockXmlEventReader.peek()).thenReturn(getCharacters("Look here "),
                getStartElementWithAttributes(iFrameElement.getName().getLocalPart(), Collections.singletonMap("src", "www.youtube.com")), getEndElement(iFrameElement.getName().getLocalPart()),
                getCharacters(" for content"), getEndElement("p"));
        eventHandler.handleStartElementEvent(pElement, mockXmlEventReader, eventWriter, mockBodyProcessingContext);
        //verify(eventWriter).writeStartTag(pElement.getName().getLocalPart(), attributesMap);
        verify(eventWriter).write("Look here ");
        verify(eventWriter).writeStartTag(iFrameElement.getName().getLocalPart(), null);
        verify(eventWriter).writeEndTag(iFrameElement.getName().getLocalPart());
        verify(eventWriter).write(" for content");
        verify(eventWriter).writeEndTag(pElement.getName().getLocalPart());
    }*/


}
