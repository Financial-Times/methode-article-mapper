package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PromoBoxEventHandlerTest extends BaseXMLEventHandlerTest {

    private PromoBoxEventHandler eventHandler;

    private static final String BIG_NUMBER_ELEMENT = "big-number";
    private static final String BIG_NUMBER_HEADLINE_VALUE = "headline";
    private static final String BIG_NUMBER_INTRO_VALUE = "intro";
    private static final String METHODE_PROMO_BOX_ELEMENT = "promo-box";
    private static final String INCORRECT_ELEMENT = "a";

    @Mock private XMLEventReader mockXmlEventReader;
    @Mock private BodyWriter mockBodyWriter;
    @Mock private BodyProcessingContext mockBodyProcessingContext;
    @Mock private PromoBoxXMLParser mockPromoBoxXMLParser;
    @Mock private PromoBoxData mockPromoBoxData;

    @Before
    public void setUp() {
        eventHandler = new PromoBoxEventHandler(mockPromoBoxXMLParser);
    }

    @Test(expected=BodyProcessingException.class)
    public void shouldThrowBodyProcessingExceptionIfOpeningTagIsNotPromoBox() throws Exception {
        StartElement startElement = getStartElementWithAttributes(INCORRECT_ELEMENT, attributeClassEqualToNumberComponent());
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
    }

    @Test
    public void shouldNotTransformContentIfAllValidDataIsNotPresent() throws Exception {
		StartElement startElement = getStartElementWithAttributes(METHODE_PROMO_BOX_ELEMENT, attributeClassEqualToNumberComponent());
        when(mockPromoBoxXMLParser.parseElementData(startElement, mockXmlEventReader, mockBodyProcessingContext)).thenReturn(mockPromoBoxData);
        when(mockPromoBoxData.isValidBigNumberData()).thenReturn(false);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter, times(0)).writeStartTag(BIG_NUMBER_ELEMENT, noAttributes());
        verify(mockBodyWriter, times(0)).writeEndTag(BIG_NUMBER_ELEMENT);
    }

    @Test
    public void shouldNotWriteTransformedContentIfAllValidDataIsNotPresentAfterTransformation() throws Exception {
		StartElement startElement = getStartElementWithAttributes(METHODE_PROMO_BOX_ELEMENT, attributeClassEqualToNumberComponent());
        when(mockPromoBoxXMLParser.parseElementData(startElement, mockXmlEventReader, mockBodyProcessingContext)).thenReturn(mockPromoBoxData);
        when(mockPromoBoxData.isValidBigNumberData()).thenReturn(true).thenReturn(false);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter, times(0)).writeStartTag(BIG_NUMBER_ELEMENT, noAttributes());
        verify(mockBodyWriter, times(0)).writeEndTag(BIG_NUMBER_ELEMENT);
    }

    @Test
    public void shouldWriteTransformedElementsToWriter() throws Exception {
		StartElement startElement = getStartElementWithAttributes(METHODE_PROMO_BOX_ELEMENT, attributeClassEqualToNumberComponent());
		when(mockPromoBoxXMLParser.parseElementData(startElement, mockXmlEventReader, mockBodyProcessingContext)).thenReturn(mockPromoBoxData);
        when(mockPromoBoxData.isValidBigNumberData()).thenReturn(true).thenReturn(true);
        when(mockPromoBoxData.getHeadline()).thenReturn(BIG_NUMBER_HEADLINE_VALUE);
        when(mockPromoBoxData.getIntro()).thenReturn(BIG_NUMBER_INTRO_VALUE);
        eventHandler.handleStartElementEvent(startElement, mockXmlEventReader, mockBodyWriter, mockBodyProcessingContext);
        verify(mockBodyWriter).writeStartTag(BIG_NUMBER_ELEMENT, noAttributes());
        verify(mockBodyWriter).write(mockPromoBoxData.getHeadline());
        verify(mockBodyWriter).write(mockPromoBoxData.getIntro());
        verify(mockBodyWriter).writeEndTag(BIG_NUMBER_ELEMENT);
    }

	private Map<String, String> attributeClassEqualToNumberComponent() {
		Map<String, String> attributeClassEqualToNumberComponent = new HashMap<>();
		attributeClassEqualToNumberComponent.put(PromoBoxEventHandler.PROMO_CLASS_ATTRIBUTE,
				PromoBoxEventHandler.NUMBERS_COMPONENT_CLASS);
		return attributeClassEqualToNumberComponent;
	}

	private Map<String, String> noAttributes() {
        return Collections.emptyMap();
    }
}
