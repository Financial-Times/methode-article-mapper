package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import static com.google.common.base.Preconditions.checkArgument;

public class StrikeoutXMLEventHandler extends BaseXMLEventHandler {

    private final XMLEventHandler fallbackEventHandler;
    private final StartElementMatcher matcher;
    private static final String FTCOM = "FTcom";
    private static final String NOT_NEWSPAPER = "!Financial Times";

    protected StrikeoutXMLEventHandler(final XMLEventHandler fallbackEventHandler, final StartElementMatcher matcher) {
        checkArgument(fallbackEventHandler != null, "fallbackEventHandler cannot be null");
        checkArgument(matcher != null, "matcher cannot be null");
        this.fallbackEventHandler = fallbackEventHandler;
        this.matcher = matcher;
    }

    @Override
    public void handleStartElementEvent(final StartElement event, final XMLEventReader xmlEventReader, final BodyWriter eventWriter,
                                        final BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        if (!matcher.matches(event)) {
            fallbackEventHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }

        Attribute channelAttribute = event.asStartElement().getAttributeByName(QName.valueOf("channel"));
        String channelAttributeString = channelAttribute.getValue();
        final String nameToMatch = event.getName().getLocalPart();

        if ((FTCOM.equals(channelAttributeString)) || (NOT_NEWSPAPER.equals(channelAttributeString))) {
            fallbackEventHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        } else {
            skipUntilMatchingEndTag(nameToMatch, xmlEventReader);
        }
    }

    @Override // Only called where the start tag used the fallback event handler
    public void handleEndElementEvent(final EndElement event, final XMLEventReader xmlEventReader, final BodyWriter eventWriter) throws XMLStreamException {
        fallbackEventHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
    }

}

