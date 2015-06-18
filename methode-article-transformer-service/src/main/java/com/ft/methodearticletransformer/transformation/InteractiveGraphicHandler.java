package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.google.common.base.Strings;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.util.*;
import java.util.regex.Pattern;

public class InteractiveGraphicHandler extends BaseXMLEventHandler {

    private static final String A = "a";
    private static final String HREF = "href";
    private static final String SRC = "src";
    private static final String DATA_ASSET_TYPE = "data-asset-type";
    private static final String INTERACTIVE_GRAPHIC = "interactive graphic";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String DATA_WIDTH = "data-width";
    private static final String DATA_HEIGHT = "data-height";
    private static final List<Pattern> ALLOWED_PATTERNS = compile(Arrays.asList(
            "http:\\/\\/interactive.ftdata.co.uk\\/(?!_other\\/ben\\/twitter).*",
            "http:\\/\\/(www.)?ft.com\\/ig\\/(?!widgets\\/widgetBrowser\\/audio).*",
            "http:\\/\\/ig.ft.com\\/features.*",
            "http:\\/\\/ft.cartodb.com.*"
    ));

    private final XMLEventHandler fallbackHandler;

    public InteractiveGraphicHandler(final XMLEventHandler fallbackHandler) {
        this.fallbackHandler = fallbackHandler;
    }

    @Override
    public void handleStartElementEvent(final StartElement event,
                                        final XMLEventReader xmlEventReader,
                                        final BodyWriter eventWriter,
                                        final BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
        final String url = extractUrl(event);
        if (Strings.isNullOrEmpty(url) ||
                !matchesInteractiveGraphicFormat(url)) {
            fallbackHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
            return;
        }
        final Map<String, String> attributesToAdd = new HashMap<>();
        attributesToAdd.put(HREF, url);
        attributesToAdd.put(DATA_ASSET_TYPE, INTERACTIVE_GRAPHIC);
        final String width = extractAttribute(WIDTH, event);
        if (!Strings.isNullOrEmpty(width)) {
            attributesToAdd.put(DATA_WIDTH, width);
        }
        final String height = extractAttribute(HEIGHT, event);
        if (!Strings.isNullOrEmpty(height)) {
            attributesToAdd.put(DATA_HEIGHT, height);
        }
        skipUntilMatchingEndTag(event.getName().toString(), xmlEventReader);
        eventWriter.writeStartTag(A, attributesToAdd);
        eventWriter.writeEndTag(A);
    }

    private boolean matchesInteractiveGraphicFormat(final String url) {
        for (final Pattern pattern : ALLOWED_PATTERNS) {
            if (pattern.matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }

    private String extractUrl(StartElement event) {
        return extractAttribute(SRC, event);
    }

    private String extractAttribute(String measure, StartElement event) {
        Attribute attribute = event.getAttributeByName(QName.valueOf(measure));
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    private static List<Pattern> compile(List<String> rules) {
        List<Pattern> patterns = new LinkedList<>();
        for (final String rule : rules) {
            patterns.add(Pattern.compile(rule));
        }
        return patterns;
    }
}
