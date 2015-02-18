package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import java.util.Collections;
import java.util.Map;

public class PullQuoteEventHandler extends BaseXMLEventHandler {

	private static final String PULL_QUOTE_ELEMENT = "pull-quote";
	private static final String PULL_QUOTE_TEXT = "pull-quote-text";
	private static final String PULL_QUOTE_SOURCE = "pull-quote-source";

	private final PullQuoteXMLParser pullQuoteXMLParser;

	public PullQuoteEventHandler(PullQuoteXMLParser pullQuoteXMLParser) {
		this.pullQuoteXMLParser = pullQuoteXMLParser;
	}

	@Override
	public void handleStartElementEvent(StartElement startElement, XMLEventReader xmlEventReader, BodyWriter eventWriter,
										BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

		// Confirm that the startEvent is of the correct type
		if (isElementOfCorrectType(startElement)) {

			// Parse the xml needed to create a bean
			PullQuoteData dataBean = parseElementData(startElement, xmlEventReader, bodyProcessingContext);

			// Add asset to the context and create the aside element if all required data is present
			if (dataBean.isAllRequiredDataPresent()) {
				writePullQuoteElement(eventWriter, dataBean);
			}

		} else {
			throw new BodyProcessingException("event must correspond to " + PULL_QUOTE_ELEMENT + " tag");
		}
	}

	private void writePullQuoteElement(BodyWriter eventWriter, PullQuoteData dataBean) {
		eventWriter.writeStartTag(PULL_QUOTE_ELEMENT, noAttributes());

		eventWriter.writeStartTag(PULL_QUOTE_TEXT, noAttributes());
		eventWriter.writeRaw(dataBean.getQuoteText());
		eventWriter.writeEndTag(PULL_QUOTE_TEXT);

		eventWriter.writeStartTag(PULL_QUOTE_SOURCE, noAttributes());
		eventWriter.writeRaw(dataBean.getQuoteSource());
		eventWriter.writeEndTag(PULL_QUOTE_SOURCE);

		eventWriter.writeEndTag(PULL_QUOTE_ELEMENT);
	}

	private Map<String, String> noAttributes() {
		return Collections.emptyMap();
	}

	protected boolean isElementOfCorrectType(StartElement event) {
		return event.getName().getLocalPart().toLowerCase().equals("web-pull-quote");
	}

	private PullQuoteData parseElementData(StartElement startElement, XMLEventReader xmlEventReader,
										   BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
		return pullQuoteXMLParser.parseElementData(startElement, xmlEventReader, bodyProcessingContext);
	}
}
