package com.ft.methodearticletransformer.transformation;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

public class StrikeoutEventHandlerRegistry extends XMLEventHandlerRegistry {

    public StrikeoutEventHandlerRegistry() {
        // Default is to check all tags for channel attribute if there is no channel the element is retained
        // If the channel value is either FTcom or !Financial Times the elements are retained
        // If the value is something else the element is removed.

        registerDefaultEventHandler(new FilterXMLByAttributeAndValuesEventHandler(new RetainXMLEventHandler(), attributeNameMatcher("channel"), "FTcom", "!Financial Times"));
        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());

        registerStartAndEndElementEventHandler(new RetainXMLEventHandler(), "body");

    }

    public static StartElementMatcher attributeNameMatcher(final String attributeName) {
        return new StartElementMatcher() {
            @Override
            public boolean matches(final StartElement element) {
                final Attribute channel = element.getAttributeByName(new QName(attributeName));
                return channel != null;
            }
        };
    }

}
