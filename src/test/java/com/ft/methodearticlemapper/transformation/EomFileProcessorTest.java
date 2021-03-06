package com.ft.methodearticlemapper.transformation;

import static com.ft.methodearticlemapper.methode.EomFileType.EOMCompoundStory;
import static com.ft.methodearticlemapper.methode.EomFileType.EOMStory;
import static com.ft.methodearticlemapper.transformation.SubscriptionLevel.FOLLOW_USUAL_RULES;
import static com.ft.methodearticlemapper.transformation.SubscriptionLevel.PREMIUM;
import static com.ft.methodearticlemapper.transformation.SubscriptionLevel.SHOWCASE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.common.FileUtils;
import com.ft.content.model.AccessLevel;
import com.ft.content.model.AlternativeStandfirsts;
import com.ft.content.model.AlternativeTitles;
import com.ft.content.model.Brand;
import com.ft.content.model.Comments;
import com.ft.content.model.Content;
import com.ft.content.model.Distribution;
import com.ft.content.model.Identifier;
import com.ft.content.model.Standout;
import com.ft.content.model.Syndication;
import com.ft.methodearticlemapper.exception.EmbargoDateInTheFutureException;
import com.ft.methodearticlemapper.exception.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.MissingInteractiveGraphicUuidException;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.SourceNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.UnsupportedEomTypeException;
import com.ft.methodearticlemapper.exception.UnsupportedObjectTypeException;
import com.ft.methodearticlemapper.exception.UnsupportedTransformationModeException;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.exception.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.util.ContentType;
import com.ft.uuidutils.DeriveUUID;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EomFileProcessorTest {
  public static final String FINANCIAL_TIMES_BRAND =
      "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54";
  public static final String REUTERS_BRAND =
      "http://api.ft.com/things/ed3b6ec5-6466-47ef-b1d8-16952fd522c7";
  public static final String DYNAMIC_CONTENT =
      "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54";
  protected static final String METHODE = "http://api.ft.com/system/FTCOM-METHODE";
  protected static final String IG = "http://api.ft.com/system/FTCOM-IG";

  private static final String ARTICLE_TEMPLATE =
      FileUtils.readFile("article/article_value.xml.mustache");
  private static final String ATTRIBUTES_TEMPLATE =
      FileUtils.readFile("article/article_attributes.xml.mustache");
  private static final String ATTRIBUTES_TEMPLATE_NO_CONTRIBUTOR_RIGHTS =
      FileUtils.readFile("article/article_attributes_no_contributor_rights.xml.mustache");
  private static final String SYSTEM_ATTRIBUTES_TEMPLATE =
      FileUtils.readFile("article/article_system_attributes.xml.mustache");

  private static final String TRANSACTION_ID = "tid_test";
  private static final String FALSE = "False";
  private static final String TRUE = "True";
  private static final String DRAFT_REF = "draftReference";
  private static final String PUBLISH_REF = "publishReference";
  private static final String API_HOST = "test.api.ft.com";
  private static final String WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";
  private static final String CANONICAL_WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";

  private static final String lastPublicationDateAsString = "20130813145815";
  private static final String initialPublicationDateAsString = "20120813145815";
  private static final String initialPublicationDateAsStringPreWfsEnforce = "20110513145815";

  private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
  private static final String EXPECTED_TITLE =
      "And sacked chimney-sweep pumps boss full of mayonnaise.";
  private static final String OBJECT_LOCATION =
      "/FT/Content/Companies/Stories/Live/Trump election victory business reaction WO 9.xml";

  private static final String WORK_FOLDER_COMPANIES = "/FT/Companies";
  private static final String SUB_FOLDER_RETAIL = "Retail &amp; Consumer";
  private static final String ES_SUB_FOLDER_RETAIL = "Retail & Consumer";

  private static final String INTERNAL_ANALYTICS_TAGS =
      "sometag, another_tag, tag with spaces, mixedCaseTag, House &amp; Home, Less &lt; More, Life+Arts";
  private static final String ES_INTERNAL_ANALYTICS_TAGS =
      "sometag, another_tag, tag with spaces, mixedCaseTag, House & Home, Less < More, Life+Arts";

  private static final String TRANSFORMED_BODY = "<body><p>some other random text</p></body>";
  private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
  private static final String EMPTY_BODY = "<body></body>";
  private static final Date LAST_MODIFIED = new Date();
  private static final String SUBSCRIPTION_LEVEL =
      Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel());

  private static final String TEMPLATE_PLACEHOLDER_MAINIMAGE = "mainImageUuid";
  private static final String TEMPLATE_PLACEHOLDER_PROMO_TITLE = "promoTitle";
  private static final String TEMPLATE_PLACEHOLDER_STANDFIRST = "standfirst";
  private static final String TEMPLATE_PLACEHOLDER_BYLINE = "byline";
  private static final String TEMPLATE_PLACEHOLDER_IMAGE_SET_UUID = "imageSetID";
  private static final String TEMPLATE_CONTENT_PACKAGE_TITLE = "contentPackageTitle";
  private static final String TEMPLATE_WEB_SUBHEAD = "webSubhead";

  private static final String IMAGE_SET_UUID = "U116035516646705FC";
  private static final String IG_UUID = "ce0cccf2-747a-11e8-b4ef-b1558cf87650";

  private final UUID uuid = UUID.randomUUID();

  @Rule public ExpectedException expectedException = ExpectedException.none();
  private FieldTransformer bodyTransformer;
  private FieldTransformer bylineTransformer;
  private BodyProcessor htmlFieldProcessor;

  private Map<ContentSource, Brand> contentSourceBrandMap;
  private EomFile standardEomFile;
  private Content standardExpectedContent;

  private EomFileProcessor eomFileProcessor;

  public static EomFile createStandardEomFileWithMainImage(
      UUID uuid, UUID mainImageUuid, String articleImageMetadataFlag) {
    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_PLACEHOLDER_MAINIMAGE, mainImageUuid);
    return new EomFile.Builder()
        .withUuid(uuid.toString())
        .withType(EOMCompoundStory.getTypeName())
        .withValue(buildEomFileValue(templateValues))
        .withAttributes(
            new EomFileAttributesBuilder(ATTRIBUTES_TEMPLATE)
                .withLastPublicationDate(lastPublicationDateAsString)
                .withInitialPublicationDate(initialPublicationDateAsString)
                .withMarkedDeleted(FALSE)
                .withImageMetadata(articleImageMetadataFlag)
                .withCommentsEnabled(FALSE)
                .withEditorsPick("")
                .withExclusive("")
                .withScoop("")
                .withEmbargoDate("")
                .withSourceCode("FT")
                .withContributorRights("")
                .withObjectLocation(OBJECT_LOCATION)
                .withSubscriptionLevel(Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel()))
                .withInternalAnalyticsTags(INTERNAL_ANALYTICS_TAGS)
                .build())
        .withSystemAttributes(
            buildEomFileSystemAttributes("FTcom", WORK_FOLDER_COMPANIES, SUB_FOLDER_RETAIL))
        .withWorkflowStatus(EomFile.WEB_READY)
        .build();
  }

  private static byte[] buildEomFileValue(Map<String, Object> templatePlaceholdersValues) {
    Template mustache = Mustache.compiler().escapeHTML(false).compile(ARTICLE_TEMPLATE);
    return mustache.execute(templatePlaceholdersValues).getBytes(UTF_8);
  }

  private static String buildEomFileSystemAttributes(
      String channel, String workFolder, String subFolder) {
    Template mustache = Mustache.compiler().compile(SYSTEM_ATTRIBUTES_TEMPLATE);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("channel", channel);
    attributes.put("workFolder", workFolder);
    attributes.put("subFolder", subFolder);
    return mustache.execute(attributes);
  }

  private static String buildEomFileAttributes(Map<String, Object> templatePlaceholdersValues) {
    Template mustache = Mustache.compiler().escapeHTML(false).compile(ATTRIBUTES_TEMPLATE);
    return mustache.execute(templatePlaceholdersValues);
  }

  private static Date toDate(String dateString, String format) {
    if (dateString == null || dateString.equals("")) {
      return null;
    }
    try {
      DateFormat dateFormat = new SimpleDateFormat(format);
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      return dateFormat.parse(dateString);
    } catch (ParseException e) {
      return null;
    }
  }

  @Before
  public void setUp() {
    bodyTransformer = mock(FieldTransformer.class);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(TRANSFORMED_BODY);

    bylineTransformer = mock(FieldTransformer.class);
    when(bylineTransformer.transform(anyString(), anyString(), eq(TransformationMode.PUBLISH)))
        .thenReturn(TRANSFORMED_BYLINE);

    htmlFieldProcessor = spy(new Html5SelfClosingTagBodyProcessor());

    contentSourceBrandMap = new HashMap<>();
    contentSourceBrandMap.put(ContentSource.FT, new Brand(FINANCIAL_TIMES_BRAND));
    contentSourceBrandMap.put(ContentSource.Reuters, new Brand(REUTERS_BRAND));
    contentSourceBrandMap.put(ContentSource.DynamicContent, new Brand(DYNAMIC_CONTENT));

    standardEomFile = createStandardEomFile(uuid);
    standardExpectedContent = createStandardExpectedFtContent();

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
  }

  @Test(expected = MethodeMarkedDeletedException.class)
  public void shouldThrowExceptionIfMarkedDeleted() {
    final EomFile eomFile =
        new EomFile.Builder().withValuesFrom(createStandardEomFile(uuid, TRUE)).build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, new Date());
  }

  @Test(expected = EmbargoDateInTheFutureException.class)
  public void shouldThrowExceptionIfEmbargoDateInTheFuture() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFileWithEmbargoDateInTheFuture(uuid))
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = UnsupportedObjectTypeException.class)
  public void shouldThrowUnsupportedObjectTypeExceptionIfObjectLocationEmpty() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFileWithObjectLocation(uuid, ""))
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, new Date());
  }

  @Test(expected = UnsupportedObjectTypeException.class)
  public void shouldThrowUnsupportedObjectTypeExceptionIfObjectTypeIsNotSupported() {
    String objectLocation =
        "/FT/Content/Companies/Stories/Live/Trump election victory business reaction WO 9.doc";
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFileWithObjectLocation(uuid, objectLocation))
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, new Date());
  }

  @Test(expected = NotWebChannelException.class)
  public void shouldThrowExceptionIfNoFtComChannel() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFile(uuid))
            .withSystemAttributes(
                buildEomFileSystemAttributes("NotFTcom", WORK_FOLDER_COMPANIES, SUB_FOLDER_RETAIL))
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = SourceNotEligibleForPublishException.class)
  public void shouldThrowExceptionIfNotFtSource() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFileNonFtOrAgencySource(uuid))
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = WorkflowStatusNotEligibleForPublishException.class)
  public void shouldThrowExceptionIfWorkflowStatusNotEligibleForPublishing() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFile(uuid))
            .withWorkflowStatus("Stories/Edit")
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void shouldAllowEOMStoryWithNonEligibleWorkflowStatusBeforeEnforceDate() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(
                createEomStoryFile(
                    uuid,
                    "FTContentMove/Ready",
                    "FTcom",
                    initialPublicationDateAsStringPreWfsEnforce))
            .build();

    String expectedBody = "<body id=\"some-random-value\"><foo/></body>";
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(expectedBody);

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(PUBLISH_REF, TRANSACTION_ID)
            .withFirstPublishedDate(
                toDate(initialPublicationDateAsStringPreWfsEnforce, DATE_TIME_FORMAT))
            .withWebUrl(URI.create(String.format(WEB_URL_TEMPLATE, uuid)))
            .withCanonicalWebUrl(URI.create(String.format(CANONICAL_WEB_URL_TEMPLATE, uuid)))
            .withXmlBody(expectedBody)
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    verify(bodyTransformer)
        .transform(
            anyString(),
            eq(TRANSACTION_ID),
            eq(TransformationMode.PUBLISH),
            eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
            eq(Maps.immutableEntry("apiHost", API_HOST)));
    assertThat(content, equalTo(expectedContent));
  }

  @Test(expected = WorkflowStatusNotEligibleForPublishException.class)
  public void shouldNotAllowEOMStoryWithNonEligibleWorkflowStatusAfterEnforceDate() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(
                createEomStoryFile(
                    uuid, "FTContentMove/Ready", "FTcom", initialPublicationDateAsString))
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void shouldAllowEOMStoryWithFinancialTimesChannelAndNonEligibleWorkflowStatus() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(
                createEomStoryFile(
                    uuid, "FTContentMove/Ready", "Financial Times", initialPublicationDateAsString))
            .build();

    String expectedBody = "<body id=\"some-random-value\"><foo/></body>";
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(expectedBody);

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(PUBLISH_REF, TRANSACTION_ID)
            .withWebUrl(URI.create(String.format(WEB_URL_TEMPLATE, uuid)))
            .withCanonicalWebUrl(URI.create(String.format(CANONICAL_WEB_URL_TEMPLATE, uuid)))
            .withXmlBody(expectedBody)
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    verify(bodyTransformer)
        .transform(
            anyString(),
            eq(TRANSACTION_ID),
            eq(TransformationMode.PUBLISH),
            eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
            eq(Maps.immutableEntry("apiHost", API_HOST)));
    assertThat(content, equalTo(expectedContent));
  }

  @Test(expected = UnsupportedEomTypeException.class)
  public void shouldThrowUnsupportedTypeExceptionIfPublishingDwc() {
    final EomFile eomFile =
        new EomFile.Builder().withValuesFrom(createDwcComponentFile(uuid)).build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = MethodeMissingFieldException.class)
  public void shouldThrowExceptionIfNoLastPublicationDate() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFileWithNoLastPublicationDate(uuid))
            .build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = MethodeMissingFieldException.class)
  public void shouldThrowExceptionIfNoWorkFolder() {
    final EomFile eomFile =
        new EomFile.Builder().withValuesFrom(createStandardEomFileWithoutWorkFolder(uuid)).build();
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void shouldShowWorkFolderOnlyIfNoSubFolder() {
    final EomFile eomFile =
        new EomFile.Builder().withValuesFrom(createStandardEomFileWithoutSubFolder(uuid)).build();
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(content.getEditorialDesk(), equalTo(WORK_FOLDER_COMPANIES));
  }

  @Test
  public void shouldMapInternalAnalyticsTags() {
    final EomFile eomFile =
        new EomFile.Builder().withValuesFrom(createStandardEomFile(uuid)).build();
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(content.getInternalAnalyticsTags(), equalTo(ES_INTERNAL_ANALYTICS_TAGS));
  }

  @Test
  public void shouldMapWithNoInternalAnalyticsTags() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(createStandardEomFileWithoutInternalAnalytics(uuid))
            .build();
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(content.getInternalAnalyticsTags(), nullValue());
  }

  @Test
  public void shouldNotBarfOnExternalDtd() {
    Content content =
        eomFileProcessor.process(
            standardEomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    Content expectedContent = createStandardExpectedFtContent();
    assertThat(content, equalTo(expectedContent));
  }

  @Test
  public void shouldTransformBodyOnPublish() {
    final EomFile eomFile = new EomFile.Builder().withValuesFrom(standardEomFile).build();

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(PUBLISH_REF, TRANSACTION_ID)
            .withWebUrl(URI.create(String.format(WEB_URL_TEMPLATE, uuid)))
            .withCanonicalWebUrl(URI.create(String.format(CANONICAL_WEB_URL_TEMPLATE, uuid)))
            .withXmlBody(TRANSFORMED_BODY)
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    verify(bodyTransformer)
        .transform(
            anyString(),
            eq(TRANSACTION_ID),
            eq(TransformationMode.PUBLISH),
            eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
            eq(Maps.immutableEntry("apiHost", API_HOST)));
    assertThat(content, equalTo(expectedContent));
  }

  @Test
  public void shouldAllowBodyWithAttributes() {
    final EomFile eomFile = new EomFile.Builder().withValuesFrom(standardEomFile).build();

    String expectedBody = "<body id=\"some-random-value\"><foo/></body>";
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(expectedBody);

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(PUBLISH_REF, TRANSACTION_ID)
            .withWebUrl(URI.create(String.format(WEB_URL_TEMPLATE, uuid)))
            .withCanonicalWebUrl(URI.create(String.format(CANONICAL_WEB_URL_TEMPLATE, uuid)))
            .withXmlBody(expectedBody)
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    verify(bodyTransformer)
        .transform(
            anyString(),
            eq(TRANSACTION_ID),
            eq(TransformationMode.PUBLISH),
            eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
            eq(Maps.immutableEntry("apiHost", API_HOST)));
    assertThat(content, equalTo(expectedContent));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfBodyTagIsMissingFromTransformedBody() {
    final EomFile eomFile = createEomStoryFile(uuid);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn("<p>some other random text</p>");
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = UntransformableMethodeContentException.class)
  public void shouldThrowExceptionIfBodyIsNull() {
    final EomFile eomFile = createEomStoryFile(uuid);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(null);
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = UntransformableMethodeContentException.class)
  public void shouldThrowExceptionIfBodyIsEmpty() {
    final EomFile eomFile = createEomStoryFile(uuid);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn("");
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = UntransformableMethodeContentException.class)
  public void shouldThrowExceptionIfTransformedBodyIsBlank() {
    final EomFile eomFile = createEomStoryFile(uuid);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn("<body> \n \n \n </body>");
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = UntransformableMethodeContentException.class)
  public void shouldThrowExceptionIfTransformedBodyIsEmpty() {
    final EomFile eomFile = createEomStoryFile(uuid);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(EMPTY_BODY);
    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void thatPreviewEmptyTransformedBodyIsAllowed() {
    final EomFile eomFile = createEomStoryFile(uuid);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PREVIEW), anyVararg()))
        .thenReturn(EMPTY_BODY);
    Content actual =
        eomFileProcessor.process(eomFile, TransformationMode.PREVIEW, TRANSACTION_ID, new Date());
    assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
  }

  @Test
  public void thatSuggestEmptyTransformedBodyIsAllowed() {
    final EomFile eomFile = createEomStoryFile(uuid);
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.SUGGEST), anyVararg()))
        .thenReturn(EMPTY_BODY);
    Content actual =
        eomFileProcessor.process(eomFile, TransformationMode.SUGGEST, TRANSACTION_ID, new Date());
    assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
  }

  @Test
  public void shouldAddPublishReferenceToTransformedBody() {

    final String reference = "some unstructured reference";

    final EomFile eomFile = new EomFile.Builder().withValuesFrom(standardEomFile).build();

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withPublishReference(reference)
            .withXmlBody(TRANSFORMED_BODY)
            .build();

    Content content =
        eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, reference, LAST_MODIFIED);

    assertThat(content, equalTo(expectedContent));
  }

  @Test
  public void shouldAddTransactionIdAsPublishReferenceToTransformedBody() {

    final String reference = "some unstructured reference";

    final EomFile eomFile = new EomFile.Builder().withValuesFrom(standardEomFile).build();

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(PUBLISH_REF, reference)
            .withXmlBody(TRANSFORMED_BODY)
            .build();

    Content content =
        eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, reference, LAST_MODIFIED);

    assertThat(content, equalTo(expectedContent));
  }

  @Test
  public void shouldTransformBylineWhenPresentOnPublish() {
    String byline = "By <author-name>Gillian Tett</author-name>";

    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_PLACEHOLDER_BYLINE, byline);

    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(standardEomFile)
            .withValue(buildEomFileValue(templateValues))
            .build();

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(PUBLISH_REF, TRANSACTION_ID)
            .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
            .withByline(TRANSFORMED_BYLINE)
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    verify(bylineTransformer)
        .transform("<byline>" + byline + "</byline>", TRANSACTION_ID, TransformationMode.PUBLISH);
    assertThat(content, equalTo(expectedContent));
  }

  @Test
  public void
      shouldThrowMethodeContentNotEligibleForPublishExceptionWhenNotCompoundStoryOnPublish() {

    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(standardEomFile)
            .withType("EOM::SomethingElse")
            .build();

    expectedException.expect(MethodeContentNotEligibleForPublishException.class);
    expectedException.expect(
        hasProperty("message", equalTo("[EOM::SomethingElse] not an EOM::CompoundStory.")));

    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void testShouldAddMainImageIfPresent() throws Exception {
    final UUID imageUuid = UUID.randomUUID();
    final UUID expectedMainImageUuid = DeriveUUID.with(DeriveUUID.Salts.IMAGE_SET).from(imageUuid);
    final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid, "Primary size");

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getMainImage(), equalTo(expectedMainImageUuid.toString()));
  }

  @Test
  public void testMainImageIsNullIfMissing() throws Exception {
    final EomFile eomFile = createStandardEomFile(uuid);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getMainImage(), nullValue());
  }

  @Test
  public void testMainImageReferenceIsPutInBodyWhenPresentAndPrimarySizeFlag() throws Exception {
    String expectedTransformedBody =
        "<body><content data-embedded=\"true\" id=\"%s\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content>"
            + "                <p>random text for now</p>"
            + "            </body>";
    testMainImageReferenceIsPutInBodyWithMetadataFlag("Primary size", expectedTransformedBody);
  }

  @Test
  public void testMainImageReferenceIsPutInBodyWhenPresentAndArticleSizeFlag() throws Exception {
    String expectedTransformedBody =
        "<body><content data-embedded=\"true\" id=\"%s\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content>"
            + "                <p>random text for now</p>"
            + "            </body>";
    testMainImageReferenceIsPutInBodyWithMetadataFlag("Article size", expectedTransformedBody);
  }

  @Test
  public void testMainImageReferenceIsNotPutInBodyWhenPresentButNoPictureFlag() throws Exception {
    String expectedTransformedBody =
        "<body>" + "                <p>random text for now</p>" + "            </body>";
    testMainImageReferenceIsPutInBodyWithMetadataFlag("No picture", expectedTransformedBody);
  }

  @Test
  public void testMainImageReferenceIsNotPutInBodyWhenMissing() throws Exception {
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .then(returnsFirstArg());
    final EomFile eomFile = createStandardEomFile(uuid);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    String expectedBody =
        "<body>" + "                <p>random text for now</p>" + "            </body>";
    assertThat(content.getMainImage(), nullValue());
    assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
  }

  @Test
  public void testCommentsArePresent() {
    final EomFile eomFile = createStandardEomFile(uuid);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(content.getComments(), notNullValue());
    assertThat(content.getComments().isEnabled(), is(true));
  }

  @Test
  public void testStandoutFieldsArePresent() {
    final EomFile eomFile = createStandardEomFile(uuid);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getStandout().isEditorsChoice(), is(true));
    assertThat(content.getStandout().isExclusive(), is(true));
    assertThat(content.getStandout().isScoop(), is(true));
  }

  @Test(expected = MethodeMissingBodyException.class)
  public void thatTransformationFailsIfThereIsNoBody() throws Exception {

    String value = FileUtils.readFile("article/article_value_with_no_body.xml");
    final EomFile eomFile =
        new EomFile.Builder()
            .withValuesFrom(standardEomFile)
            .withValue(value.getBytes(UTF_8))
            .build();

    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void testArticleCanBeSyndicated() {
    final EomFile eomFile =
        createStandardEomFileWithContributorRights(
            uuid, ContributorRights.ALL_NO_PAYMENT.getValue());

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getCanBeSyndicated(), is(Syndication.YES));
  }

  @Test
  public void testArticleCanBeSyndicatedWithContributorPayment() {
    String[] contributorRights = {
      ContributorRights.FIFTY_FIFTY_NEW.getValue(), ContributorRights.FIFTY_FIFTY_OLD.getValue()
    };

    for (String contributorRight : contributorRights) {
      final EomFile eomFileContractContributorRights =
          createStandardEomFileWithContributorRights(uuid, contributorRight);

      Content content =
          eomFileProcessor.process(
              eomFileContractContributorRights,
              TransformationMode.PUBLISH,
              TRANSACTION_ID,
              LAST_MODIFIED);
      assertThat(content.getCanBeSyndicated(), is(Syndication.WITH_CONTRIBUTOR_PAYMENT));
    }
  }

  @Test
  public void testArticleCanNotBeSyndicated() {
    final EomFile eomFile =
        createStandardEomFileWithContributorRights(uuid, ContributorRights.NO_RIGHTS.getValue());

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getCanBeSyndicated(), is(Syndication.NO));
  }

  @Test
  public void testArticleNeedsToBeVerifiedForSyndication() {
    String[] contributorRights = {
      ContributorRights.CONTRACT.getValue(), "", "invalid value", "-1", "6"
    };

    for (String contributorRight : contributorRights) {
      final EomFile eomFileContractContributorRights =
          createStandardEomFileWithContributorRights(uuid, contributorRight);

      Content content =
          eomFileProcessor.process(
              eomFileContractContributorRights,
              TransformationMode.PUBLISH,
              TRANSACTION_ID,
              LAST_MODIFIED);
      assertThat(
          "contributorRight=" + contributorRight,
          content.getCanBeSyndicated(),
          is(Syndication.VERIFY));
    }
  }

  @Test
  public void testArticleWithMissingContributorRights() {
    final EomFile eomFile =
        new EomFile.Builder()
            .withUuid(uuid.toString())
            .withType(EOMCompoundStory.getTypeName())
            .withValue(buildEomFileValue(new HashMap<>()))
            .withAttributes(
                new EomFileAttributesBuilder(ATTRIBUTES_TEMPLATE_NO_CONTRIBUTOR_RIGHTS)
                    .withLastPublicationDate(lastPublicationDateAsString)
                    .withInitialPublicationDate(initialPublicationDateAsString)
                    .withMarkedDeleted(FALSE)
                    .withImageMetadata("No picture")
                    .withCommentsEnabled(TRUE)
                    .withEditorsPick("Yes")
                    .withExclusive("Yes")
                    .withScoop("Yes")
                    .withEmbargoDate("")
                    .withSourceCode("FT")
                    .withObjectLocation(OBJECT_LOCATION)
                    .withSubscriptionLevel(
                        Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel()))
                    .withInternalAnalyticsTags(INTERNAL_ANALYTICS_TAGS)
                    .build())
            .withSystemAttributes(
                buildEomFileSystemAttributes("FTcom", WORK_FOLDER_COMPANIES, SUB_FOLDER_RETAIL))
            .withWorkflowStatus(EomFile.WEB_READY)
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getCanBeSyndicated(), is(Syndication.VERIFY));
  }

  @Test
  public void testArticleWithNoSubscriptionLevelHasNullAccessLevel() {
    final EomFile eomFile = createStandardEomFileWithSubscriptionLevel(uuid, "");
    expectedException.expect(UntransformableMethodeContentException.class);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertNull(content.getAccessLevel());
  }

  @Test
  public void testArticleWithUnknownSubscriptionLevelHasNullAccessLevel() {
    final EomFile eomFile = createStandardEomFileWithSubscriptionLevel(uuid, "5");
    expectedException.expect(UntransformableMethodeContentException.class);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertNull(content.getAccessLevel());
  }

  @Test
  public void testArticleHasSubscribedAccessLevel() {
    final EomFile eomFile =
        createStandardEomFileWithSubscriptionLevel(
            uuid, Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel()));
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getAccessLevel(), is(AccessLevel.SUBSCRIBED));
  }

  @Test
  public void testArticleHasFreeAccessLevel() {
    final EomFile eomFile =
        createStandardEomFileWithSubscriptionLevel(
            uuid, Integer.toString(SHOWCASE.getSubscriptionLevel()));

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getAccessLevel(), is(AccessLevel.FREE));
  }

  @Test
  public void testArticleHasPremiumAccessLevel() {
    final EomFile eomFile =
        createStandardEomFileWithSubscriptionLevel(
            uuid, Integer.toString(PREMIUM.getSubscriptionLevel()));

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getAccessLevel(), is(AccessLevel.PREMIUM));
  }

  /**
   * Tests that a non-standard old type EOM::Story is also a valid type. If the test fails exception
   * will be thrown.
   */
  @Test
  public void thatStoryTypeIsAValidType() {
    final EomFile eomFile = createEomStoryFile(uuid);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content, notNullValue());
  }

  @Test
  public void thatStandfirstIsPresent() {
    final String expectedStandfirst = "Test standfirst";

    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_PLACEHOLDER_STANDFIRST, expectedStandfirst);

    final EomFile eomFile =
        (new EomFile.Builder())
            .withValuesFrom(createStandardEomFile(uuid))
            .withValue(buildEomFileValue(templateValues))
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getStandfirst(), is(equalToIgnoringWhiteSpace(expectedStandfirst)));
  }

  @Test
  public void thatWhitespaceStandfirstIsTreatedAsAbsent() {
    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_PLACEHOLDER_STANDFIRST, "\n");

    final EomFile eomFile =
        (new EomFile.Builder())
            .withValuesFrom(createStandardEomFile(uuid))
            .withValue(buildEomFileValue(templateValues))
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getStandfirst(), is(nullValue()));
  }

  @Test
  public void thatStandfirstIsOptional() {
    final EomFile eomFile = createStandardEomFile(uuid);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getStandfirst(), is(nullValue()));
  }

  @Test
  public void thatAlternativeTitlesArePresent() {
    final String promoTitle = "Test Promo Title";
    final String contentPackageTitle = "Test Content Package Title";

    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_PLACEHOLDER_PROMO_TITLE, promoTitle);
    templateValues.put(TEMPLATE_CONTENT_PACKAGE_TITLE, contentPackageTitle);

    final EomFile eomFile =
        (new EomFile.Builder())
            .withValuesFrom(createStandardEomFile(uuid))
            .withValue(buildEomFileValue(templateValues))
            .build();

    final Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content, is(notNullValue()));

    final AlternativeTitles actual = content.getAlternativeTitles();
    assertThat(actual, is(notNullValue()));
    assertThat(actual.getPromotionalTitle(), is(equalToIgnoringWhiteSpace(promoTitle)));
    assertThat(actual.getContentPackageTitle(), is(equalToIgnoringWhiteSpace(contentPackageTitle)));
  }

  @Test
  public void thatAlternativeTitlesAreOptional() {
    final EomFile eomFile = createStandardEomFile(uuid);

    final Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content, is(notNullValue()));

    final AlternativeTitles actual = content.getAlternativeTitles();
    assertThat(actual, is(notNullValue()));
    assertThat(actual.getPromotionalTitle(), is(nullValue()));
    assertThat(actual.getContentPackageTitle(), is(nullValue()));
  }

  @Test
  public void thatWhitespaceAlternativeTitlesAreTreatedAsAbsent() {
    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_PLACEHOLDER_PROMO_TITLE, "\n");
    templateValues.put(TEMPLATE_CONTENT_PACKAGE_TITLE, "\t");

    final EomFile eomFile =
        (new EomFile.Builder())
            .withValuesFrom(createStandardEomFile(uuid))
            .withValue(buildEomFileValue(templateValues))
            .build();

    final Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content, is(notNullValue()));

    final AlternativeTitles actual = content.getAlternativeTitles();
    assertThat(actual, is(notNullValue()));
    assertThat(actual.getPromotionalTitle(), is(nullValue()));
    assertThat(actual.getContentPackageTitle(), is(nullValue()));
  }

  @Test
  public void thatDummyAlternativeTitlesAreTreatedAsAbsent() {
    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_PLACEHOLDER_PROMO_TITLE, "<?EM-dummyText promo title here... ?>");
    templateValues.put(
        TEMPLATE_CONTENT_PACKAGE_TITLE, "<?EM-dummyText content package title here... ?>");

    final EomFile eomFile =
        (new EomFile.Builder())
            .withValuesFrom(createStandardEomFile(uuid))
            .withValue(buildEomFileValue(templateValues))
            .build();

    final Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content, is(notNullValue()));

    final AlternativeTitles actual = content.getAlternativeTitles();
    assertThat(actual, is(notNullValue()));
    assertThat(actual.getPromotionalTitle(), is(nullValue()));
    assertThat(actual.getContentPackageTitle(), is(nullValue()));
  }

  @Test
  public void thatWebUrlIsSet() {
    final EomFile eomFile = createStandardEomFile(uuid);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getWebUrl(), is(equalTo(URI.create(String.format(WEB_URL_TEMPLATE, uuid)))));
  }

  @Test
  public void thatCanonicalWebUrlIsSet() {
    final EomFile eomFile = createStandardEomFile(uuid);

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(
        content.getCanonicalWebUrl(),
        is(equalTo(URI.create(String.format(CANONICAL_WEB_URL_TEMPLATE, uuid)))));
  }

  @Test
  public void testAgencyContentProcessPublication() {
    final EomFile eomFile = createStandardEomFileAgencySource(uuid);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    final Content expectedContent = createStandardExpectedAgencyContent();

    assertThat(content, equalTo(expectedContent));
  }

  @Test
  public void testAgencyContentProcessPreview() {
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PREVIEW), anyVararg()))
        .thenReturn(TRANSFORMED_BODY);

    final EomFile eomFile = createStandardEomFileAgencySource(uuid);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PREVIEW, TRANSACTION_ID, LAST_MODIFIED);

    final Content expectedContent = createStandardExpectedAgencyContent();

    assertThat(content, equalTo(expectedContent));
  }

  @Test
  public void testTypeArticleIsDefaultSetIfNoContentPackage() {
    final EomFile eomFile = createStandardEomFile(uuid);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(ContentType.Type.ARTICLE, equalTo(content.getType()));
  }

  @Test
  public void testImageSet() {
    String expectedUUID = UUID.nameUUIDFromBytes(IMAGE_SET_UUID.getBytes(UTF_8)).toString();
    String expectedBody =
        "<body>"
            + "<p>random text for now</p>"
            + "<ft-content type=\"http://www.ft.com/ontology/content/ImageSet\" url=\"http://api.ft.com/content/"
            + expectedUUID
            + "\" data-embedded=\"true\"></ft-content>"
            + "</body>";
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .thenReturn(expectedBody);

    EomFile eomFile = createStandardEomFileWithImageSet(IMAGE_SET_UUID);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
  }

  @Test
  public void testImageSetPreview() {
    String expectedUUID = UUID.nameUUIDFromBytes(IMAGE_SET_UUID.getBytes(UTF_8)).toString();
    String expectedBody =
        "<body>"
            + "<p>random text for now</p>"
            + "<ft-content type=\"http://www.ft.com/ontology/content/ImageSet\" url=\"http://api.ft.com/content/"
            + expectedUUID
            + "\" data-embedded=\"true\"></ft-content>"
            + "</body>";
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PREVIEW), anyVararg()))
        .thenReturn(expectedBody);

    EomFile eomFile = createStandardEomFileWithImageSet(IMAGE_SET_UUID);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PREVIEW, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
  }

  @Test
  public void thatAlternativeStandfirtstIsPresent() {
    final String expectedPromotionalStandfirst = "Test promotional Standfirst";

    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put(TEMPLATE_WEB_SUBHEAD, expectedPromotionalStandfirst);

    final EomFile eomFile =
        (new EomFile.Builder())
            .withValuesFrom(createStandardEomFile(uuid))
            .withValue(buildEomFileValue(templateValues))
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(
        content.getAlternativeStandfirsts().getPromotionalStandfirst(),
        is(equalToIgnoringWhiteSpace(expectedPromotionalStandfirst)));
  }

  @Test
  public void thatAlternativeStandfirtstIsNotPresent() {
    final EomFile eomFile =
        (new EomFile.Builder()).withValuesFrom(createStandardEomFile(uuid)).build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getAlternativeStandfirsts().getPromotionalStandfirst(), is(nullValue()));
  }

  @Test
  public void thatSuggestModeIsPassedThrough() {
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.SUGGEST), anyVararg()))
        .thenReturn(TRANSFORMED_BODY);

    final EomFile eomFile = new EomFile.Builder().withValuesFrom(standardEomFile).build();

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(PUBLISH_REF, TRANSACTION_ID)
            .withXmlBody(TRANSFORMED_BODY)
            .build();

    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.SUGGEST, TRANSACTION_ID, LAST_MODIFIED);

    verify(bodyTransformer)
        .transform(
            anyString(),
            eq(TRANSACTION_ID),
            eq(TransformationMode.SUGGEST),
            eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
            eq(Maps.immutableEntry("apiHost", API_HOST)));
    assertThat(content, equalTo(expectedContent));
  }

  @Test(expected = UnsupportedTransformationModeException.class)
  public void thatUnsupportedModeIsRejected() {
    eomFileProcessor =
        new EomFileProcessor(
            EnumSet.of(TransformationMode.SUGGEST),
            bodyTransformer,
            bylineTransformer,
            htmlFieldProcessor,
            contentSourceBrandMap,
            PUBLISH_REF,
            API_HOST,
            WEB_URL_TEMPLATE,
            CANONICAL_WEB_URL_TEMPLATE);

    final EomFile eomFile = new EomFile.Builder().withValuesFrom(standardEomFile).build();

    eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void thatDraftReferenceIsAddedToTransformedBody() {
    EomFile.setAdditionalMappings(Collections.singletonMap(DRAFT_REF, DRAFT_REF));
    eomFileProcessor =
        new EomFileProcessor(
            EnumSet.allOf(TransformationMode.class),
            bodyTransformer,
            bylineTransformer,
            htmlFieldProcessor,
            contentSourceBrandMap,
            DRAFT_REF,
            API_HOST,
            WEB_URL_TEMPLATE,
            CANONICAL_WEB_URL_TEMPLATE);

    final String reference = "test_draft";

    final EomFile eomFile = new EomFile.Builder().withValuesFrom(standardEomFile).build();

    final Content expectedContent =
        Content.builder()
            .withValuesFrom(standardExpectedContent)
            .withTransactionId(DRAFT_REF, reference)
            .withXmlBody(TRANSFORMED_BODY)
            .build();

    Content content =
        eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, reference, LAST_MODIFIED);

    assertThat(content, equalTo(expectedContent));
  }

  private void testMainImageReferenceIsPutInBodyWithMetadataFlag(
      String articleImageMetadataFlag, String expectedTransformedBody) {
    when(bodyTransformer.transform(
            anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg()))
        .then(returnsFirstArg());
    final UUID imageUuid = UUID.randomUUID();
    final UUID expectedMainImageUuid = DeriveUUID.with(DeriveUUID.Salts.IMAGE_SET).from(imageUuid);
    final EomFile eomFile =
        createStandardEomFileWithMainImage(uuid, imageUuid, articleImageMetadataFlag);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

    String expectedBody = String.format(expectedTransformedBody, expectedMainImageUuid);
    assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
  }

  @Test
  public void testDCTypeIsSet() {
    Map<String, Object> attributesTemplateValues = new HashMap<>();
    attributesTemplateValues.put("sourceCode", "DynamicContent");

    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put("ig-uuid", IG_UUID);

    final EomFile eomFile = createDynamicContent(attributesTemplateValues, templateValues);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getType(), equalTo(ContentType.Type.DYNAMIC_CONTENT));
  }

  @Test
  public void testIdentifiersIsSet() {
    Map<String, Object> attributesTemplateValues = new HashMap<>();
    attributesTemplateValues.put("sourceCode", "DynamicContent");

    Map<String, Object> templateValues = new HashMap<>();
    templateValues.put("ig-uuid", IG_UUID);

    final EomFile eomFile = createDynamicContent(attributesTemplateValues, templateValues);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getIdentifiers().first().getAuthority(), equalTo(IG));
    assertThat(
        content.getIdentifiers().first().getIdentifierValue(),
        equalTo("ce0cccf2-747a-11e8-b4ef-b1558cf87650"));
    assertThat(content.getIdentifiers().last().getAuthority(), equalTo(METHODE));
    assertThat(content.getIdentifiers().last().getIdentifierValue(), equalTo(uuid.toString()));
  }

  @Test(expected = MissingInteractiveGraphicUuidException.class)
  public void testIGUUIDisEmpty() {
    Map<String, Object> attributesTemplateValues = new HashMap<>();
    attributesTemplateValues.put("sourceCode", "DynamicContent");

    Map<String, Object> valueTemplateValues = new HashMap<>();
    valueTemplateValues.put("ig-uuid", "");

    final EomFile eomFile = createDynamicContent(attributesTemplateValues, valueTemplateValues);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getIdentifiers().first().getAuthority(), equalTo(IG));
    assertThat(content.getIdentifiers().first().getIdentifierValue(), equalTo(""));
    assertThat(content.getIdentifiers().last().getAuthority(), equalTo(METHODE));
    assertThat(content.getIdentifiers().last().getIdentifierValue(), equalTo(uuid.toString()));
  }

  @Test(expected = MissingInteractiveGraphicUuidException.class)
  public void testIGUUIDisNull() {
    Map<String, Object> attributesTemplateValues = new HashMap<>();
    attributesTemplateValues.put("sourceCode", "DynamicContent");

    Map<String, Object> valueTemplateValues = new HashMap<>();
    valueTemplateValues.put("ig-uuid", null);

    final EomFile eomFile = createDynamicContent(attributesTemplateValues, valueTemplateValues);
    Content content =
        eomFileProcessor.process(
            eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    assertThat(content.getIdentifiers().first().getAuthority(), equalTo(IG));
    assertThat(content.getIdentifiers().first().getIdentifierValue(), equalTo(null));
    assertThat(content.getIdentifiers().last().getAuthority(), equalTo(METHODE));
    assertThat(content.getIdentifiers().last().getIdentifierValue(), equalTo(uuid.toString()));
  }
  /**
   * Creates EomFile with an non-standard type EOM::Story as opposed to EOM::CompoundStory which is
   * standard.
   *
   * @param uuid uuid of an article
   * @return EomFile
   */
  private EomFile createEomStoryFile(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFile(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithObjectLocation(UUID uuid, String objectLocation) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMCompoundStory.getTypeName(),
        "",
        objectLocation,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createEomStoryFile(
      UUID uuid, String workflowStatus, String channel, String initialPublicationDate) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        channel,
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        workflowStatus,
        lastPublicationDateAsString,
        initialPublicationDate,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileNonFtOrAgencySource(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "Pepsi",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        FALSE,
        "",
        "",
        "",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFile(UUID uuid, String markedDeleted) {
    return createStandardEomFile(
        uuid,
        markedDeleted,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        FALSE,
        "",
        "",
        "",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileAgencySource(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "REU2",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithEmbargoDateInTheFuture(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        true,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        FALSE,
        "",
        "",
        "",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithNoLastPublicationDate(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        "",
        initialPublicationDateAsString,
        FALSE,
        "",
        "",
        "",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithoutWorkFolder(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        "",
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        "",
        initialPublicationDateAsString,
        FALSE,
        "",
        "",
        "",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithoutSubFolder(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        "",
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithoutInternalAnalytics(UUID uuid) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        "",
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        "");
  }

  private EomFile createStandardEomFileWithContributorRights(UUID uuid, String contributorRights) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMCompoundStory.getTypeName(),
        contributorRights,
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithSubscriptionLevel(UUID uuid, String subscriptionLevel) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMCompoundStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        subscriptionLevel,
        null,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFileWithImageSet(String imageSetID) {
    return createStandardEomFile(
        uuid,
        FALSE,
        false,
        "FTcom",
        WORK_FOLDER_COMPANIES,
        SUB_FOLDER_RETAIL,
        "FT",
        EomFile.WEB_READY,
        lastPublicationDateAsString,
        initialPublicationDateAsString,
        TRUE,
        "Yes",
        "Yes",
        "Yes",
        EOMStory.getTypeName(),
        "",
        OBJECT_LOCATION,
        SUBSCRIPTION_LEVEL,
        imageSetID,
        INTERNAL_ANALYTICS_TAGS);
  }

  private EomFile createStandardEomFile(
      UUID uuid,
      String markedDeleted,
      boolean embargoDateInTheFuture,
      String channel,
      String workFolder,
      String subFolder,
      String sourceCode,
      String workflowStatus,
      String lastPublicationDateAsString,
      String initialPublicationDateAsString,
      String commentsEnabled,
      String editorsPick,
      String exclusive,
      String scoop,
      String eomType,
      String contributorRights,
      String objectLocation,
      String subscriptionLevel,
      String imageSetID,
      String internalAnalyticsTags) {

    String embargoDate = "";
    if (embargoDateInTheFuture) {
      embargoDate = dateInTheFutureAsStringInMethodeFormat();
    }

    Map<String, Object> templateValues = new HashMap<>();

    templateValues.put(TEMPLATE_PLACEHOLDER_IMAGE_SET_UUID, imageSetID);

    return new EomFile.Builder()
        .withUuid(uuid.toString())
        .withType(eomType)
        .withValue(buildEomFileValue(templateValues))
        .withAttributes(
            new EomFileAttributesBuilder(ATTRIBUTES_TEMPLATE)
                .withLastPublicationDate(lastPublicationDateAsString)
                .withInitialPublicationDate(initialPublicationDateAsString)
                .withMarkedDeleted(markedDeleted)
                .withImageMetadata("No picture")
                .withCommentsEnabled(commentsEnabled)
                .withEditorsPick(editorsPick)
                .withExclusive(exclusive)
                .withScoop(scoop)
                .withEmbargoDate(embargoDate)
                .withSourceCode(sourceCode)
                .withContributorRights(contributorRights)
                .withObjectLocation(objectLocation)
                .withSubscriptionLevel(subscriptionLevel)
                .withContentPackageFlag(false)
                .withInternalAnalyticsTags(internalAnalyticsTags)
                .build())
        .withSystemAttributes(buildEomFileSystemAttributes(channel, workFolder, subFolder))
        .withWorkflowStatus(workflowStatus)
        .build();
  }

  private EomFile createDwcComponentFile(UUID uuid) {

    return new EomFile.Builder()
        .withUuid(uuid.toString())
        .withType("EOM::WebContainer")
        .withValue(
            ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<!-- $Id: editorsChoice.dwc,v 1.1 2005/08/05 15:19:37 alant Exp $ -->\n"
                    + "<!--\n"
                    + "    Note: The dwc should simply define the dwcContent entity and reference it\n"
                    + "-->\n"
                    + "<!DOCTYPE dwc SYSTEM \"/FTcom Production/ZZZ_Templates/DWC/common/ref/dwc.dtd\" [\n"
                    + "\n"
                    + "    <!ENTITY dwcContent SYSTEM \"/FTcom Production/ZZZ_Templates/DWC/editorsChoice/ref/editorsChoice_dwc.xml\">\n"
                    + "    <!ENTITY % entities SYSTEM \"/FTcom Production/ZZZ_Templates/DWC/common/ref/entities.xml\">\n"
                    + "    %entities;\n"
                    + "]>\n"
                    + "<dwc>\n"
                    + "&dwcContent;\n"
                    + "</dwc>\n")
                .getBytes(UTF_8))
        .withAttributes(
            "<!DOCTYPE ObjectMetadata SYSTEM \"/SysConfig/Classify/FTDWC2/classify.dtd\">\n"
                + "<ObjectMetadata><FTcom><DIFTcomWebType>editorsChoice_2</DIFTcomWebType>\n"
                + "<autoFill/>\n"
                + "<footwellDedupe/>\n"
                + "<displayCode/>\n"
                + "<searchAge>1</searchAge>\n"
                + "<agingRule>1</agingRule>\n"
                + "<markDeleted>False</markDeleted>\n"
                + "</FTcom>\n"
                + "</ObjectMetadata>")
        .withSystemAttributes("<props><productInfo><name>FTcom</name></productInfo></props>")
        .withWorkflowStatus("") // This is what DWCs get.
        .build();
  }

  private EomFile createDynamicContent(
      Map<String, Object> attributesTemplateValues, Map<String, Object> templateValues) {
    attributesTemplateValues.put("lastPublicationDate", lastPublicationDateAsString);
    attributesTemplateValues.put("initialPublicationDate", initialPublicationDateAsString);
    attributesTemplateValues.put("subscriptionLevel", SUBSCRIPTION_LEVEL);
    attributesTemplateValues.put("objectLocation", OBJECT_LOCATION);
    attributesTemplateValues.put(
        "internalAnalyticsTags",
        "<InternalAnalyticsTags>{{internalAnalyticsTags}}</InternalAnalyticsTags>");

    return new EomFile.Builder()
        .withUuid(uuid.toString())
        .withType(EOMCompoundStory.getTypeName())
        .withValue(buildEomFileValue(templateValues))
        .withAttributes(buildEomFileAttributes(attributesTemplateValues))
        .withSystemAttributes(
            buildEomFileSystemAttributes("FTcom", WORK_FOLDER_COMPANIES, SUB_FOLDER_RETAIL))
        .withWorkflowStatus(EomFile.WEB_READY)
        .build();
  }

  private String dateInTheFutureAsStringInMethodeFormat() {
    return dateFromNowInMethodeFormat(10);
  }

  private String dateFromNowInMethodeFormat(int timeDifference) {
    Date currentDate = new Date(System.currentTimeMillis());
    Calendar cal = Calendar.getInstance();
    cal.setTime(currentDate);
    cal.add(Calendar.DATE, timeDifference);

    DateFormat methodeDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
    methodeDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return methodeDateFormat.format(cal.getTime());
  }

  private Content createStandardExpectedFtContent() {
    return createStandardExpectedContent(ContentSource.FT);
  }

  private Content createStandardExpectedAgencyContent() {
    return createStandardExpectedContent(ContentSource.Reuters);
  }

  private Content createStandardExpectedContent(ContentSource contentSource) {
    return Content.builder()
        .withTitle(EXPECTED_TITLE)
        .withType(ContentType.Type.ARTICLE)
        .withXmlBody("<body><p>some other random text</p></body>")
        .withByline("")
        .withBrands(
            new TreeSet<>(Collections.singletonList(contentSourceBrandMap.get(contentSource))))
        .withPublishedDate(toDate(lastPublicationDateAsString, DATE_TIME_FORMAT))
        .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
        .withComments(new Comments(true))
        .withStandout(new Standout(true, true, true))
        .withUuid(uuid)
        .withPublishReference(TRANSACTION_ID)
        .withLastModified(LAST_MODIFIED)
        .withCanBeSyndicated(Syndication.VERIFY)
        .withFirstPublishedDate(toDate(initialPublicationDateAsString, DATE_TIME_FORMAT))
        .withAccessLevel(AccessLevel.SUBSCRIBED)
        .withCanBeDistributed(
            contentSource == ContentSource.FT
                ? Distribution.YES
                : contentSource == ContentSource.Reuters ? Distribution.NO : Distribution.VERIFY)
        .withAlternativeStandfirsts(new AlternativeStandfirsts(null))
        .withEditorialDesk(WORK_FOLDER_COMPANIES + "/" + ES_SUB_FOLDER_RETAIL)
        .withWebUrl(URI.create(String.format(WEB_URL_TEMPLATE, uuid)))
        .withCanonicalWebUrl(URI.create(String.format(CANONICAL_WEB_URL_TEMPLATE, uuid)))
        .withInternalAnalyticsTags(ES_INTERNAL_ANALYTICS_TAGS)
        .build();
  }
}
