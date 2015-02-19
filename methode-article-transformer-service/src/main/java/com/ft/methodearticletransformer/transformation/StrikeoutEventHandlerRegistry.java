package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

public class StrikeoutEventHandlerRegistry extends XMLEventHandlerRegistry {

    private static final String FINANCIAL_TIMES_STRIKEOUT = "Financial Times";
    private static final String FTCOM_STRIKEOUT = "FTcom";

    public StrikeoutEventHandlerRegistry() {
        //default is to check all
        registerDefaultEventHandler(new RemoveElementEventHandler(new RetainXMLEventHandler(), attributeNameAndValueMatcher("channel")));
        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());
        // want to be sure to keep the wrapping node
        registerStartAndEndElementEventHandler(new RetainXMLEventHandler(), "body");


        // Handle strikeouts, i.e. where have <p channel="!"> or <span channel="!">
        // For these elements if the attribute is missing use the fallback handler
    }

    public static StartElementMatcher attributeNameAndValueMatcher(final String attributeName) {
        return new StartElementMatcher() {
            @Override
            public boolean matches(final StartElement element) {
                final Attribute channel = element.getAttributeByName(new QName(attributeName));
                if(channel!=null) {
                    final String value = element.getAttributeByName(new QName(attributeName)).getValue();
                    if (value.startsWith("!") || value.equals(FINANCIAL_TIMES_STRIKEOUT) || value.equals(FTCOM_STRIKEOUT) || value.isEmpty()) {
                        return (channel == null) ? false : true;
                    }
                }
                return false;
            }
        };
    }

}
