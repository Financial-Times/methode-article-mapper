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

public class PromoBoxEventHandler extends BaseXMLEventHandler {

	private static final String BIG_NUMBER_ELEMENT = "big-number";
	private static final String BIG_NUMBER_HEADLINE = "big-number-headline";
	private static final String BIG_NUMBER_INTRO = "big-number-intro";

	private final PromoBoxXMLParser promoBoxXMLParser;

	public PromoBoxEventHandler(PromoBoxXMLParser promoBoxXMLParser) {
		this.promoBoxXMLParser = promoBoxXMLParser;
	}

	@Override
	public void handleStartElementEvent(StartElement startElement, XMLEventReader xmlEventReader, BodyWriter eventWriter,
										BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

		// Confirm that the startEvent is of the correct type
		if (isElementOfCorrectType(startElement)) {

			// Parse the xml needed to create a bean
			BigNumberData dataBean = parseElementData(startElement, xmlEventReader);

			// Add asset to the context and create the aside element if all required data is present
			if (dataBean.isValidBigNumberData()) {
				// process raw data and add any assets to the context
				transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);

				// ensure that the mutated bean data is still valid for processing after the transform field content processing
				if(dataBean.isValidBigNumberData()) {
					eventWriter.writeStartTag(BIG_NUMBER_ELEMENT, noAttributes());
					writePullQuoteElement(eventWriter, dataBean);
				}
			}

		} else {
			throw new BodyProcessingException("event must correspond to " + BIG_NUMBER_ELEMENT + " tag");
		}
	}

	private void writePullQuoteElement(BodyWriter eventWriter, BigNumberData dataBean) {
		eventWriter.writeStartTag(BIG_NUMBER_HEADLINE, noAttributes());
		eventWriter.write(dataBean.getHeadline());
		eventWriter.writeEndTag(BIG_NUMBER_HEADLINE);

		eventWriter.writeStartTag(BIG_NUMBER_INTRO, noAttributes());
		eventWriter.write(dataBean.getIntro());
		eventWriter.writeEndTag(BIG_NUMBER_INTRO);
	}

	private Map<String, String> noAttributes() {
		return Collections.emptyMap();
	}

	@Override
	public void handleEndElementEvent(EndElement event, XMLEventReader xmlEventReader, BodyWriter eventWriter) throws XMLStreamException {
		eventWriter.writeEndTag(BIG_NUMBER_ELEMENT);
	}

	protected boolean isElementOfCorrectType(StartElement event) {
		return event.getName().getLocalPart().toLowerCase().equals("promo-box");
	}

	private BigNumberData parseElementData(StartElement startElement, XMLEventReader xmlEventReader) throws XMLStreamException {
		return promoBoxXMLParser.parseElementData(startElement, xmlEventReader);
	}

	private void transformFieldContentToStructuredFormat(BigNumberData dataBean, BodyProcessingContext bodyProcessingContext) {
		promoBoxXMLParser.transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);
	}
}
