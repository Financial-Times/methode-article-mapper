package com.ft.methodearticletransformer.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

public class MethodeOtherVideoXmlEventHandler extends BaseXMLEventHandler {
    private static final String PARAGRAPH_TAG = "p";
    private static final String NEW_ELEMENT = "a";
    private static final String NEW_ELEMENT_ATTRIBUTE = "href";
    private static final String DATA_EMBEDDED = "data-embedded";
    private static final String DATA_EMBEDDED_VALUE = "true";
    private static final String DATA_ASSET_TYPE = "data-asset-type";
    private static final String DATA_ASSET_TYPE_VALUE = "video";
    private final XMLEventHandler fallbackHandler;
    private List<String> validRegexes; //because all 3rd party content is put into iframes we need to know which ones we want to keep.


    public MethodeOtherVideoXmlEventHandler(XMLEventHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
        validRegexes = new ArrayList<>();
        validRegexes.add("^(https?:)?(\\/\\/)?player.vimeo.com/video/[\\S]+");
        validRegexes.add("^(https?:)?(\\/\\/)?www.youtube.com/embed/[\\S]+");
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

        Attribute srcAttribute = event.asStartElement().getAttributeByName(QName.valueOf("src"));

        if(srcAttribute == null){
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }

        boolean matches = false;
        for (String regex : validRegexes){
            if (Pattern.matches(regex, srcAttribute.getValue())) {
                matches=true;
                break;
            }
        }
        if(!matches){
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }

        String videoLink =  srcAttribute.getValue();
        Map<String, String> attributesToAdd = new HashMap<String, String>();
        attributesToAdd.put(NEW_ELEMENT_ATTRIBUTE, videoLink);
        attributesToAdd.put(DATA_EMBEDDED, DATA_EMBEDDED_VALUE);
        attributesToAdd.put(DATA_ASSET_TYPE, DATA_ASSET_TYPE_VALUE);

        eventWriter.writeStartTag(NEW_ELEMENT, attributesToAdd);
        eventWriter.writeEndTag(NEW_ELEMENT);

    }

    @Override
    public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
        //only a fallback one should hit this code.
        fallbackHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
    }

}
