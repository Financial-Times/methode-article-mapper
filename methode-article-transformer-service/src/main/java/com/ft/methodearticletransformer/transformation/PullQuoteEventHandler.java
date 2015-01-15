package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
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
			PullQuoteData dataBean = parseElementData(startElement, xmlEventReader);

			// Add asset to the context and create the aside element if all required data is present
			if (dataBean.isAllRequiredDataPresent()) {
				// process raw data and add any assets to the context
				transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);

				// ensure that the mutated bean data is still valid for processing after the transform field content processing
				if(dataBean.isAllRequiredDataPresent()) {
					eventWriter.writeStartTag(PULL_QUOTE_ELEMENT, noAttributes());
					writePullQuoteElement(eventWriter, dataBean);
					eventWriter.writeEndTag(PULL_QUOTE_ELEMENT);
				}
			}

		} else {
			throw new BodyProcessingException("event must correspond to " + PULL_QUOTE_ELEMENT + " tag");
		}
	}

	private void writePullQuoteElement(BodyWriter eventWriter, PullQuoteData dataBean) {
		eventWriter.writeStartTag(PULL_QUOTE_TEXT, noAttributes());
		eventWriter.write(dataBean.getQuoteText());
		eventWriter.writeEndTag(PULL_QUOTE_TEXT);

		eventWriter.writeStartTag(PULL_QUOTE_SOURCE, noAttributes());
		eventWriter.write(dataBean.getQuoteSource());
		eventWriter.writeEndTag(PULL_QUOTE_SOURCE);
	}

	private Map<String, String> noAttributes() {
		return Collections.emptyMap();
	}

	protected boolean isElementOfCorrectType(StartElement event) {
		return event.getName().getLocalPart().toLowerCase().equals("web-pull-quote");
	}

	private PullQuoteData parseElementData(StartElement startElement, XMLEventReader xmlEventReader) throws XMLStreamException {
		return pullQuoteXMLParser.parseElementData(startElement, xmlEventReader);
	}

	private void transformFieldContentToStructuredFormat(PullQuoteData dataBean, BodyProcessingContext bodyProcessingContext) {
		pullQuoteXMLParser.transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);
	}
}
