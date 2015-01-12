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

public class MethodeVideoXmlEventHandler extends BaseXMLEventHandler {
    private static final String NEW_ELEMENT = "a";
    private static final String NEW_ELEMENT_ATTRIBUTE = "href";
    private String videoIdAttributeName;
    private Map<String, String> attributesToAdd;

    private Map<String, String> sourceToUrlMap;

    public MethodeVideoXmlEventHandler(String videoIdAttributeName) {
        this.videoIdAttributeName = videoIdAttributeName;
        sourceToUrlMap = new HashMap<String, String>();
        sourceToUrlMap.put("brightcove", "http://video.ft.com/%s");
        sourceToUrlMap.put("youTube", "%s");
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        String source;
        if(("iframe").equals(event.getName().toString())) {
            source = "youTube";
        }
        else{
            source = "brightcove";
        }

        String id = extractVideoId(event);
        String videoUrl = String.format(sourceToUrlMap.get(source), id);
        attributesToAdd = new HashMap<String, String>();
        attributesToAdd.put(NEW_ELEMENT_ATTRIBUTE, videoUrl);

        eventWriter.writeStartTag(NEW_ELEMENT, attributesToAdd);
    }

    private String extractVideoId(StartElement event) {
        return event.getAttributeByName(new QName(videoIdAttributeName)).getValue();
    }

    @Override
    public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
        eventWriter.writeEndTag(NEW_ELEMENT);
    }


}
