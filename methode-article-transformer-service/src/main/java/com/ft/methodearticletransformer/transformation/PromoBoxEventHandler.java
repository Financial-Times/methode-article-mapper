package com.ft.methodearticletransformer.transformation;

import java.util.Collections;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLEventHandler;

public class PromoBoxEventHandler extends BaseXMLEventHandler {

	private static final String BIG_NUMBER_ELEMENT = "big-number";
	private static final String BIG_NUMBER_HEADLINE = "big-number-headline";
	private static final String BIG_NUMBER_INTRO = "big-number-intro";
	private static final String PROMO_BOX = "promo-box";
	public static final String NUMBERS_COMPONENT_CLASS = "numbers-component";
	public static final String PROMO_CLASS_ATTRIBUTE = "class";
    public static final String PARAGRAPH_TAG = "p";

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
            PromoBoxData dataBean = parseElementData(startElement, xmlEventReader, bodyProcessingContext);

            // Add asset to the context and create the aside element if all required data is present
            if (promoBoxIsValidBigNumber(startElement, dataBean)) {
                if (eventWriter.isPTagCurrentlyOpen()) {
                    eventWriter.writeEndTag(PARAGRAPH_TAG);

                    writeBigNumber(eventWriter, dataBean);

                    eventWriter.writeStartTag(PARAGRAPH_TAG, noAttributes());

                }
                else {
                    writeBigNumber(eventWriter, dataBean);
                }

            }

        }
        else {
            throw new BodyProcessingException("event must correspond to " + PROMO_BOX + " tag");
        }
    }

    private void writeBigNumber(BodyWriter eventWriter, PromoBoxData dataBean) {
        eventWriter.writeStartTag(BIG_NUMBER_ELEMENT, noAttributes());
        writeBigNumberElement(eventWriter, dataBean);
        eventWriter.writeEndTag(BIG_NUMBER_ELEMENT);
    }


    private boolean promoBoxIsValidBigNumber(StartElement startElement, PromoBoxData dataBean) {
		Attribute classAttribute = startElement.getAttributeByName(new QName(PROMO_CLASS_ATTRIBUTE));
		return isNumbersComponent(classAttribute) && dataBean.isValidBigNumberData();
	}

	private void writeBigNumberElement(BodyWriter eventWriter, PromoBoxData dataBean) {
		eventWriter.writeStartTag(BIG_NUMBER_HEADLINE, noAttributes());
		eventWriter.writeRaw(dataBean.getHeadline());
		eventWriter.writeEndTag(BIG_NUMBER_HEADLINE);

		eventWriter.writeStartTag(BIG_NUMBER_INTRO, noAttributes());
		eventWriter.writeRaw(dataBean.getIntro());
		eventWriter.writeEndTag(BIG_NUMBER_INTRO);
	}

	private Map<String, String> noAttributes() {
		return Collections.emptyMap();
	}

	protected boolean isPromoBox(StartElement event) {
		return event.getName().getLocalPart().toLowerCase().equals(PROMO_BOX);
	}

	private PromoBoxData parseElementData(StartElement startElement, XMLEventReader xmlEventReader,
										  BodyProcessingContext bodyProcessingContext) throws XMLStreamException {
		return promoBoxXMLParser.parseElementData(startElement, xmlEventReader, bodyProcessingContext);
	}

	private boolean isNumbersComponent(Attribute classAttribute) {
		return classAttribute != null && NUMBERS_COMPONENT_CLASS.equals(classAttribute.getValue());
	}
}
