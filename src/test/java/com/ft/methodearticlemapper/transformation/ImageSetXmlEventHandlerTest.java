package com.ft.methodearticlemapper.transformation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import com.ft.uuidutils.GenerateV3UUID;
import org.codehaus.stax2.XMLEventReader2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class ImageSetXmlEventHandlerTest extends BaseXMLEventHandlerTest {

    private static final String IMAGE_SET_TAG = "image-set";
    private static final String ID_ATTRIBUTE = "id";
    private static final String IMAGE_SET_ID = "U11603541372105PPB";

    private static final String FT_CONTENT_TAG = "ft-content";
    private static final String GENERATED_UUID = GenerateV3UUID.singleDigested(IMAGE_SET_ID).toString();

    @Mock
    private BodyWriter mockEventWriter;
    @Mock
    private XMLEventReader2 mockXmlEventReader;
    @Mock
    private BodyProcessingContext mockBodyProcessingContext;
    private ImageSetXmlEventHandler eventHandler;

    private Map<String, String> expectedAttributes;

    @Before
    public void setUp() throws Exception {
        eventHandler = new ImageSetXmlEventHandler();

        expectedAttributes = new HashMap<>();
        expectedAttributes.put("type", "http://www.ft.com/ontology/content/ImageSet");
        expectedAttributes.put("url", "http://api.ft.com/content/" + GENERATED_UUID);
        expectedAttributes.put("data-embedded", "true");
    }

    @Test
    public void testTransformStartElementTag() throws Exception {
        Map<String, String> attributesMap = new HashMap<>();
        attributesMap.put(ID_ATTRIBUTE, IMAGE_SET_ID);
        StartElement imageSetStartElementTag = getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

        eventHandler.handleStartElementEvent(imageSetStartElementTag, mockXmlEventReader, mockEventWriter, mockBodyProcessingContext);

        verify(mockEventWriter).writeStartTag(FT_CONTENT_TAG, expectedAttributes);
        verify(mockEventWriter).writeEndTag(FT_CONTENT_TAG);
    }

    @Test
    public void testTransformStartElementShouldSkipMissingIDAttribute() throws Exception {
        Map<String, String> attributesMap = new HashMap<>();

        StartElement imageSetStartElementTag = getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

        eventHandler.handleStartElementEvent(imageSetStartElementTag, mockXmlEventReader, mockEventWriter, mockBodyProcessingContext);
        verifyZeroInteractions(mockEventWriter);
    }

    @Test
    public void testTransformStartElementShouldSkipEmptyIDAttribute() throws Exception {
        Map<String, String> attributesMap = new HashMap<>();
        attributesMap.put(ID_ATTRIBUTE, "");

        StartElement imageSetStartElementTag = getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

        eventHandler.handleStartElementEvent(imageSetStartElementTag, mockXmlEventReader, mockEventWriter, mockBodyProcessingContext);
        verifyZeroInteractions(mockEventWriter);
    }

    @Test
    public void testTransformEndElement() throws Exception {
        EndElement imageSetEndElementTag = getEndElement(IMAGE_SET_TAG);

        eventHandler.handleEndElementEvent(imageSetEndElementTag, mockXmlEventReader, mockEventWriter);

        verifyZeroInteractions(mockEventWriter);
    }
}
