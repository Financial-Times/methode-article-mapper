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

public class PullQuoteXMLParser extends BaseXMLParser<PullQuoteData> implements XmlParser<PullQuoteData> {

	private static final String QUOTE_SOURCE = "web-pull-quote-source";
	private static final String QUOTE_TEXT = "web-pull-quote-text";
	private static final String PULL_QUOTE = "web-pull-quote";
    private static final String DUMMY_SOURCE_TEXT = "EM-dummyText";
	private StAXTransformingBodyProcessor stAXTransformingBodyProcessor;

	public PullQuoteXMLParser(StAXTransformingBodyProcessor stAXTransformingBodyProcessor) {
		super(PULL_QUOTE);
		checkNotNull(stAXTransformingBodyProcessor, "The StAXTransformingBodyProcessor cannot be null.");
		this.stAXTransformingBodyProcessor = stAXTransformingBodyProcessor;
	}

	@Override
	public void transformFieldContentToStructuredFormat(PullQuoteData pullQuoteData, BodyProcessingContext bodyProcessingContext) {
		pullQuoteData.setQuoteText(transformRawContentToStructuredFormat(pullQuoteData.getQuoteText(), bodyProcessingContext));
		pullQuoteData.setQuoteSource(transformRawContentToStructuredFormat(pullQuoteData.getQuoteSource(), bodyProcessingContext));
	}

	@Override
	public PullQuoteData createDataBeanInstance() {
		return new PullQuoteData();
	}

	private String transformRawContentToStructuredFormat(String unprocessedContent, BodyProcessingContext bodyProcessingContext) {
		if (!StringUtils.isBlank(unprocessedContent)) {
			return stAXTransformingBodyProcessor.process(unprocessedContent, bodyProcessingContext);
		}
		return "";
	}

	@Override
	protected void populateBean(PullQuoteData pullQuoteData, StartElement nextStartElement,
								XMLEventReader xmlEventReader) {
		// look for either web-pull-quote-text or web-pull-quote-source
		if (isElementNamed(nextStartElement.getName(), QUOTE_TEXT)) {
			pullQuoteData.setQuoteText(parseRawContent(QUOTE_TEXT, xmlEventReader));
		}
		if (isElementNamed(nextStartElement.getName(), QUOTE_SOURCE)) {
            String source = parseRawContent(QUOTE_SOURCE, xmlEventReader);
            if(!Strings.isNullOrEmpty(source) && source.contains(DUMMY_SOURCE_TEXT)){
                source = "";
            }
			pullQuoteData.setQuoteSource(source);
		}
	}

	@Override
	public boolean doesTriggerElementContainAllDataNeeded() {
		return false;
	}
}
