package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynamicContentXMLEventHandlerTest extends BaseXMLEventHandlerTest {

    private static final String START_ELEMENT_TAG = "a";
    private static final String FT_CONTENT_TAG = "ft-content";
    private static final String API_HOST = "test.api.ft.com";
    private static final String UUID = "d02886fc-58ff-11e8-9859-6668838a4c10";

    private DynamicContentXMLEventHandler dynamicContentXMLEventHandler;

    @Mock
    private DynamicContentXMLParser mockDynamicContentXMLParser;
    @Mock
    private DynamicContentData mockDynamicContentData;
    @Mock
    private XMLEventReader mockXMLEventReader;
    @Mock
    private BodyWriter mockBodyWriter;
    @Mock
    private MappedDataBodyProcessingContext mockMappedDataBodyProcessingContext;
    @Mock
    private BodyProcessingContext mockBodyProcessingContext;

    @Before
    public void setUp() {
        dynamicContentXMLEventHandler = new DynamicContentXMLEventHandler(mockDynamicContentXMLParser);
    }

    @Test
    public void shouldNotWriteDynamicContentIfRequiredDataIsMissing() throws Exception {
        StartElement startElement = getStartElement(START_ELEMENT_TAG);
        when(mockDynamicContentXMLParser.parseElementData(startElement, mockXMLEventReader, mockMappedDataBodyProcessingContext)).thenReturn(mockDynamicContentData);

        dynamicContentXMLEventHandler.handleStartElementEvent(startElement, mockXMLEventReader, mockBodyWriter, mockMappedDataBodyProcessingContext);

        verify(mockBodyWriter, times(0)).writeStartTag(anyString(), any());
        verify(mockBodyWriter, times(0)).writeEndTag(anyString());
    }

    @Test
    public void shouldWriteTransformedDynamicContent() throws Exception {
        StartElement startElement = getStartElement(START_ELEMENT_TAG);
        when(mockDynamicContentXMLParser.parseElementData(startElement, mockXMLEventReader, mockMappedDataBodyProcessingContext)).thenReturn(mockDynamicContentData);
        when(mockDynamicContentData.isAllRequiredDataPresent()).thenReturn(true);
        when(mockDynamicContentData.getUuid()).thenReturn(UUID);
        when(mockMappedDataBodyProcessingContext.get("apiHost", String.class)).thenReturn(API_HOST);

        dynamicContentXMLEventHandler.handleStartElementEvent(startElement, mockXMLEventReader, mockBodyWriter, mockMappedDataBodyProcessingContext);

        verify(mockBodyWriter).writeStartTag(FT_CONTENT_TAG, getAttributes(API_HOST));
        verify(mockBodyWriter).writeEndTag(FT_CONTENT_TAG);
    }

    @Test
    public void shouldWriteTransformedDynamicContentWhenApiHostIsMissing() throws Exception {
        StartElement startElement = getStartElement(START_ELEMENT_TAG);
        when(mockDynamicContentXMLParser.parseElementData(startElement, mockXMLEventReader, mockMappedDataBodyProcessingContext)).thenReturn(mockDynamicContentData);
        when(mockDynamicContentData.isAllRequiredDataPresent()).thenReturn(true);
        when(mockDynamicContentData.getUuid()).thenReturn(UUID);
        when(mockMappedDataBodyProcessingContext.get("apiHost", String.class)).thenReturn(null);

        dynamicContentXMLEventHandler.handleStartElementEvent(startElement, mockXMLEventReader, mockBodyWriter, mockMappedDataBodyProcessingContext);

        verify(mockBodyWriter).writeStartTag(FT_CONTENT_TAG, getAttributes(null));
        verify(mockBodyWriter).writeEndTag(FT_CONTENT_TAG);
    }

    @Test
    public void shouldWriteTransformedDynamicContentWhenWrongBodyProcessingContextInstanceType() throws Exception {
        StartElement startElement = getStartElement(START_ELEMENT_TAG);
        when(mockDynamicContentXMLParser.parseElementData(startElement, mockXMLEventReader, mockBodyProcessingContext)).thenReturn(mockDynamicContentData);
        when(mockDynamicContentData.isAllRequiredDataPresent()).thenReturn(true);
        when(mockDynamicContentData.getUuid()).thenReturn(UUID);

        dynamicContentXMLEventHandler.handleStartElementEvent(startElement, mockXMLEventReader, mockBodyWriter, mockBodyProcessingContext);

        verify(mockBodyWriter).writeStartTag(FT_CONTENT_TAG, getAttributes(null));
        verify(mockBodyWriter).writeEndTag(FT_CONTENT_TAG);
    }

    private Map<String, String> getAttributes(String apiHost) {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("type", "http://www.ft.com/ontology/content/DynamicContent");
        attributes.put("url", String.format("https://%s/content/%s", apiHost, UUID));
        attributes.put("data-embedded", "true");
        return attributes;
    }

}
