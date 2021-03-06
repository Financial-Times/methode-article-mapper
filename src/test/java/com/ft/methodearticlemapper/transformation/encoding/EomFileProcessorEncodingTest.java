package com.ft.methodearticlemapper.transformation.encoding;

import static com.ft.methodearticlemapper.transformation.EomFileProcessorTest.FINANCIAL_TIMES_BRAND;
import static com.ft.methodearticlemapper.transformation.EomFileProcessorTest.REUTERS_BRAND;
import static com.ft.methodearticlemapper.transformation.EomFileProcessorTest.createStandardEomFileWithMainImage;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.content.model.Brand;
import com.ft.content.model.Content;
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.FieldTransformer;
import com.ft.methodearticlemapper.transformation.TransformationMode;
import com.ft.uuidutils.DeriveUUID;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class EomFileProcessorEncodingTest {
  private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
  private static final String TRANSACTION_ID = "tid_test";
  private static final String PUBLISH_REF = "publishReference";
  private static final String API_HOST = "test.api.ft.com";
  private static final String WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";
  private static final String CANONICAL_WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";

  private FieldTransformer bodyTransformer = mock(FieldTransformer.class);
  private FieldTransformer bylineTransformer = mock(FieldTransformer.class);
  private BodyProcessor htmlFieldProcessor = spy(new Html5SelfClosingTagBodyProcessor());

  private final UUID uuid = UUID.randomUUID();

  private EomFileProcessor eomFileProcessor;

  @Before
  public void setUp() {
    Map<ContentSource, Brand> contentSourceBrandMap = new HashMap<>();
    contentSourceBrandMap.put(ContentSource.FT, new Brand(FINANCIAL_TIMES_BRAND));
    contentSourceBrandMap.put(ContentSource.Reuters, new Brand(REUTERS_BRAND));

    eomFileProcessor =
        new EomFileProcessor(
            EnumSet.allOf(TransformationMode.class),
            bodyTransformer,
            bylineTransformer,
            htmlFieldProcessor,
            contentSourceBrandMap,
            PUBLISH_REF,
            API_HOST,
            WEB_URL_TEMPLATE,
            CANONICAL_WEB_URL_TEMPLATE);

    when(bylineTransformer.transform(anyString(), anyString(), eq(TransformationMode.PUBLISH)))
        .thenReturn(TRANSFORMED_BYLINE);
  }

  @Test
  public void thatCharacterEncodingIsPreserved() {
    final String bodyText =
        "<p>Gina Rinehart, Australia’s richest person, has taken a television channel "
            + "to court in an apparent attempt to block the broadcast of a hit miniseries detailing her "
            + "colourful family and business history.</p>";

    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(String.format("<body>%s</body>", bodyText));

    final UUID imageUuid = UUID.randomUUID();
    final UUID expectedMainImageUuid = DeriveUUID.with(DeriveUUID.Salts.IMAGE_SET).from(imageUuid);

    String expectedBody =
        String.format(
            "<body><content data-embedded=\"true\" id=\"%s\" type=\"%s\"></content>%s</body>",
            expectedMainImageUuid, "http://www.ft.com/ontology/content/ImageSet", bodyText);

    final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid, "Primary size");
    Content content =
        eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, new Date());

    assertThat(
        String.format("body content using JVM encoding %s", System.getProperty("file.encoding")),
        content.getBody(),
        equalToIgnoringWhiteSpace(expectedBody));
  }
}
