package com.ft.methodearticletransformer.transformation;

import static com.google.common.base.Preconditions.checkArgument;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

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
        } else {
            XMLEvent found = getEventAndSkipBlock(xmlEventReader, event.getName().toString(), "iframe");
            final String nameToMatch = event.getName().getLocalPart();
            if (nameToMatch == PARAGRAPH_TAG && found != null) {
                fallbackEventHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            } else {
                skipUntilMatchingEndTag(nameToMatch, xmlEventReader);
            }
        }
	}
	
	@Override // Only called where the start tag used the fallback event handler
	public void handleEndElementEvent(final EndElement event, final XMLEventReader xmlEventReader, final BodyWriter eventWriter) throws XMLStreamException {
		fallbackEventHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
	}

    private XMLEvent getEventAndSkipBlock(XMLEventReader xmlEventReader, String primaryElementName, String secondaryElementName) throws XMLStreamException {
        XMLEvent xmlEvent = null;
        int primaryOpenElementNameCount = 1;
        while (xmlEventReader.hasNext()) {
            XMLEvent nextEvent = xmlEventReader.nextEvent();
            if(nextEvent.isStartElement()) {
                StartElement newStartElement = nextEvent.asStartElement();
                if((primaryElementName).equals(newStartElement.getName().getLocalPart())) {
                    primaryOpenElementNameCount++;
                }

                if((secondaryElementName).equals(newStartElement.getName().getLocalPart())) {
                    xmlEvent = nextEvent;
                }
            }
            if(nextEvent.isEndElement()) {
                EndElement newEndElement = nextEvent.asEndElement();
                if((primaryElementName).equals(newEndElement.getName().getLocalPart())) {
                    if(primaryOpenElementNameCount == 1) {
                        return xmlEvent;
                    }
                    primaryOpenElementNameCount--;
                }
            }
        }
        throw new BodyProcessingException("Reached end without encountering closing primary tag: " + primaryElementName);
    }
}
