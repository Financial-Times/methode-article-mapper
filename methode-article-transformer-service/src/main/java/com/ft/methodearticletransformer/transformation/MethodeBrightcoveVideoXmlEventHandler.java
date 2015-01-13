package com.ft.methodearticletransformer.transformation;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

public class MethodeBrightcoveVideoXmlEventHandler extends BaseXMLEventHandler {
    private static final String NEW_ELEMENT = "a";
    private static final String NEW_ELEMENT_ATTRIBUTE = "href";
    private static final String VIDEO_URL = "http://video.ft.com/%s";
    private final XMLEventHandler fallbackHandler;
    private String videoIdAttributeName;
    private Map<String, String> attributesToAdd;

    public MethodeBrightcoveVideoXmlEventHandler(String videoIdAttributeName, XMLEventHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
        this.videoIdAttributeName = videoIdAttributeName;
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        String id = extractVideoId(event);
        if(id == null){
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
        }
        String videoUrl = String.format(VIDEO_URL, id);
        attributesToAdd = new HashMap<String, String>();
        attributesToAdd.put(NEW_ELEMENT_ATTRIBUTE, videoUrl);
        eventWriter.writeStartTag(NEW_ELEMENT, attributesToAdd);
    }

    private String extractVideoId(StartElement event) {
        return event.getAttributeByName(QName.valueOf(videoIdAttributeName)).getValue();
    }

    @Override
    public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
        eventWriter.writeEndTag(NEW_ELEMENT);
    }
}
