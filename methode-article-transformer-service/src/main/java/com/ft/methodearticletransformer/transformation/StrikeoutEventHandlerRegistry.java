package com.ft.methodearticletransformer.transformation;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

public class StrikeoutEventHandlerRegistry extends XMLEventHandlerRegistry {

    public StrikeoutEventHandlerRegistry() {
        // Default is to check all tags for channel attribute as these are considered strikeouts and remove them
        // Fallback handler retains all tags with no channel attribute
        // <p channel="?"><iframe></iframe></p> will also be retained as this scenario represents a video which will be processed separately

        registerDefaultEventHandler(new StrikeoutXMLEventHandler(new RetainXMLEventHandler(), attributeNameMatcher("channel"), "p", "iframe"));
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
