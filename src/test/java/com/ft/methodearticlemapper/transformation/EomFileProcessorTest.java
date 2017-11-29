package com.ft.methodearticlemapper.transformation;

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
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.uuidutils.DeriveUUID;
import com.google.common.collect.ImmutableSortedSet;

import com.ft.methodearticlemapper.exception.EmbargoDateInTheFutureException;
import com.ft.methodearticlemapper.exception.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.SourceNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.UnsupportedEomTypeException;
import com.ft.methodearticlemapper.exception.UnsupportedObjectTypeException;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.exception.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticlemapper.model.EomFile;
import com.google.common.collect.Maps;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.UUID;

import static com.ft.methodearticlemapper.methode.EomFileType.EOMCompoundStory;
import static com.ft.methodearticlemapper.methode.EomFileType.EOMStory;
import static com.ft.methodearticlemapper.transformation.EomFileProcessor.METHODE;
import static com.ft.methodearticlemapper.transformation.SubscriptionLevel.*;
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


public class EomFileProcessorTest {
    public static final String FINANCIAL_TIMES_BRAND = "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String REUTERS_BRAND = "http://api.ft.com/things/ed3b6ec5-6466-47ef-b1d8-16952fd522c7";

    private static final String ARTICLE_TEMPLATE = FileUtils.readFile("article/article_value.xml.mustache");
    private static final String ATTRIBUTES_TEMPLATE = FileUtils.readFile("article/article_attributes.xml.mustache");
    private static final String ATTRIBUTES_TEMPLATE_NO_CONTRIBUTOR_RIGHTS = FileUtils.readFile("article/article_attributes_no_contributor_rights.xml.mustache");
    private static final String SYSTEM_ATTRIBUTES_TEMPLATE = FileUtils.readFile("article/article_system_attributes.xml.mustache");

    private static final String TRANSACTION_ID = "tid_test";
    private static final String FALSE = "False";
    private static final String TRUE = "True";
    private static final String API_HOST = "test.api.ft.com";

    private static final String lastPublicationDateAsString = "20130813145815";
    private static final String initialPublicationDateAsString = "20120813145815";
    private static final String initialPublicationDateAsStringPreWfsEnforce = "20110513145815";

    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
    private static final String EXPECTED_TITLE = "And sacked chimney-sweep pumps boss full of mayonnaise.";
    private static final String OBJECT_LOCATION = "/FT/Content/Companies/Stories/Live/Trump election victory business reaction WO 9.xml";

    private static final String TRANSFORMED_BODY = "<body><p>some other random text</p></body>";
    private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
    private static final String EMPTY_BODY = "<body></body>";
    private static final Date LAST_MODIFIED = new Date();
    private static final String SUBSCRIPTION_LEVEL = Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel());

    private static final String TEMPLATE_PLACEHOLDER_MAINIMAGE = "mainImageUuid";
    private static final String TEMPLATE_PLACEHOLDER_PROMO_TITLE = "promoTitle";
    private static final String TEMPLATE_PLACEHOLDER_STANDFIRST = "standfirst";
    private static final String TEMPLATE_PLACEHOLDER_BYLINE = "byline";
    private static final String TEMPLATE_PLACEHOLDER_STORY_PACKAGE_UUID = "storyPackageUuid";
    private static final String TEMPLATE_PLACEHOLDER_IMAGE_SET_UUID = "imageSetID";
    private static final String TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE = "contentPackage";
    private static final String TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_DESC = "contentPackageDesc";
    private static final String TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_LIST_HREF = "contentPackageListHref";
    private static final String TEMPLATE_CONTENT_PACKAGE_TITLE = "contentPackageTitle";
    private static final String TEMPLATE_WEB_SUBHEAD = "webSubhead";

    private static final String IMAGE_SET_UUID = "U116035516646705FC";

    private final UUID uuid = UUID.randomUUID();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private FieldTransformer bodyTransformer;
    private FieldTransformer bylineTransformer;
    private BodyProcessor htmlFieldProcessor;

    private Map<ContentSource, Brand> contentSourceBrandMap;

    private EomFile standardEomFile;
    private Content standardExpectedContent;

    private EomFileProcessor eomFileProcessor;

    public static EomFile createStandardEomFileWithMainImage(UUID uuid,
                                                             UUID mainImageUuid,
                                                             String articleImageMetadataFlag) {
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
                                .build())
                .withSystemAttributes(
                        buildEomFileSystemAttributes("FTcom"))
                .withWorkflowStatus(EomFile.WEB_READY)
                .build();
    }

    private static byte[] buildEomFileValue(Map<String, Object> templatePlaceholdersValues) {
        Template mustache = Mustache.compiler().escapeHTML(false).compile(ARTICLE_TEMPLATE);
        return mustache.execute(templatePlaceholdersValues).getBytes(UTF_8);
    }

    private static String buildEomFileSystemAttributes(String channel) {
        Template mustache = Mustache.compiler().compile(SYSTEM_ATTRIBUTES_TEMPLATE);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("channel", channel);
        return mustache.execute(attributes);
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
    public void setUp() throws Exception {
        bodyTransformer = mock(FieldTransformer.class);
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(TRANSFORMED_BODY);

        bylineTransformer = mock(FieldTransformer.class);
        when(bylineTransformer.transform(anyString(), anyString())).thenReturn(TRANSFORMED_BYLINE);

        htmlFieldProcessor = spy(new Html5SelfClosingTagBodyProcessor());

        contentSourceBrandMap = new HashMap<>();
        contentSourceBrandMap.put(ContentSource.FT, new Brand(FINANCIAL_TIMES_BRAND));
        contentSourceBrandMap.put(ContentSource.Reuters, new Brand(REUTERS_BRAND));

        standardEomFile = createStandardEomFile(uuid);
        standardExpectedContent = createStandardExpectedFtContent();

        eomFileProcessor = new EomFileProcessor(bodyTransformer, bylineTransformer, htmlFieldProcessor, contentSourceBrandMap, API_HOST);
    }

    @Test(expected = MethodeMarkedDeletedException.class)
    public void shouldThrowExceptionIfMarkedDeleted() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFile(uuid, TRUE))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, new Date());
    }

    @Test(expected = EmbargoDateInTheFutureException.class)
    public void shouldThrowExceptionIfEmbargoDateInTheFuture() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithEmbargoDateInTheFuture(uuid))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = UnsupportedObjectTypeException.class)
    public void shouldThrowUnsupportedObjectTypeExceptionIfObjectLocationEmpty() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithObjectLocation(uuid, ""))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, new Date());
    }

    @Test(expected = UnsupportedObjectTypeException.class)
    public void shouldThrowUnsupportedObjectTypeExceptionIfObjectTypeIsNotSupported() {
        String objectLocation = "/FT/Content/Companies/Stories/Live/Trump election victory business reaction WO 9.doc";
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithObjectLocation(uuid, objectLocation))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, new Date());
    }

    @Test(expected = NotWebChannelException.class)
    public void shouldThrowExceptionIfNoFtComChannel() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFile(uuid))
                .withSystemAttributes(buildEomFileSystemAttributes("NotFTcom"))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = SourceNotEligibleForPublishException.class)
    public void shouldThrowExceptionIfNotFtSource() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileNonFtOrAgencySource(uuid))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = WorkflowStatusNotEligibleForPublishException.class)
    public void shouldThrowExceptionIfWorkflowStatusNotEligibleForPublishing() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFile(uuid))
                .withWorkflowStatus("Stories/Edit")
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void shouldAllowEOMStoryWithNonEligibleWorkflowStatusBeforeEnforceDate() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createEomStoryFile(uuid, "FTContentMove/Ready", "FTcom", initialPublicationDateAsStringPreWfsEnforce))
                .build();

        String expectedBody = "<body id=\"some-random-value\"><foo/></body>";
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(expectedBody);

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withFirstPublishedDate(toDate(initialPublicationDateAsStringPreWfsEnforce, DATE_TIME_FORMAT))
                .withXmlBody(expectedBody).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bodyTransformer).transform(
                anyString(),
                eq(TRANSACTION_ID),
                eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
                eq(Maps.immutableEntry("apiHost", API_HOST)));
        assertThat(content, equalTo(expectedContent));
    }

    @Test(expected = WorkflowStatusNotEligibleForPublishException.class)
    public void shouldNotAllowEOMStoryWithNonEligibleWorkflowStatusAfterEnforceDate() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createEomStoryFile(uuid, "FTContentMove/Ready", "FTcom", initialPublicationDateAsString))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void shouldAllowEOMStoryWithFinancialTimesChannelAndNonEligibleWorkflowStatus() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createEomStoryFile(uuid, "FTContentMove/Ready", "Financial Times", initialPublicationDateAsString))
                .build();

        String expectedBody = "<body id=\"some-random-value\"><foo/></body>";
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(expectedBody);

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withXmlBody(expectedBody).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bodyTransformer).transform(
                anyString(),
                eq(TRANSACTION_ID),
                eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
                eq(Maps.immutableEntry("apiHost", API_HOST)));
        assertThat(content, equalTo(expectedContent));
    }

    @Test(expected = UnsupportedEomTypeException.class)
    public void shouldThrowUnsupportedTypeExceptionIfPublishingDwc() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createDwcComponentFile(uuid))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = MethodeMissingFieldException.class)
    public void shouldThrowExceptionIfNoLastPublicationDate() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithNoLastPublicationDate(uuid))
                .build();
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void shouldNotBarfOnExternalDtd() {
        Content content = eomFileProcessor.processPublication(standardEomFile, TRANSACTION_ID, LAST_MODIFIED);
        Content expectedContent = createStandardExpectedFtContent();
        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void shouldTransformBodyOnPublish() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .build();

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withXmlBody(TRANSFORMED_BODY).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bodyTransformer).transform(
                anyString(),
                eq(TRANSACTION_ID),
                eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
                eq(Maps.immutableEntry("apiHost", API_HOST)));
        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void shouldAllowBodyWithAttributes() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .build();

        String expectedBody = "<body id=\"some-random-value\"><foo/></body>";
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(expectedBody);

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withXmlBody(expectedBody).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bodyTransformer).transform(
                anyString(),
                eq(TRANSACTION_ID),
                eq(Maps.immutableEntry("uuid", eomFile.getUuid())),
                eq(Maps.immutableEntry("apiHost", API_HOST)));
        assertThat(content, equalTo(expectedContent));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfBodyTagIsMissingFromTransformedBody() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn("<p>some other random text</p>");
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void shouldThrowExceptionIfBodyIsNull() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(null);
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void shouldThrowExceptionIfBodyIsEmpty() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn("");
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void shouldThrowExceptionIfTransformedBodyIsBlank() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn("<body> \n \n \n </body>");
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void shouldThrowExceptionIfTransformedBodyIsEmpty() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(EMPTY_BODY);
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void thatPreviewEmptyTransformedBodyIsAllowed() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(EMPTY_BODY);
        Content actual = eomFileProcessor.processPreview(eomFile, TRANSACTION_ID, new Date());
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
    }

    @Test
    public void thatContentPackageNullBodyIsAllowed() {
        final EomFile eomFile = createEomFileWithRandomContentPackage();

        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(null);
        Content actual = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, new Date());
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
    }

    @Test
    public void thatContentPackageEmptyBodyIsAllowed() {
        final EomFile eomFile = createEomFileWithRandomContentPackage();

        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn("");
        Content actual = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, new Date());
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
    }

    @Test
    public void thatContentPackageBlankTransformedBodyIsAllowed() {
        final EomFile eomFile = createEomFileWithRandomContentPackage();

        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn("<body> \n \n \n </body>");
        Content actual = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, new Date());
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
    }

    private EomFile createEomFileWithRandomContentPackage() {
        return createStandardEomFileWithContentPackage(
                uuid,
                Boolean.TRUE,
                "cp",
                "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + UUID.randomUUID().toString() + "\"/>");
    }

    @Test
    public void shouldAddPublishReferenceToTransformedBody() {

        final String reference = "some unstructured reference";

        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .build();

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withPublishReference(reference)
                .withXmlBody(TRANSFORMED_BODY).build();

        Content content = eomFileProcessor.processPublication(eomFile, reference, LAST_MODIFIED);

        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void shouldTransformBylineWhenPresentOnPublish() {
        String byline = "By <author-name>Gillian Tett</author-name>";

        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put(TEMPLATE_PLACEHOLDER_BYLINE, byline);

        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .withValue(buildEomFileValue(templateValues))
                .build();

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
                .withByline(TRANSFORMED_BYLINE).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bylineTransformer).transform("<byline>" + byline + "</byline>", TRANSACTION_ID);
        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void shouldThrowMethodeContentNotEligibleForPublishExceptionWhenNotCompoundStoryOnPublish() {

        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .withType("EOM::SomethingElse")
                .build();

        expectedException.expect(MethodeContentNotEligibleForPublishException.class);
        expectedException.expect(hasProperty("message", equalTo("[EOM::SomethingElse] not an EOM::CompoundStory.")));

        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void testShouldAddMainImageIfPresent() throws Exception {
        final UUID imageUuid = UUID.randomUUID();
        final UUID expectedMainImageUuid = DeriveUUID.with(DeriveUUID.Salts.IMAGE_SET).from(imageUuid);
        final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid, "Primary size");

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getMainImage(), equalTo(expectedMainImageUuid.toString()));
    }

    @Test
    public void testMainImageIsNullIfMissing() throws Exception {
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getMainImage(), nullValue());
    }

    @Test
    public void testMainImageReferenceIsPutInBodyWhenPresentAndPrimarySizeFlag() throws Exception {
        String expectedTransformedBody = "<body><content data-embedded=\"true\" id=\"%s\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content>" +
                "                <p>random text for now</p>" +
                "            </body>";
        testMainImageReferenceIsPutInBodyWithMetadataFlag("Primary size",
                expectedTransformedBody);
    }

    @Test
    public void testMainImageReferenceIsPutInBodyWhenPresentAndArticleSizeFlag() throws Exception {
        String expectedTransformedBody = "<body><content data-embedded=\"true\" id=\"%s\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content>" +
                "                <p>random text for now</p>" +
                "            </body>";
        testMainImageReferenceIsPutInBodyWithMetadataFlag("Article size",
                expectedTransformedBody);
    }

    @Test
    public void testMainImageReferenceIsNotPutInBodyWhenPresentButNoPictureFlag() throws Exception {
        String expectedTransformedBody = "<body>" +
                "                <p>random text for now</p>" +
                "            </body>";
        testMainImageReferenceIsPutInBodyWithMetadataFlag("No picture", expectedTransformedBody);
    }

    @Test
    public void testMainImageReferenceIsNotPutInBodyWhenMissing() throws Exception {
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).then(returnsFirstArg());
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        String expectedBody = "<body>" +
                "                <p>random text for now</p>" +
                "            </body>";
        assertThat(content.getMainImage(), nullValue());
        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }

    @Test
    public void testStoryPackage() {
        final UUID storyPackageUuid = UUID.randomUUID();
        final EomFile eomFile = createStandardEomFileWithStoryPackage(uuid, storyPackageUuid.toString());
        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(content.getStoryPackage(), notNullValue());
        assertThat(content.getStoryPackage(), equalTo(storyPackageUuid.toString()));
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testStoryPackageWithInvalidUuid() {
        final String storyPackageUuid = "invalid-uuid";
        final EomFile eomFile = createStandardEomFileWithStoryPackage(uuid, storyPackageUuid);
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    private static EomFile createStandardEomFileWithStoryPackage(UUID uuid, String storyPackageUuid) {
        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put(TEMPLATE_PLACEHOLDER_STORY_PACKAGE_UUID, storyPackageUuid);

        return new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType(EOMCompoundStory.getTypeName())
                .withValue(buildEomFileValue(templateValues))
                .withAttributes(
                        new EomFileAttributesBuilder(ATTRIBUTES_TEMPLATE)
                                .withLastPublicationDate(lastPublicationDateAsString)
                                .withInitialPublicationDate(initialPublicationDateAsString)
                                .withMarkedDeleted(FALSE)
                                .withImageMetadata("No picture")
                                .withCommentsEnabled(FALSE)
                                .withEditorsPick("")
                                .withExclusive("")
                                .withScoop("")
                                .withEmbargoDate("")
                                .withSourceCode("FT")
                                .withContributorRights("")
                                .withObjectLocation(OBJECT_LOCATION)
                                .withSubscriptionLevel(Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel()))
                                .withContentPackageFlag(Boolean.FALSE)
                                .build())
                .withSystemAttributes(
                        buildEomFileSystemAttributes("FTcom"))
                .withWorkflowStatus(EomFile.WEB_READY)
                .build();
    }

    @Test
    public void testCommentsArePresent() {
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(content.getComments(), notNullValue());
        assertThat(content.getComments().isEnabled(), is(true));
    }

    @Test
    public void testStandoutFieldsArePresent() {
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getStandout().isEditorsChoice(), is(true));
        assertThat(content.getStandout().isExclusive(), is(true));
        assertThat(content.getStandout().isScoop(), is(true));
    }

    @Test(expected = MethodeMissingBodyException.class)
    public void thatTransformationFailsIfThereIsNoBody()
            throws Exception {

        String value = FileUtils.readFile("article/article_value_with_no_body.xml");
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .withValue(value.getBytes(UTF_8))
                .build();

        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void testArticleCanBeSyndicated() {
        final EomFile eomFile = createStandardEomFileWithContributorRights(
                uuid, ContributorRights.ALL_NO_PAYMENT.getValue());

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getCanBeSyndicated(), is(Syndication.YES));
    }

    @Test
    public void testArticleCanBeSyndicatedWithContributorPayment() {
        String[] contributorRights = {
                ContributorRights.FIFTY_FIFTY_NEW.getValue(),
                ContributorRights.FIFTY_FIFTY_OLD.getValue()
        };

        for (String contributorRight : contributorRights) {
            final EomFile eomFileContractContributorRights = createStandardEomFileWithContributorRights(
                    uuid, contributorRight);

            Content content = eomFileProcessor.processPublication(eomFileContractContributorRights, TRANSACTION_ID, LAST_MODIFIED);
            assertThat(content.getCanBeSyndicated(), is(Syndication.WITH_CONTRIBUTOR_PAYMENT));
        }
    }

    @Test
    public void testArticleCanNotBeSyndicated() {
        final EomFile eomFile = createStandardEomFileWithContributorRights(
                uuid, ContributorRights.NO_RIGHTS.getValue());

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getCanBeSyndicated(), is(Syndication.NO));
    }

    @Test
    public void testArticleNeedsToBeVerifiedForSyndication() {
        String[] contributorRights = {
                ContributorRights.CONTRACT.getValue(),
                "",
                "invalid value",
                "-1",
                "6"
        };

        for (String contributorRight : contributorRights) {
            final EomFile eomFileContractContributorRights = createStandardEomFileWithContributorRights(
                    uuid, contributorRight);

            Content content = eomFileProcessor.processPublication(eomFileContractContributorRights, TRANSACTION_ID, LAST_MODIFIED);
            assertThat("contributorRight=" + contributorRight, content.getCanBeSyndicated(), is(Syndication.VERIFY));
        }
    }

    @Test
    public void testArticleWithMissingContributorRights() {
        final EomFile eomFile = new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType(EOMCompoundStory.getTypeName())
                .withValue(buildEomFileValue(new HashMap<>()))
                .withAttributes(new EomFileAttributesBuilder(ATTRIBUTES_TEMPLATE_NO_CONTRIBUTOR_RIGHTS)
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
                        .withSubscriptionLevel(Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel()))
                        .build()
                )
                .withSystemAttributes(buildEomFileSystemAttributes("FTcom"))
                .withWorkflowStatus(EomFile.WEB_READY)
                .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getCanBeSyndicated(), is(Syndication.VERIFY));
    }

    @Test
    public void testArticleWithNoSubscriptionLevelHasNullAccessLevel(){
        final EomFile eomFile = createStandardEomFileWithSubscriptionLevel(uuid, "");
        expectedException.expect(UntransformableMethodeContentException.class);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertNull(content.getAccessLevel());
    }

    @Test
    public void testArticleWithUnknownSubscriptionLevelHasNullAccessLevel(){
        final EomFile eomFile = createStandardEomFileWithSubscriptionLevel(uuid, "5");
        expectedException.expect(UntransformableMethodeContentException.class);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertNull(content.getAccessLevel());
    }

    @Test
    public void testArticleHasSubscribedAccessLevel() {
        final EomFile eomFile = createStandardEomFileWithSubscriptionLevel(uuid, Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel()));
        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getAccessLevel(), is(AccessLevel.SUBSCRIBED));
    }

    @Test
    public void testArticleHasFreeAccessLevel() {
        final EomFile eomFile = createStandardEomFileWithSubscriptionLevel(uuid, Integer.toString(SHOWCASE.getSubscriptionLevel()));

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getAccessLevel(), is(AccessLevel.FREE));
    }

    @Test
    public void testArticleHasPremiumAccessLevel() {
        final EomFile eomFile = createStandardEomFileWithSubscriptionLevel(uuid, Integer.toString(PREMIUM.getSubscriptionLevel()));

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getAccessLevel(), is(AccessLevel.PREMIUM));
    }


    /**
     * Tests that a non-standard old type EOM::Story is also a valid type.
     * If the test fails exception will be thrown.
     */
    @Test
    public void thatStoryTypeIsAValidType() {
        final EomFile eomFile = createEomStoryFile(uuid);
        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content, notNullValue());
    }

    @Test
    public void thatStandfirstIsPresent() {
        final String expectedStandfirst = "Test standfirst";

        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put(TEMPLATE_PLACEHOLDER_STANDFIRST, expectedStandfirst);

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(templateValues))
          .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getStandfirst(), is(equalToIgnoringWhiteSpace(expectedStandfirst)));
    }

    @Test
    public void thatWhitespaceStandfirstIsTreatedAsAbsent() {
        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put(TEMPLATE_PLACEHOLDER_STANDFIRST, "\n");

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(templateValues))
          .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getStandfirst(), is(nullValue()));
    }

    @Test
    public void thatStandfirstIsOptional() {
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getStandfirst(), is(nullValue()));
    }

    @Test
    public void thatAlternativeTitlesArePresent() {
        final String promoTitle = "Test Promo Title";
        final String contentPackageTitle = "Test Content Package Title";

        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put(TEMPLATE_PLACEHOLDER_PROMO_TITLE, promoTitle);
        templateValues.put(TEMPLATE_CONTENT_PACKAGE_TITLE, contentPackageTitle);

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(templateValues))
          .build();

        final Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content, is(notNullValue()));

        final AlternativeTitles actual = content.getAlternativeTitles();
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getPromotionalTitle(), is(equalToIgnoringWhiteSpace(promoTitle)));
        assertThat(actual.getContentPackageTitle(), is(equalToIgnoringWhiteSpace(contentPackageTitle)));
    }

    @Test
    public void thatAlternativeTitlesAreOptional() {
        final EomFile eomFile = createStandardEomFile(uuid);

        final Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
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

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(templateValues))
          .build();

        final Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
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
        templateValues.put(TEMPLATE_CONTENT_PACKAGE_TITLE, "<?EM-dummyText content package title here... ?>");

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(templateValues))
          .build();

        final Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content, is(notNullValue()));

        final AlternativeTitles actual = content.getAlternativeTitles();
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getPromotionalTitle(), is(nullValue()));
        assertThat(actual.getContentPackageTitle(), is(nullValue()));
    }

    @Test
    public void thatWebUrlIsPresent() {
        URI webUrl = URI.create("http://www.ft.com/a-fancy-url");
        final EomFile eomFile = createStandardEomFileWithWebUrl(uuid, webUrl);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getWebUrl(), is(equalTo(webUrl)));
    }

    @Test
    public void testContentPackageWithFormattedDescAndHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"/>";

        testContentPackage(description, listHref, description, listId);
    }

    @Test
    public void testContentPackageWithNonFormattedDescAndDummyTextInLink() throws Exception {
        final String description = "Description";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"><?EM-dummyText ...?>\\r\\n</a>";

        testContentPackage(description, listHref, description, listId);
    }

    @Test
    public void testContentPackageWithEmptyDescription() throws Exception {
        final String description = "";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"/>";

        testContentPackage(description, listHref, null, listId);
    }

    @Test
    public void testContentPackageWithDummyDescription() throws Exception {
        final String description = "<?EM-dummyText ...?>";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"/>";

        testContentPackage(description, listHref, null, listId);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageWithEmptyHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listHref = "";

        testContentPackage(description, listHref, null, null);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageWithNoUuidInHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc\"/>";

        testContentPackage(description, listHref, null, null);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageWithInvalidUuidInHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=123\"/>";

        testContentPackage(description, listHref, null, null);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageAttributeSetButNoValues() throws Exception {
        testContentPackage(null, null, null, null);
    }

    @Test
    public void testAgencyContentProcessPublication() {
        final EomFile eomFile = createStandardEomFileAgencySource(uuid);
        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        final Content expectedContent = createStandardExpectedAgencyContent();

        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void testAgencyContentProcessPreview() {
        final EomFile eomFile = createStandardEomFileAgencySource(uuid);
        Content content = eomFileProcessor.processPreview(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        final Content expectedContent = createStandardExpectedAgencyContent();

        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void testTypeArticleIsDefaultSetIfNoContentPackage() {
        final EomFile eomFile = createStandardEomFile(uuid);
        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(EomFileProcessor.Type.ARTICLE, equalTo(content.getType()));
    }

    @Test
    public void testImageSet() {
        String expectedUUID = UUID.nameUUIDFromBytes(IMAGE_SET_UUID.getBytes(UTF_8)).toString();
        String expectedBody = "<body>"
            + "<p>random text for now</p>"
            + "<ft-content type=\"http://www.ft.com/ontology/content/ImageSet\" url=\"http://api.ft.com/content/" + expectedUUID + "\" data-embedded=\"true\"></ft-content>"
            + "</body>";
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(expectedBody);

        EomFile eomFile = createStandardEomFileWithImageSet(IMAGE_SET_UUID);
        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }

    @Test
    public void testImageSetPreview() {
        String expectedUUID = UUID.nameUUIDFromBytes(IMAGE_SET_UUID.getBytes(UTF_8)).toString();
        String expectedBody = "<body>"
            + "<p>random text for now</p>"
            + "<ft-content type=\"http://www.ft.com/ontology/content/ImageSet\" url=\"http://api.ft.com/content/" + expectedUUID + "\" data-embedded=\"true\"></ft-content>"
            + "</body>";
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).thenReturn(expectedBody);

        EomFile eomFile = createStandardEomFileWithImageSet(IMAGE_SET_UUID);
        Content content = eomFileProcessor.processPreview(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }

    @Test
    public void thatAlternativeStandfirtstIsPresent() {
        final String expectedPromotionalStandfirst = "Test promotional Standfirst";

        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put(TEMPLATE_WEB_SUBHEAD, expectedPromotionalStandfirst);

        final EomFile eomFile = (new EomFile.Builder())
                .withValuesFrom(createStandardEomFile(uuid))
                .withValue(buildEomFileValue(templateValues))
                .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getAlternativeStandfirsts().getPromotionalStandfirst(), is(equalToIgnoringWhiteSpace(expectedPromotionalStandfirst)));
    }

    @Test
    public void thatAlternativeStandfirtstIsNotPresent() {
        final EomFile eomFile = (new EomFile.Builder())
                .withValuesFrom(createStandardEomFile(uuid))
                .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getAlternativeStandfirsts().getPromotionalStandfirst(), is(nullValue()));
    }

    private void testContentPackage(final String description,
                                    final String listHref,
                                    final String expectedDescription,
                                    final String expectedListId) {
        final EomFile eomFile = createStandardEomFileWithContentPackage(UUID.randomUUID(), true, description, listHref);
        final Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(content.getType(), is(EomFileProcessor.Type.CONTENT_PACKAGE));
        assertThat(content.getDescription(), is(expectedDescription));
        assertThat(content.getContentPackage(), is(expectedListId));
        assertThat(content.getCanBeDistributed(), is(Distribution.VERIFY));
    }

    private void testMainImageReferenceIsPutInBodyWithMetadataFlag(String articleImageMetadataFlag, String expectedTransformedBody) {
        when(bodyTransformer.transform(anyString(), anyString(), anyVararg())).then(returnsFirstArg());
        final UUID imageUuid = UUID.randomUUID();
        final UUID expectedMainImageUuid = DeriveUUID.with(DeriveUUID.Salts.IMAGE_SET).from(imageUuid);
        final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid,
            articleImageMetadataFlag);
        Content content = eomFileProcessor
            .processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        String expectedBody = String.format(expectedTransformedBody, expectedMainImageUuid);
        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }

    /**
     * Creates EomFile with an non-standard type EOM::Story as opposed to EOM::CompoundStory which
     * is standard.
     *
     * @param uuid uuid of an article
     * @return EomFile
     */
    private EomFile createEomStoryFile(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFile(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileWithObjectLocation(UUID uuid, String objectLocation) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, "", objectLocation, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createEomStoryFile(UUID uuid, String workflowStatus, String channel, String initialPublicationDate) {
        return createStandardEomFile(uuid, FALSE, false, channel, "FT", workflowStatus, lastPublicationDateAsString,
                initialPublicationDate, TRUE, "Yes", "Yes", "Yes", EOMStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileNonFtOrAgencySource(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "Pepsi", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted) {
        return createStandardEomFile(uuid, markedDeleted, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileAgencySource(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "REU2", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileWithEmbargoDateInTheFuture(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, true, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileWithNoLastPublicationDate(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, "", initialPublicationDateAsString,
                FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileWithWebUrl(UUID uuid, URI webUrl) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), webUrl, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileWithContributorRights(UUID uuid, String contributorRights) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, contributorRights, OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                null, null, null, null);
    }

    private EomFile createStandardEomFileWithSubscriptionLevel(UUID uuid, String subscriptionLevel) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, subscriptionLevel,
                null, null, null, null);
    }

    private EomFile createStandardEomFileWithContentPackage(UUID uuid, Boolean hasContentPackage, String contentPackageDesc, String contentPackageHref) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                hasContentPackage, contentPackageDesc, contentPackageHref, null);
    }

    private EomFile createStandardEomFileWithImageSet(String imageSetID) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
            initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
            null, null, null, imageSetID);
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted, boolean embargoDateInTheFuture,
                                          String channel, String sourceCode, String workflowStatus,
                                          String lastPublicationDateAsString, String initialPublicationDateAsString,
                                          String commentsEnabled, String editorsPick, String exclusive, String scoop,
                                          String eomType, URI webUrl, String contributorRights, String objectLocation, String subscriptionLevel,
                                          Boolean hasContentPackage, String contentPackageDesc, String contentPackageListHref,
                                          String imageSetID) {

        String embargoDate = "";
        if (embargoDateInTheFuture) {
            embargoDate = dateInTheFutureAsStringInMethodeFormat();
        }

        Map<String, Object> templateValues = new HashMap<>();
        if (contentPackageDesc != null && contentPackageListHref != null) {
            templateValues.put(TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE, Boolean.TRUE);
            templateValues.put(TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_DESC, contentPackageDesc);
            templateValues.put(TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_LIST_HREF, contentPackageListHref);
        }

        templateValues.put(TEMPLATE_PLACEHOLDER_IMAGE_SET_UUID, imageSetID);

        return new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType(eomType)
                .withValue(buildEomFileValue(templateValues))
                .withAttributes(new EomFileAttributesBuilder(ATTRIBUTES_TEMPLATE)
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
                        .withContentPackageFlag(hasContentPackage)
                        .build()
                )
                .withSystemAttributes(buildEomFileSystemAttributes(channel))
                .withWorkflowStatus(workflowStatus)
                .withWebUrl(webUrl)
                .build();
    }

    private EomFile createDwcComponentFile(UUID uuid) {

        return new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType("EOM::WebContainer")
                .withValue(("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<!-- $Id: editorsChoice.dwc,v 1.1 2005/08/05 15:19:37 alant Exp $ -->\n" +
                        "<!--\n" +
                        "    Note: The dwc should simply define the dwcContent entity and reference it\n" +
                        "-->\n" +
                        "<!DOCTYPE dwc SYSTEM \"/FTcom Production/ZZZ_Templates/DWC/common/ref/dwc.dtd\" [\n" +
                        "\n" +
                        "    <!ENTITY dwcContent SYSTEM \"/FTcom Production/ZZZ_Templates/DWC/editorsChoice/ref/editorsChoice_dwc.xml\">\n" +
                        "    <!ENTITY % entities SYSTEM \"/FTcom Production/ZZZ_Templates/DWC/common/ref/entities.xml\">\n" +
                        "    %entities;\n" +
                        "]>\n" +
                        "<dwc>\n" +
                        "&dwcContent;\n" +
                        "</dwc>\n").getBytes(UTF_8))
                .withAttributes("<!DOCTYPE ObjectMetadata SYSTEM \"/SysConfig/Classify/FTDWC2/classify.dtd\">\n" +
                        "<ObjectMetadata><FTcom><DIFTcomWebType>editorsChoice_2</DIFTcomWebType>\n" +
                        "<autoFill/>\n" +
                        "<footwellDedupe/>\n" +
                        "<displayCode/>\n" +
                        "<searchAge>1</searchAge>\n" +
                        "<agingRule>1</agingRule>\n" +
                        "<markDeleted>False</markDeleted>\n" +
                        "</FTcom>\n" +
                        "</ObjectMetadata>")
                .withSystemAttributes("<props><productInfo><name>FTcom</name></productInfo></props>")
                .withWorkflowStatus("") // This is what DWCs get.
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

    private Content createStandardExpectedContent(ContentSource contentSource){
        return Content.builder()
                .withTitle(EXPECTED_TITLE)
                .withType(EomFileProcessor.Type.ARTICLE)
                .withXmlBody("<body><p>some other random text</p></body>")
                .withByline("")
                .withBrands(new TreeSet<>(Collections.singletonList(contentSourceBrandMap.get(contentSource))))
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
                .withCanBeDistributed(contentSource == ContentSource.FT
                        ? Distribution.YES
                        : contentSource == ContentSource.Reuters ? Distribution.NO : Distribution.VERIFY)
                .withAlternativeStandfirsts(new AlternativeStandfirsts(null))
                .build();
    }
}
