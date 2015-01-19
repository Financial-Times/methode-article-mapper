package com.ft.methodearticletransformer.transformation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;

public class SlideshowEventHandler extends BaseXMLEventHandler {
    
    private static final String A_TAG_NAME = "a";
    private static final String HREF_ATTRIBUTE_NAME = "href";
    
    private static final String SLIDESHOW_URL_TEMPLATE = "http://www.ft.com/cms/s/%s.html#slide0";

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
                transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);
                writeSlideshowElement(eventWriter, dataBean);
            }
        }
    }

    private void transformFieldContentToStructuredFormat(SlideshowData dataBean, BodyProcessingContext bodyProcessingContext) {
        slideshowXMLParser.transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);
    }

    SlideshowData parseElementData(StartElement startElement, XMLEventReader xmlEventReader) throws XMLStreamException {
        return slideshowXMLParser.parseElementData(startElement, xmlEventReader);
    }
    
    private void writeSlideshowElement(BodyWriter eventWriter, SlideshowData dataBean) {
        eventWriter.writeStartTag(A_TAG_NAME, getValidAttributes(dataBean));
        eventWriter.writeEndTag(A_TAG_NAME);
    }

    private Map<String, String> getValidAttributes(SlideshowData dataBean) {
        Map<String, String> validAttributes = new HashMap<>();

        String slideshowUrl = String.format(SLIDESHOW_URL_TEMPLATE, dataBean.getUuid()) + queryParamsIfPresent(dataBean);
        validAttributes.put(HREF_ATTRIBUTE_NAME, slideshowUrl);
        validAttributes.put("type", "slideshow");

        return ImmutableMap.copyOf(validAttributes);
    }

    private String queryParamsIfPresent(SlideshowData dataBean) {
        if (dataBean.getQueryParams().size() == 0) {
            return "";
        } else {
            return "?" + StringUtils.join(dataBean.getQueryParams(), "&");
        }
    }

}
