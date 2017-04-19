package com.ft.methodearticlemapper.transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

public class MethodeVideoXmlEventHandler extends BaseXMLEventHandler {

    private static final String CONTENT_TAG = "content";
    private static final String DATA_EMBEDDED = "data-embedded";
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String VIDEO_TYPE = "http://www.ft.com/ontology/content/MediaResource";
    private static final String UUID_REGEX = "[0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}";
    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

    private final XMLEventHandler fallbackHandler;
    private final String videoIdAttributeName;

    public MethodeVideoXmlEventHandler(String videoIdAttributeName, XMLEventHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
        this.videoIdAttributeName = videoIdAttributeName;
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        String videoId = extractVideoId(event);
        if(Strings.isNullOrEmpty(videoId)){
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }
        Map<String, String> attributesToAdd = new HashMap<>();
        attributesToAdd.put(ID, getAsUUID(videoId));
        attributesToAdd.put(DATA_EMBEDDED, Boolean.TRUE.toString());
        attributesToAdd.put(TYPE, VIDEO_TYPE);
        skipUntilMatchingEndTag(event.getName().toString(), xmlEventReader);
        eventWriter.writeStartTag(CONTENT_TAG, attributesToAdd);
        eventWriter.writeEndTag(CONTENT_TAG);
    }

    private String extractVideoId(StartElement event) {
        Attribute attribute = event.getAttributeByName(QName.valueOf(videoIdAttributeName));
        if(attribute == null){
            return null;
        }
        return event.getAttributeByName(QName.valueOf(videoIdAttributeName)).getValue();
    }

    private String getAsUUID(String videoId) {
        Matcher matcher = UUID_PATTERN.matcher(videoId);
        return matcher.matches() ? videoId : UUID.nameUUIDFromBytes(videoId.getBytes()).toString();
    }
}
