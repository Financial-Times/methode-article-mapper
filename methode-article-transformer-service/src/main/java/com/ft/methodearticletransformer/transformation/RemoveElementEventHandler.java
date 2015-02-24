package com.ft.methodearticletransformer.transformation;

import static com.google.common.base.Preconditions.checkArgument;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class RemoveElementEventHandler extends BaseXMLEventHandler {
	private final XMLEventHandler fallbackEventHandler;
	private final StartElementMatcher matcher;

    private static final String PARAGRAPH_TAG = "p";

	protected RemoveElementEventHandler(final XMLEventHandler fallbackEventHandler, final StartElementMatcher matcher) {
		checkArgument(fallbackEventHandler != null, "fallbackEventHandler cannot be null");
		checkArgument(matcher != null, "matcher cannot be null");
		this.fallbackEventHandler = fallbackEventHandler;
		this.matcher = matcher;
	}

	@Override
	public void handleStartElementEvent(final StartElement event, final XMLEventReader xmlEventReader, final BodyWriter eventWriter,
			final BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        if(!matcher.matches(event)) {
            fallbackEventHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }
        final String nameToMatch = event.getName().getLocalPart();
        if(!(PARAGRAPH_TAG.equals(nameToMatch))) {
            skipUntilMatchingEndTag(nameToMatch, xmlEventReader);
            return;
        }
        List<XMLEvent> eventList = makeListOfXMLEventsAndSkip(event, xmlEventReader, event.getName().toString(), "iframe");
        if (eventList != null) {
            try {
                writeSavedEventList(eventList, eventWriter);
            } catch (IOException e) {
                throw new TransformationException(e);
            }
        }
	}

    @Override // Only called where the start tag used the fallback event handler
	public void handleEndElementEvent(final EndElement event, final XMLEventReader xmlEventReader, final BodyWriter eventWriter) throws XMLStreamException {
		fallbackEventHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
	}

    private List<XMLEvent> makeListOfXMLEventsAndSkip(StartElement event, XMLEventReader xmlEventReader, String primaryElementName, String secondaryElementName) throws XMLStreamException {
        List<XMLEvent> xmlEventList = new ArrayList<>();
        boolean containsSecondaryElement = false;
        int primaryOpenElementNameCount = 1;
        xmlEventList.add(event);
        while(xmlEventReader.hasNext()) {
            XMLEvent nextEvent = xmlEventReader.nextEvent();
            if(nextEvent.isStartElement()) {
                StartElement newStartElement = nextEvent.asStartElement();
                xmlEventList.add(newStartElement);
                if ((primaryElementName).equals(newStartElement.getName().getLocalPart())) {
                    primaryOpenElementNameCount++;
                }
                if ((secondaryElementName).equals(newStartElement.getName().getLocalPart())) {
                    containsSecondaryElement = true;
                }
            }
            if(nextEvent.isCharacters()) {
                Characters chars = nextEvent.asCharacters();
                xmlEventList.add(chars);
            }
            if(nextEvent.isEndElement()) {
                EndElement newEndElement = nextEvent.asEndElement();
                xmlEventList.add(newEndElement);
                if((primaryElementName).equals(newEndElement.getName().getLocalPart())) {
                    if(primaryOpenElementNameCount == 1) {
                        if(containsSecondaryElement) {
                            return xmlEventList;
                        }
                        return null;
                    }
                    primaryOpenElementNameCount--;
                }
            }
        }
        throw new BodyProcessingException("Reached end without encountering closing primary tag: " + primaryElementName);
    }

    private void writeSavedEventList(List<XMLEvent> eventList, BodyWriter eventWriter) throws XMLStreamException, IOException{
        Writer writer = new StringWriter();
        for (XMLEvent event : eventList) {
            event.writeAsEncodedUnicode(writer);
        }
        eventWriter.writeRaw(writer.toString());
    }

}
