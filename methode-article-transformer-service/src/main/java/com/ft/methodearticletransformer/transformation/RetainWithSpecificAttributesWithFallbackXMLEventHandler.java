package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class RetainWithSpecificAttributesWithFallbackXMLEventHandler extends BaseXMLEventHandler {

	private final XMLEventHandler fallbackEventHandler;
	private final StartElementMatcher matcher;
	protected List<String> validAttributes;

	public RetainWithSpecificAttributesWithFallbackXMLEventHandler(final XMLEventHandler fallbackEventHandler,
																   final StartElementMatcher matcher,
																   String... validAttributes) {
		checkArgument(fallbackEventHandler != null, "fallbackEventHandler cannot be null");
		checkArgument(matcher != null, "matcher cannot be null");
		this.fallbackEventHandler = fallbackEventHandler;
		this.matcher = matcher;

		this.validAttributes = new ArrayList<>();
		for(String name: validAttributes) {
			this.validAttributes.add(name.toLowerCase());
		}
	}

	@Override // // Only called where the start tag used the fallback event handler
	public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
		fallbackEventHandler.handleEndElementEvent(event, xmlEventReader, eventWriter);
	}

	@Override
	public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
										BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
		if(matcher.matches(event)) {
			Map<String,String> validAttributesAndValues  = getValidAttributesAndValues(event, validAttributes);
			eventWriter.writeStartTag(event.getName().getLocalPart(), validAttributesAndValues);
		} else {
			fallbackEventHandler.handleStartElementEvent(event, xmlEventReader, eventWriter, bodyProcessingContext);
		}
	}
}
