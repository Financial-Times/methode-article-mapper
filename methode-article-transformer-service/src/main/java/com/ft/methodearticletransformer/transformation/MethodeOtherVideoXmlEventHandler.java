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
import javax.xml.stream.events.XMLEvent;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

public class MethodeOtherVideoXmlEventHandler extends BaseXMLEventHandler {
    private static final String PARAGRAPH_TAG = "p";
    private static final String NEW_ELEMENT = "a";
    private static final String NEW_ELEMENT_ATTRIBUTE = "href";
    private final String targetedHtmlClass;
    private final XMLEventHandler fallbackHandler;
    private List<String> validRegexes; //because all 3rd party content is put into iframes we need to know which ones we want to keep.


    public MethodeOtherVideoXmlEventHandler(String targetedHtmlClass, XMLEventHandler fallbackHandler) {
        this.targetedHtmlClass = targetedHtmlClass;
        this.fallbackHandler = fallbackHandler;
        validRegexes = new ArrayList<String>();
        validRegexes.add("^(https?:)?(\\/\\/)?player.vimeo.com/video/[\\S]+");
        validRegexes.add("^(https?:)?(\\/\\/)?www.youtube.com/embed/[\\S]+");
    }

    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

        if (!isTargetedClass(event)) {
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }

        XMLEvent found = getEventAndSkipBlock(xmlEventReader, event.getName().toString(), "iframe", "src", validRegexes);
        if(found != null) {

            Attribute srcAttribute = found.asStartElement().getAttributeByName(QName.valueOf("src"));

            String videoLink =  srcAttribute.getValue();
            Map<String, String> attributesToAdd = new HashMap<String, String>();
            attributesToAdd.put(NEW_ELEMENT_ATTRIBUTE, videoLink);

            eventWriter.writeStartTag(PARAGRAPH_TAG, null);
            eventWriter.writeStartTag(NEW_ELEMENT, attributesToAdd);
            eventWriter.writeEndTag(NEW_ELEMENT);
            eventWriter.writeEndTag(PARAGRAPH_TAG);

        }
        // if this fails it has skipped the whole block by default anyway. If otherwise a secondary fallback is required for p + channel.
        // It shouldn't go to fallback to the fallbackHandler because that is p with no channel

    }

    private XMLEvent getEventAndSkipBlock(XMLEventReader reader, String primaryElementName, String secondaryElementName,
                                                     String secondaryElementAttributeName, List<String> secondaryElementAttributeValueRegex)
                                                     throws XMLStreamException {
        XMLEvent xmlEvent = null;
        int primaryOpenElementNameCount = 1;
        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.nextEvent();
            if (nextEvent.isStartElement()) {
                StartElement newStartElement = nextEvent.asStartElement();
                if((primaryElementName).equals(newStartElement.getName().getLocalPart())) {
                    primaryOpenElementNameCount++;
                }

                if ((secondaryElementName).equals(newStartElement.getName().getLocalPart())) {
                    Attribute attribute = newStartElement.getAttributeByName(QName.valueOf(secondaryElementAttributeName));
                    for (String regex :secondaryElementAttributeValueRegex){
                        if (Pattern.matches(regex, attribute.getValue())) {
                            xmlEvent = nextEvent;
                        }
                    }
                }
            }
            if(nextEvent.isEndElement()){
                EndElement newEndElement = nextEvent.asEndElement();
                if ((primaryElementName).equals(newEndElement.getName().getLocalPart()) ) {
                    if(primaryOpenElementNameCount ==1){
                        return xmlEvent;
                    }
                    primaryOpenElementNameCount--;
                }

            }
        }
        throw new BodyProcessingException("Reached end without encountering closing primary tag : " + primaryElementName);

    }

    private boolean isTargetedClass(StartElement event) {
        Attribute classesAttr = event.getAttributeByName(QName.valueOf(targetedHtmlClass));
        return  classesAttr!=null;
    }

    @Override
    public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
        //only a fallback one should hit this code.
        fallbackHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
    }

}
