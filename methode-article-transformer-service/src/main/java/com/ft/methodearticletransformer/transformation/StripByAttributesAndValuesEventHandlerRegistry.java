package com.ft.methodearticletransformer.transformation;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.xml.eventhandlers.PlainTextHtmlEntityReferenceEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;

import java.util.ArrayList;
import java.util.List;

public class StripByAttributesAndValuesEventHandlerRegistry extends XMLEventHandlerRegistry {

    public StripByAttributesAndValuesEventHandlerRegistry() {
        // Default is to check all tags for channel attribute if there is no channel the element is retained
        // If the channel value is either FTcom or !Financial Times the elements are retained
        // If the value is something else the element is removed.

        List<String> channelAttributes = new ArrayList<>();
        channelAttributes.add("FTcom");
        channelAttributes.add("!Financial Times");

        List<String> classAttributes = new ArrayList<>();
        classAttributes.add("@notes");

        registerDefaultEventHandler(new StripByAttributesAndValuesEventHandler(new StripByAttributesAndValuesEventHandler(new RetainXMLEventHandler(),
                strikeoutMatcher("channel", channelAttributes), channelAttributes), negateStrikeoutMatcher("class", classAttributes), classAttributes));
        registerCharactersEventHandler(new RetainXMLEventHandler());
        registerEntityReferenceEventHandler(new PlainTextHtmlEntityReferenceEventHandler());

        registerStartAndEndElementEventHandler(new RetainXMLEventHandler(), "body");

    }

    public static StrikeoutMatcher strikeoutMatcher(final String attributeName, final List<String> attributesValuesList) {
        return new StrikeoutMatcher() {
            @Override
            public boolean matchesOnElementName(StartElement startElement) {
                final Attribute attribute = startElement.getAttributeByName(new QName(attributeName));
                return attribute != null;
            }

            @Override
            public boolean matchesStrikeoutCriteria(List<String> attributeValueList, StartElement startElement) {
                final String startElementAttributeValue = startElement.getAttributeByName(new QName(attributeName)).getValue();
                boolean channelValueIsStrikeout = true;
                for(String attributeValue : attributesValuesList) {
                    if(startElementAttributeValue.equals(attributeValue)) {
                        channelValueIsStrikeout = false;
                        return channelValueIsStrikeout;
                    }
                }
                return channelValueIsStrikeout;
            }
        };
    }

    public static StrikeoutMatcher negateStrikeoutMatcher(final String attributeName, final List<String> attributesValueList) {
        return new StrikeoutMatcher() {
            @Override
            public boolean matchesOnElementName(StartElement startElement) {
                final Attribute attribute = startElement.getAttributeByName(new QName(attributeName));
                return attribute != null;
            }

            @Override
            public boolean matchesStrikeoutCriteria(List<String> attributeValueList, StartElement startElement) {
                final String startElementAttributeValue = startElement.getAttributeByName(new QName(attributeName)).getValue();
                boolean channelValueIsStrikeout = false;
                for(String attributeValue : attributesValueList) {
                    if(startElementAttributeValue.equals(attributeValue)) {
                        channelValueIsStrikeout = true;
                        return channelValueIsStrikeout;
                    }
                }
                return channelValueIsStrikeout;
            }
        };
    }
}
