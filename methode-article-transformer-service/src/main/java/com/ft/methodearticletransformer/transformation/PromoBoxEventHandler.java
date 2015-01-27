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

public class PromoBoxEventHandler extends BaseXMLEventHandler {

	private static final String BIG_NUMBER_ELEMENT = "big-number";
	private static final String BIG_NUMBER_HEADLINE = "big-number-headline";
	private static final String BIG_NUMBER_INTRO = "big-number-intro";
	private static final String PROMO_BOX = "promo-box";

	private final PromoBoxXMLParser promoBoxXMLParser;

	public PromoBoxEventHandler(PromoBoxXMLParser promoBoxXMLParser) {
		this.promoBoxXMLParser = promoBoxXMLParser;
	}

	@Override
	public void handleStartElementEvent(StartElement startElement, XMLEventReader xmlEventReader, BodyWriter eventWriter,
										BodyProcessingContext bodyProcessingContext) throws XMLStreamException {

		// Confirm that the startEvent is of the correct type
		if (isPromoBox(startElement)) {

			// Parse the xml needed to create a bean
			PromoBoxData dataBean = parseElementData(startElement, xmlEventReader);

			// Add asset to the context and create the aside element if all required data is present
			if (promoBoxIsValidBigNumber(dataBean)) {
				// We assume this promo box is a valid big number.

				// process raw data and add any assets to the context
				transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);

				// ensure that the mutated bean data is still valid for processing after the transform field content processing
				if(dataBean.isValidBigNumberData()) {
					eventWriter.writeStartTag(BIG_NUMBER_ELEMENT, noAttributes());
					writePullQuoteElement(eventWriter, dataBean);
					eventWriter.writeEndTag(BIG_NUMBER_ELEMENT);
				}
			}

		} else {
			throw new BodyProcessingException("event must correspond to " + PROMO_BOX + " tag");
		}
	}

	private boolean promoBoxIsValidBigNumber(PromoBoxData dataBean) {
		return dataBean.isValidBigNumberData();
	}

	private void writePullQuoteElement(BodyWriter eventWriter, PromoBoxData dataBean) {
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

	protected boolean isPromoBox(StartElement event) {
		return event.getName().getLocalPart().toLowerCase().equals(PROMO_BOX);
	}

	private PromoBoxData parseElementData(StartElement startElement, XMLEventReader xmlEventReader) throws XMLStreamException {
		return promoBoxXMLParser.parseElementData(startElement, xmlEventReader);
	}

	private void transformFieldContentToStructuredFormat(PromoBoxData dataBean, BodyProcessingContext bodyProcessingContext) {
		promoBoxXMLParser.transformFieldContentToStructuredFormat(dataBean, bodyProcessingContext);
	}
}
