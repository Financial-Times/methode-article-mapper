package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;
import com.google.common.collect.ImmutableList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.util.List;

public class RetainWithSpecificAttributesXMLEventHandler extends BaseXMLEventHandler {

	private final List<String> validAttributes;

	public RetainWithSpecificAttributesXMLEventHandler(String... validAttributes) {
		this.validAttributes = ImmutableList.copyOf(validAttributes);
	}

	@Override
	public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
		eventWriter.writeEndTag(event.getName().getLocalPart());
	}

	@Override
	public void handleStartElementEvent(StartElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter,
			BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
		eventWriter.writeStartTag(event.getName().getLocalPart(), getValidAttributesAndValues(event, validAttributes));
	}
}
