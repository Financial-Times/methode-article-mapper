package com.ft.methodearticletransformer.transformation;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLParser;
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;

public class PromoBoxXMLParser extends BaseXMLParser<BigNumberData> implements XmlParser<BigNumberData> {

	private static final String PROMO_INTRO = "promo-intro";
	private static final String PROMO_HEADLINE = "promo-headline";
	private static final String PULL_QUOTE = "promo-box";
	private static final String PROMO_LINK = "promo-link";
	private static final String PROMO_IMAGE = "promo-image";
	private static final String PROMO_TITLE = "promo-title";
    private static final String DUMMY_SOURCE_TEXT = "EM-dummyText";
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
		return "";
	}

	@Override
	protected void populateBean(BigNumberData pullQuoteData, StartElement nextStartElement,
								XMLEventReader xmlEventReader) {
		// look for either promo-headline or promo-intro
		if (isElementNamed(nextStartElement.getName(), PROMO_HEADLINE)) {
			pullQuoteData.setHeadline(getValueOrDefault(parseRawContent(PROMO_HEADLINE, xmlEventReader)));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_INTRO)) {
			pullQuoteData.setIntro(getValueOrDefault(parseRawContent(PROMO_INTRO, xmlEventReader)));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_LINK)) {
			pullQuoteData.setLink(getValueOrDefault(parseRawContent(PROMO_LINK, xmlEventReader)));
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_IMAGE)) {
			pullQuoteData.setImagePresent(true);
		}
		if (isElementNamed(nextStartElement.getName(), PROMO_TITLE)) {
			pullQuoteData.setTitle(getValueOrDefault(parseRawContent(PROMO_TITLE, xmlEventReader)));
		}
	}

	@Override
	public boolean doesTriggerElementContainAllDataNeeded() {
		return false;
	}

    //TODO This is currently duplicated across Parsers
    private String getValueOrDefault(String value){
        if(!Strings.isNullOrEmpty(value) && value.contains(DUMMY_SOURCE_TEXT)){
            return "";
        }
        return value;
    }
}
