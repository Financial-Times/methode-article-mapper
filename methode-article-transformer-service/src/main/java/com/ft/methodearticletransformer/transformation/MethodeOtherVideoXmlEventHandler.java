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
import com.sun.xml.internal.stream.events.EndElementEvent;
import com.sun.xml.internal.stream.events.StartElementEvent;
import org.codehaus.stax2.ri.evt.CharactersEventImpl;

public class MethodeOtherVideoXmlEventHandler extends BaseXMLEventHandler {

    private final String targetedHtmlClass;

    public MethodeOtherVideoXmlEventHandler(String targetedHtmlClass, XMLEventHandler fallbackHandler) {
        this.targetedHtmlClass = targetedHtmlClass;
    }


    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

        if (!isTargetedClass(event)) {
            return;
        }

        List<EventsAndAttributes> potentialEvents = new ArrayList<EventsAndAttributes>();

        StartElementEvent startElementEvent = new StartElementEvent(QName.valueOf("p"));
        EndElementEvent endElementEvent = new EndElementEvent(QName.valueOf("p"));
        EventsAndAttributes pStart = new EventsAndAttributes(startElementEvent, null);
        EventsAndAttributes pEnd = new EventsAndAttributes(endElementEvent, null);
        potentialEvents.add(pStart);


        if (xmlEventReader.hasNext()) {
            XMLEvent peekEvent = xmlEventReader.peek();
            if(peekEvent.isCharacters()) {
                XMLEvent nextEvent = xmlEventReader.nextEvent();
                if (nextEvent.isCharacters()) {
                    potentialEvents.add(new EventsAndAttributes(nextEvent.asCharacters(), null));
                }
            }
        }

        XMLEvent found = skipBlockUnlessConditionSatisfied(xmlEventReader, "p", "iframe", "src", "^http://[.a-zA-Z0-9/?=]*");
        if(found != null) {

            Attribute srcAttribute = found.asStartElement().getAttributeByName(QName.valueOf("src"));

            String videoLink =  srcAttribute.getValue();
            Map<String, String> attributesToAdd = new HashMap<>();
            attributesToAdd.put("href", videoLink);


            StartElementEvent aStartElementEvent = new StartElementEvent(QName.valueOf("a"));
            EndElementEvent aEndElementEvent = new EndElementEvent(QName.valueOf("a"));

            potentialEvents.add(new EventsAndAttributes(aStartElementEvent, attributesToAdd));
            potentialEvents.add(new EventsAndAttributes(aEndElementEvent, null));
            potentialEvents.add(pEnd);

            writePotentialEventToWriter(eventWriter, potentialEvents);

        }
    }

    private void writePotentialEventToWriter(BodyWriter eventWriter, List<EventsAndAttributes> potentialEvents) {
        for(EventsAndAttributes eventsAndAttribute : potentialEvents){
            if(eventsAndAttribute.isStartEvent()){
                eventWriter.writeStartTag(eventsAndAttribute.getEvent().asStartElement().getName().toString(), eventsAndAttribute.getAttributes());
            }
            else if(eventsAndAttribute.isEndEvent()){
                eventWriter.writeEndTag(eventsAndAttribute.getEvent().asEndElement().getName().toString());
            }
            else if(eventsAndAttribute.isCharEvent()){
                eventWriter.write(eventsAndAttribute.getEvent().asCharacters().getData());
            }

        }
    }

    private XMLEvent skipBlockUnlessConditionSatisfied(XMLEventReader reader, String primaryElementName, String secondaryElementName,
                                                     String secondaryElementAttributeName, String secondaryElementAttributeValueRegex)
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
                    if (Pattern.matches(secondaryElementAttributeValueRegex, attribute.getValue())) {
                        xmlEvent = nextEvent;
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
        // do nothing
    }

    private static class EventsAndAttributes {
        private XMLEvent event;
        private Map<String, String> attributes;


        private EventsAndAttributes(XMLEvent event, Map<String, String> attributes) {
            this.event = event;
            this.attributes = attributes;
        }

        public XMLEvent getEvent() {
            return event;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public boolean isStartEvent(){
            return (event instanceof StartElementEvent);
        }

        public boolean isEndEvent(){
            return (event instanceof EndElementEvent);
        }

        public boolean isCharEvent(){
            return (event instanceof CharactersEventImpl);
        }
    }

}
