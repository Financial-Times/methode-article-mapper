package com.ft.methodearticletransformer.transformation;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLParser;
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;
import org.apache.commons.lang.StringUtils;

public class PromoBoxXMLParser extends BaseXMLParser<PromoBoxData> implements XmlParser<PromoBoxData> {

	private static final String PROMO_INTRO = "promo-intro";
	private static final String PROMO_HEADLINE = "promo-headline";
	private static final String PROMO_BOX = "promo-box";
	private static final String PROMO_LINK = "promo-link";
	private static final String PROMO_IMAGE = "promo-image";
	private static final String PROMO_TITLE = "promo-title";
    private static final String DUMMY_SOURCE_TEXT = "EM-dummyText";
	private StAXTransformingBodyProcessor stAXTransformingBodyProcessor;

	public PromoBoxXMLParser(StAXTransformingBodyProcessor stAXTransformingBodyProcessor) {
		super(PROMO_BOX);
		checkNotNull(stAXTransformingBodyProcessor, "The StAXTransformingBodyProcessor cannot be null.");
		this.stAXTransformingBodyProcessor = stAXTransformingBodyProcessor;
	}

	@Override
	public void transformFieldContentToStructuredFormat(PromoBoxData promoBoxData, BodyProcessingContext bodyProcessingContext) {
		// TODO Remove this method when possible, as it is now deprecated.
		throw new IllegalStateException("This method should no longer be called.");
	}

	@Override
	public PromoBoxData createDataBeanInstance() {
		return new PromoBoxData();
	}

	private String transformRawContentToStructuredFormat(String unprocessedContent, BodyProcessingContext bodyProcessingContext) {
		if (!StringUtils.isBlank(unprocessedContent)) {
			return stAXTransformingBodyProcessor.process(unprocessedContent, bodyProcessingContext);
		}
		return "";
	}

	@Override
	protected void populateBean(PromoBoxData promoBoxData, StartElement nextStartElement,
								XMLEventReader xmlEventReader, BodyProcessingContext bodyProcessingContext) {
		// look for either promo-headline or promo-intro
		if (isElementNamed(nextStartElement.getName(), PROMO_HEADLINE)) {
			promoBoxData.setHeadline(transformRawContentToStructuredFormat(
                    parseRawContent(PROMO_HEADLINE, xmlEventReader), bodyProcessingContext));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_INTRO)) {
			promoBoxData.setIntro(transformRawContentToStructuredFormat(
                    parseRawContent(PROMO_INTRO, xmlEventReader), bodyProcessingContext));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_LINK)) {
			promoBoxData.setLink(parseRawContent(PROMO_LINK, xmlEventReader));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_IMAGE)) {
			promoBoxData.setImagePresent(true);
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_TITLE)) {
			promoBoxData.setTitle(parseRawContent(PROMO_TITLE, xmlEventReader));
		}
	}

	@Override
	public boolean doesTriggerElementContainAllDataNeeded() {
		return false;
	}

}
