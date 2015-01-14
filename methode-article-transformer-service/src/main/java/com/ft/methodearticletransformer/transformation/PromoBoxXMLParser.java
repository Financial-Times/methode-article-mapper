package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLParser;
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;
import org.apache.commons.lang.StringUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import static com.google.common.base.Preconditions.checkNotNull;

public class PromoBoxXMLParser extends BaseXMLParser<BigNumberData> implements XmlParser<BigNumberData> {

	private static final String PROMO_INTRO = "promo-intro";
	private static final String PROMO_HEADLINE = "promo-headline";
	private static final String PULL_QUOTE = "promo-box";
	private static final String PROMO_LINK = "promo-link";
	private StAXTransformingBodyProcessor stAXTransformingBodyProcessor;

	public PromoBoxXMLParser(StAXTransformingBodyProcessor stAXTransformingBodyProcessor) {
		super(PULL_QUOTE);
		checkNotNull(stAXTransformingBodyProcessor, "The StAXTransformingBodyProcessor cannot be null.");
		this.stAXTransformingBodyProcessor = stAXTransformingBodyProcessor;
	}

	@Override
	public void transformFieldContentToStructuredFormat(BigNumberData bigNumberData, BodyProcessingContext bodyProcessingContext) {
		bigNumberData.setHeadline(transformRawContentToStructuredFormat(bigNumberData.getHeadline(), bodyProcessingContext));
		bigNumberData.setIntro(transformRawContentToStructuredFormat(bigNumberData.getIntro(), bodyProcessingContext));
	}

	@Override
	public BigNumberData createDataBeanInstance() {
		return new BigNumberData();
	}

	private String transformRawContentToStructuredFormat(String unprocessedContent, BodyProcessingContext bodyProcessingContext) {
		if (!StringUtils.isBlank(unprocessedContent)) {
			return stAXTransformingBodyProcessor.process(unprocessedContent, bodyProcessingContext);
		}
		return null;
	}

	@Override
	protected void populateBean(BigNumberData pullQuoteData, StartElement nextStartElement,
								XMLEventReader xmlEventReader) {
		// look for either promo-headline or promo-intro
		if (isElementNamed(nextStartElement.getName(), PROMO_HEADLINE)) {
			pullQuoteData.setHeadline(parseRawContent(PROMO_HEADLINE, xmlEventReader));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_INTRO)) {
			pullQuoteData.setIntro(parseRawContent(PROMO_INTRO, xmlEventReader));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_LINK)) {
			pullQuoteData.setLink(parseRawContent(PROMO_LINK, xmlEventReader));
		}
	}

	@Override
	public boolean doesTriggerElementContainAllDataNeeded() {
		return false;
	}
}
