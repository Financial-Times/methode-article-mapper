package com.ft.methodearticletransformer.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.codehaus.stax2.ri.evt.AttributeEventImpl;
import org.codehaus.stax2.ri.evt.EndElementEventImpl;
import org.codehaus.stax2.ri.evt.StartElementEventImpl;

public class MethodeOtherVideoXmlEventHandler extends BaseXMLEventHandler {

    private final String targetedHtmlClass;
    private final XMLEventHandler fallbackHandler   ;

    public MethodeOtherVideoXmlEventHandler(String targetedHtmlClass, XMLEventHandler fallbackHandler) {
        this.targetedHtmlClass = targetedHtmlClass;
        this.fallbackHandler = fallbackHandler;
    }


    @Override
    public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
                                        BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

        if (!isTargetedClass(event)) {
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }

        List<XMLEvent> potentialEvents = new ArrayList<XMLEvent>();


        StartElement pStartElementEvent = StartElementEventImpl.construct(null, new QName("p"), null, null, null);
        EndElement pEndElementEvent = new EndElementEventImpl(null, new QName("p"), null);
        potentialEvents.add(pStartElementEvent);


        if (xmlEventReader.hasNext()) {
            XMLEvent nextEvent = xmlEventReader.nextEvent();
            if (nextEvent.isCharacters()) {
                potentialEvents.add(nextEvent.asCharacters());
            }
        }

        XMLEvent found = skipBlockUnlessConditionSatisfied(xmlEventReader, "p", "iframe", "src", "^http://[.a-zA-Z0-9/?=]*");
        if(found != null) {

            Attribute srcAttribute = found.asStartElement().getAttributeByName(QName.valueOf("src"));

            String videoLink =  srcAttribute.getValue();
            List<Attribute> attributesToAdd = new ArrayList<Attribute>();
            Attribute attribute = new AttributeEventImpl(null, new QName("href"), videoLink, false);
            attributesToAdd.add(attribute);

            StartElement aStartElementEvent = StartElementEventImpl.construct(null, new QName("a"), attributesToAdd.iterator(), null, null);
            EndElement aEndElementEvent = new EndElementEventImpl(null, new QName("a"), null);

            potentialEvents.add(aStartElementEvent);
            potentialEvents.add(aEndElementEvent);
            potentialEvents.add(pEndElementEvent);

            writePotentialEventToWriter(eventWriter, potentialEvents);

        }
    }

    private void writePotentialEventToWriter(BodyWriter eventWriter, List<XMLEvent> potentialEvents) {
        for(XMLEvent event : potentialEvents){
            if(event.isStartElement()){
                Iterator it = event.asStartElement().getAttributes();
                Map<String, String> attributes = new HashMap<>();

                while(it.hasNext()){
                    Attribute attribute = (Attribute)it.next();
                    attributes.put(attribute.getName().toString(), attribute.getValue().toString());
                }

                eventWriter.writeStartTag(event.asStartElement().getName().toString(), attributes);
            }
            else if(event.isEndElement()){
                eventWriter.writeEndTag(event.asEndElement().getName().toString());
            }
            else if(event.isCharacters()){
                eventWriter.write(event.asCharacters().getData());
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
        //only a fallback one should hit this code.
        fallbackHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
    }

}
