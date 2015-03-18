package com.ft.methodearticletransformer.transformation;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.writer.HTML5VoidElementHandlingXMLBodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import org.codehaus.stax2.XMLOutputFactory2;


/**
 * This class allows you to wrap another eventHandler and insert the contents
 * between another set of specified tags.
 */
public class WrappedHandlerXmlEventHandler extends BaseXMLEventHandler {

    private XMLEventHandler eventHandlerToWrap;

    public WrappedHandlerXmlEventHandler(XMLEventHandler eventHandlerToWrap) {
        this.eventHandlerToWrap = eventHandlerToWrap;
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter, BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        String eventTag = event.getName().toString();
        try {
            eventWriter.writeStartTag(eventTag, noAttributes());
            HTML5VoidElementHandlingXMLBodyWriter writer = new HTML5VoidElementHandlingXMLBodyWriter((XMLOutputFactory2) XMLOutputFactory2.newInstance());
            eventHandlerToWrap.handleStartElementEvent(event, xmlEventReader, writer, bodyProcessingContext);
            String imageOutput = writer.asString();
            eventWriter.writeRaw(imageOutput);
            eventWriter.writeEndTag(eventTag);
        }
        catch(IOException ioe){
            throw new RuntimeException(ioe);
        }
    }

    private Map<String, String> noAttributes() {
        return Collections.emptyMap();
    }
}
