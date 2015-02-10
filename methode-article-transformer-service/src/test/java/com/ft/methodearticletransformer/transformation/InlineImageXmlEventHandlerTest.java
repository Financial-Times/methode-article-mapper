package com.ft.methodearticletransformer.transformation;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.eventhandlers.RetainXMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandler;
import com.ft.bodyprocessing.xml.eventhandlers.XMLEventHandlerRegistry;
import com.ft.methodearticletransformer.util.ImageSetUuidGenerator;
import org.codehaus.stax2.XMLEventReader2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(value = MockitoJUnitRunner.class)
public class InlineImageXmlEventHandlerTest extends BaseXMLEventHandlerTest {

    private static final String CONTENT_TAG = "content";
    private static final String FILE_REF_ATTRIBUTE = "fileref";
    private static final String IMAGE_SET_TYPE = "http://www.ft.com/ontology/content/ImageSet";
    private static final String UUID = "c12f1318-cafc-11e3-ba9d-00144feabdc0";

    @Mock
    private BodyWriter mockEventWriter;
    @Mock
    private XMLEventReader2 mockXmlEventReader;
    @Mock
    private BodyProcessingContext mockBodyProcessingContext;
    private InlineImageXmlEventHandler eventHandler;

    @Before
    public void setUp() throws Exception {
        eventHandler = new InlineImageXmlEventHandler();
    }

    @Test
    public void testTransformStartElementTag() throws Exception {
        Map<String, String> attributesMap = new HashMap<>();
        attributesMap.put(FILE_REF_ATTRIBUTE, "/FT/Graphics/Online/Master_2048x1152/2014/04/MAS_CatalansDemo.jpg?uuid=" + UUID);
        StartElement webInlinePictureStartElementTag = getStartElementWithAttributes("web-inline-picture", attributesMap);

        eventHandler.handleStartElementEvent(webInlinePictureStartElementTag, mockXmlEventReader, mockEventWriter, mockBodyProcessingContext);

        Map<String, String> expectedAttributes = new HashMap<>();
        expectedAttributes.put("id", ImageSetUuidGenerator.fromImageUuid(java.util.UUID.fromString(UUID)).toString());
        expectedAttributes.put("type", IMAGE_SET_TYPE);
        verify(mockEventWriter).writeStartTag(CONTENT_TAG, expectedAttributes);
        verify(mockEventWriter).writeEndTag(CONTENT_TAG);
    }

    @Test(expected = BodyProcessingException.class)
    public void testTransformStartElementShouldRejectInvalidUuidInFileReference() throws Exception {
        Map<String, String> attributesMap = new HashMap<>();
        attributesMap.put(FILE_REF_ATTRIBUTE, "/FT/Graphics/Online/Master_2048x1152/2014/04/MAS_CatalansDemo.jpg?uuid=invalidUuid");

        StartElement webInlinePictureStartElementTag = getStartElementWithAttributes("web-inline-picture", attributesMap);

        eventHandler.handleStartElementEvent(webInlinePictureStartElementTag, mockXmlEventReader, mockEventWriter, mockBodyProcessingContext);
    }

    @Test(expected = BodyProcessingException.class)
    public void testTransformStartElementShouldRejectInvalidFileReference() throws Exception {
        Map<String, String> attributesMap = new HashMap<>();
        attributesMap.put(FILE_REF_ATTRIBUTE, "/FT/Graphics/Online/Master_2048x1152/2014/04/MAS_CatalansDemo.jpg?" + UUID);

        StartElement webInlinePictureStartElementTag = getStartElementWithAttributes("web-inline-picture", attributesMap);

        eventHandler.handleStartElementEvent(webInlinePictureStartElementTag, mockXmlEventReader, mockEventWriter, mockBodyProcessingContext);
    }

    @Test(expected = BodyProcessingException.class)
    public void testTransformStartElementShouldRejectMissingFileReference() throws Exception {
        StartElement webInlinePictureStartElementTag = getStartElementWithAttributes("web-inline-picture", new HashMap<String, String>());

        eventHandler.handleStartElementEvent(webInlinePictureStartElementTag, mockXmlEventReader, mockEventWriter, mockBodyProcessingContext);
    }

    @Test
    public void testTransformEndElement() throws Exception {
        EndElement webInlinePictureEndElementTag = getEndElement("web-inline-picture");

        eventHandler.handleEndElementEvent(webInlinePictureEndElementTag, mockXmlEventReader, mockEventWriter);

        verifyZeroInteractions(mockEventWriter);
    }

    @Test
    public void testTransformInlineImageWithAdditionalData() throws Exception {
        final XMLEventHandler charactersEventHandler = new RetainXMLEventHandler();
        final XMLEventHandler inlineImageXmlEventHandler = new InlineImageXmlEventHandler();
        final XMLEventHandlerRegistry registry = new XMLEventHandlerRegistry() {{
            this.registerCharactersEventHandler(charactersEventHandler);
            this.registerStartAndEndElementEventHandler(inlineImageXmlEventHandler, "web-inline-picture");
        }};
        final StAXTransformingBodyProcessor processor = new StAXTransformingBodyProcessor(registry);
        final String inputXml = String.format("<web-inline-picture " +
                "fileref=\"/FT/Graphics/Online/Master_2048x1152/2014/04/MAS_CatalansDemo.jpg?uuid=%s\" " +
                "tmx=\"272 193 272 193\" xtransform=\"scale(0.116 0.116)\" width=\"236\" height=\"133\" " +
                "align=\"right\" alt=\"\" channel=\"FTcom\" dtxInsert=\"Right-aligned landscape\" >" +
                "arbitrary text</web-inline-picture>", UUID);
        final String actual = processor.process(inputXml, null);

        String expectedXml = String.format("<content id=\"%s\" type=\"%s\"></content>",
                ImageSetUuidGenerator.fromImageUuid(java.util.UUID.fromString(UUID)).toString(), IMAGE_SET_TYPE);
        assertEquals(expectedXml, actual);
    }
}
