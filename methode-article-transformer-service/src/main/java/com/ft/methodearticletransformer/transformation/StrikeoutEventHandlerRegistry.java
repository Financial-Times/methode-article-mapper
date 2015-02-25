package com.ft.methodearticletransformer.transformation;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

public class StrikeoutEventHandlerRegistry extends XMLEventHandlerRegistry {

    public StrikeoutEventHandlerRegistry() {
        //default is to check all tags for channel attribute
        // Handle strikeouts, i.e. where have <p channel="!"> or <span channel="!">
        // For these elements if the attribute is missing use the fallback handler

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
