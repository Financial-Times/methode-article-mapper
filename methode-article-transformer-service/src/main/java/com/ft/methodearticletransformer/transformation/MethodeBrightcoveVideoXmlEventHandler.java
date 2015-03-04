package com.ft.methodearticletransformer.transformation;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.google.common.base.Strings;

public class MethodeBrightcoveVideoXmlEventHandler extends BaseXMLEventHandler {

    private static final String NEW_ELEMENT = "a";
    private static final String NEW_ELEMENT_ATTRIBUTE = "href";
    private static final String VIDEO_URL = "http://video.ft.com/%s";
    private static final String DATA_EMBEDDED = "data-embedded";
    private static final String TRUE = "true";
    private static final String DATA_ASSET_TYPE = "data-asset-type";
    private static final String VIDEO = "video";
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
        if(Strings.isNullOrEmpty(id)){
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }
        String videoUrl = String.format(VIDEO_URL, id);
        attributesToAdd = new HashMap<>();
        attributesToAdd.put(NEW_ELEMENT_ATTRIBUTE, videoUrl);
        attributesToAdd.put(DATA_EMBEDDED, TRUE);
        attributesToAdd.put(DATA_ASSET_TYPE, VIDEO);
        skipUntilMatchingEndTag(event.getName().toString(), xmlEventReader);
        eventWriter.writeStartTag(NEW_ELEMENT, attributesToAdd);
        eventWriter.writeEndTag(NEW_ELEMENT);
    }

    private String extractVideoId(StartElement event) {
        Attribute attribute = event.getAttributeByName(QName.valueOf(videoIdAttributeName));
        if(attribute == null){
            return null;
        }
        return event.getAttributeByName(QName.valueOf(videoIdAttributeName)).getValue();
    }

}
