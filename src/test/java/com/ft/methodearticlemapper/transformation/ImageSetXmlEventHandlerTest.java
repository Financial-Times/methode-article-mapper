package com.ft.methodearticlemapper.transformation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.DefaultTransactionIdBodyProcessingContext;
import com.ft.bodyprocessing.writer.BodyWriter;
import com.ft.uuidutils.GenerateV3UUID;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import org.codehaus.stax2.XMLEventReader2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(value = MockitoJUnitRunner.class)
public class ImageSetXmlEventHandlerTest extends BaseXMLEventHandlerTest {

  private static final String API_HOST = "test.api.ft.com";
  private static final String TEST_TID = "test_tid";
  private static final String ARTICLE_UUID = "edf0d3db-6497-406a-9d40-79176b0ffadb";

  private static final String IMAGE_SET_TAG = "image-set";
  private static final String ID_ATTRIBUTE = "id";
  private static final String IMAGE_SET_ID = "U11603541372105PPB";

  private static final String FT_CONTENT_TAG = "ft-content";
  private static final String GENERATED_UUID =
      GenerateV3UUID.singleDigested(ARTICLE_UUID + IMAGE_SET_ID).toString();

  @Mock private BodyWriter mockEventWriter;
  @Mock private XMLEventReader2 mockXmlEventReader;

  private BodyProcessingContext bodyProcessingContext;
  private ImageSetXmlEventHandler eventHandler;

  private Map<String, String> expectedAttributes;

  @Before
  public void setUp() throws Exception {
    eventHandler = new ImageSetXmlEventHandler();

    bodyProcessingContext =
        new MappedDataBodyProcessingContext(
            TEST_TID,
            TransformationMode.PUBLISH,
            Maps.immutableEntry("uuid", ARTICLE_UUID),
            Maps.immutableEntry("apiHost", API_HOST));

    expectedAttributes = new HashMap<>();
    expectedAttributes.put("type", "http://www.ft.com/ontology/content/ImageSet");
    expectedAttributes.put("url", String.format("http://%s/content/%s", API_HOST, GENERATED_UUID));
    expectedAttributes.put("data-embedded", "true");
  }

  @Test
  public void testTransformStartElementTag() throws Exception {
    Map<String, String> attributesMap = new HashMap<>();
    attributesMap.put(ID_ATTRIBUTE, IMAGE_SET_ID);
    StartElement imageSetStartElementTag =
        getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

    eventHandler.handleStartElementEvent(
        imageSetStartElementTag, mockXmlEventReader, mockEventWriter, bodyProcessingContext);

    verify(mockEventWriter).writeStartTag(FT_CONTENT_TAG, expectedAttributes);
    verify(mockEventWriter).writeEndTag(FT_CONTENT_TAG);
  }

  @Test
  public void testTransformStartElementShouldSkipMissingIDAttribute() throws Exception {
    Map<String, String> attributesMap = new HashMap<>();

    StartElement imageSetStartElementTag =
        getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

    eventHandler.handleStartElementEvent(
        imageSetStartElementTag, mockXmlEventReader, mockEventWriter, bodyProcessingContext);
    verifyZeroInteractions(mockEventWriter);
  }

  @Test
  public void testTransformStartElementShouldSkipEmptyIDAttribute() throws Exception {
    Map<String, String> attributesMap = new HashMap<>();
    attributesMap.put(ID_ATTRIBUTE, "");

    StartElement imageSetStartElementTag =
        getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

    eventHandler.handleStartElementEvent(
        imageSetStartElementTag, mockXmlEventReader, mockEventWriter, bodyProcessingContext);
    verifyZeroInteractions(mockEventWriter);
  }

  @Test
  public void testTransformEndElement() throws Exception {
    EndElement imageSetEndElementTag = getEndElement(IMAGE_SET_TAG);

    eventHandler.handleEndElementEvent(imageSetEndElementTag, mockXmlEventReader, mockEventWriter);

    verifyZeroInteractions(mockEventWriter);
  }

  @Test
  public void testNoMappedDataContextSkipsProcessing() throws Exception {
    final Map<String, String> attributesMap = new HashMap<>();
    attributesMap.put(ID_ATTRIBUTE, IMAGE_SET_ID);
    final StartElement imageSetStartElementTag =
        getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

    eventHandler.handleStartElementEvent(
        imageSetStartElementTag,
        mockXmlEventReader,
        mockEventWriter,
        new DefaultTransactionIdBodyProcessingContext(TEST_TID));

    verifyZeroInteractions(mockEventWriter);
  }

  @Test
  public void testNoUuidInMappedDataContextSkipsProcessing() throws Exception {
    final Map<String, String> attributesMap = new HashMap<>();
    attributesMap.put(ID_ATTRIBUTE, IMAGE_SET_ID);
    final StartElement imageSetStartElementTag =
        getStartElementWithAttributes(IMAGE_SET_TAG, attributesMap);

    eventHandler.handleStartElementEvent(
        imageSetStartElementTag,
        mockXmlEventReader,
        mockEventWriter,
        new MappedDataBodyProcessingContext(TEST_TID, TransformationMode.PUBLISH));

    verifyZeroInteractions(mockEventWriter);
  }
}
