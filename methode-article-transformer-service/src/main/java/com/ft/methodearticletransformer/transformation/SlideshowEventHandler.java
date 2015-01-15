package com.ft.methodearticletransformer.transformation;

import static com.google.common.base.Preconditions.checkArgument;

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
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;
import com.google.common.collect.ImmutableMap;


public class SlideshowEventHandler extends BaseXMLEventHandler {
    
    private static final String A_TAG_NAME = "a";
    private static final String HREF_ATTRIBUTE_NAME = "href";
    
    private static final String SLIDESHOW_URL_TEMPLATE = "http://www.ft.com/cms/s/%s.html#slide0";

    private static final String TYPE_SLIDESHOW = "slideshow";
    private static final String TYPE_ATTRIBUTE_NAME = "type";
    
    private XMLEventHandler fallbackEventHandler;
    private XmlParser<SlideshowData> slideshowXMLParser;
    private final StartElementMatcher matcher;  

    protected SlideshowEventHandler(XmlParser<SlideshowData> slideshowXMLParser, XMLEventHandler fallbackEventHandler, final StartElementMatcher matcher) {
        
        checkArgument(fallbackEventHandler != null, "fallbackEventHandler cannot be null");
        checkArgument(slideshowXMLParser != null, "slideshowXMLParser cannot be null");
        checkArgument(matcher != null, "matcher cannot be null");
        
        this.fallbackEventHandler = fallbackEventHandler;
        this.slideshowXMLParser = slideshowXMLParser;
        this.matcher = matcher;
    }

    
    @Override
    public void handleStartElementEvent(final StartElement event, final XMLEventReader xmlEventReader, final BodyWriter eventWriter,
            final BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        if(!matcher.matches(event)) {
            fallbackEventHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
        } else {
            
            SlideshowData dataBean = parseElementData(event, xmlEventReader);
            
            if (dataBean.isAllRequiredDataPresent()) {
                writeSlideshowElement(eventWriter, dataBean);
            }
        }
    }


    SlideshowData parseElementData(StartElement startElement, XMLEventReader xmlEventReader) throws XMLStreamException {
        return slideshowXMLParser.parseElementData(startElement, xmlEventReader);
    }
    
    private void writeSlideshowElement(BodyWriter eventWriter, SlideshowData dataBean) {
        eventWriter.writeStartTag(A_TAG_NAME, getValidAttributes(dataBean));
        eventWriter.writeEndTag(A_TAG_NAME);
    }

    
    private Map<String, String> getValidAttributes(SlideshowData dataBean) {
        String slideshowUrl = String.format(SLIDESHOW_URL_TEMPLATE, dataBean.getUuid());
        return ImmutableMap.of(HREF_ATTRIBUTE_NAME, slideshowUrl);
    }


    protected boolean isElementOfCorrectType(StartElement event) {
        Attribute typeAttribute = event.getAttributeByName(QName.valueOf(TYPE_ATTRIBUTE_NAME));
        return typeAttribute != null && TYPE_SLIDESHOW.equals(typeAttribute.getValue());
    }

}
