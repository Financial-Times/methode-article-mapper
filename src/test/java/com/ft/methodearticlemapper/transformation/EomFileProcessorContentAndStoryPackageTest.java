package com.ft.methodearticlemapper.transformation;

import com.ft.bodyprocessing.BodyProcessor;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.common.FileUtils;
import com.ft.content.model.Brand;
import com.ft.content.model.Content;
import com.ft.content.model.Distribution;
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.model.EomFile;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static com.ft.methodearticlemapper.methode.EomFileType.EOMCompoundStory;
import static com.ft.methodearticlemapper.methode.EomFileType.EOMStory;
import static com.ft.methodearticlemapper.transformation.SubscriptionLevel.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class EomFileProcessorContentAndStoryPackageTest {
    public static final String FINANCIAL_TIMES_BRAND = "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54";
    public static final String REUTERS_BRAND = "http://api.ft.com/things/ed3b6ec5-6466-47ef-b1d8-16952fd522c7";

    private static final String ARTICLE_TEMPLATE = FileUtils.readFile("article/article_value.xml.mustache");
    private static final String ATTRIBUTES_TEMPLATE = FileUtils.readFile("article/article_attributes.xml.mustache");
    private static final String SYSTEM_ATTRIBUTES_TEMPLATE = FileUtils.readFile("article/article_system_attributes.xml.mustache");

    private static final String TRANSACTION_ID = "tid_test";
    private static final String FALSE = "False";
    private static final String TRUE = "True";
    private static final String PUBLISH_REF = "publishReference";
    private static final String API_HOST = "test.api.ft.com";
    private static final String WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";
    private static final String CANONICAL_WEB_URL_TEMPLATE = "https://www.ft.com/content/%s";

    private static final String lastPublicationDateAsString = "20130813145815";
    private static final String initialPublicationDateAsString = "20120813145815";

    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
    private static final String OBJECT_LOCATION = "/FT/Content/Companies/Stories/Live/Trump election victory business reaction WO 9.xml";

    private static final String WORK_FOLDER_COMPANIES = "/FT/Companies";
    private static final String SUB_FOLDER_RETAIL = "Retail &amp; Consumer";

    private static final String INTERNAL_ANALYTICS_TAGS = "sometag, another_tag, tag with spaces, mixedCaseTag, House &amp; Home, Less &lt; More, Life+Arts";


    private static final String TRANSFORMED_BODY = "<body><p>some other random text</p></body>";
    private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
    private static final String EMPTY_BODY = "<body></body>";
    private static final Date LAST_MODIFIED = new Date();
    private static final String SUBSCRIPTION_LEVEL = Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel());

    private static final String TEMPLATE_PLACEHOLDER_MAINIMAGE = "mainImageUuid";
    private static final String TEMPLATE_PLACEHOLDER_STORY_PACKAGE_UUID = "storyPackageUuid";
    private static final String TEMPLATE_PLACEHOLDER_IMAGE_SET_UUID = "imageSetID";
    private static final String TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE = "contentPackage";
    private static final String TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_DESC = "contentPackageDesc";
    private static final String TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_LIST_HREF = "contentPackageListHref";

    private final UUID uuid = UUID.randomUUID();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private FieldTransformer bodyTransformer;
    private FieldTransformer bylineTransformer;
    private BodyProcessor htmlFieldProcessor;

    private Map<ContentSource, Brand> contentSourceBrandMap;

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

    private static String buildEomFileSystemAttributes(String channel, String workFolder, String subFolder) {
        Template mustache = Mustache.compiler().compile(SYSTEM_ATTRIBUTES_TEMPLATE);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("channel", channel);
        attributes.put("workFolder", workFolder);
        attributes.put("subFolder", subFolder);
        return mustache.execute(attributes);
    }

    @Before
    public void setUp() {
        bodyTransformer = mock(FieldTransformer.class);
        when(bodyTransformer.transform(anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg())).thenReturn(TRANSFORMED_BODY);

        bylineTransformer = mock(FieldTransformer.class);
        when(bylineTransformer.transform(anyString(), anyString(), eq(TransformationMode.PUBLISH))).thenReturn(TRANSFORMED_BYLINE);

        htmlFieldProcessor = spy(new Html5SelfClosingTagBodyProcessor());

        contentSourceBrandMap = new HashMap<>();
        contentSourceBrandMap.put(ContentSource.FT, new Brand(FINANCIAL_TIMES_BRAND));
        contentSourceBrandMap.put(ContentSource.Reuters, new Brand(REUTERS_BRAND));

        eomFileProcessor = new EomFileProcessor(EnumSet.allOf(TransformationMode.class), bodyTransformer,
                bylineTransformer, htmlFieldProcessor, contentSourceBrandMap, PUBLISH_REF, API_HOST,
                WEB_URL_TEMPLATE, CANONICAL_WEB_URL_TEMPLATE);
    }

    @Test
    public void thatContentPackageNullBodyIsAllowed() {
        final EomFile eomFile = createEomFileWithRandomContentPackage();

        when(bodyTransformer.transform(anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg())).thenReturn(null);
        Content actual = eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, new Date());
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
    }

    @Test
    public void thatContentPackageEmptyBodyIsAllowed() {
        final EomFile eomFile = createEomFileWithRandomContentPackage();

        when(bodyTransformer.transform(anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg())).thenReturn("");
        Content actual = eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, new Date());
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
    }

    @Test
    public void thatContentPackageBlankTransformedBodyIsAllowed() {
        final EomFile eomFile = createEomFileWithRandomContentPackage();

        when(bodyTransformer.transform(anyString(), anyString(), eq(TransformationMode.PUBLISH), anyVararg())).thenReturn("<body> \n \n \n </body>");
        Content actual = eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, new Date());
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
    }

    @Test
    public void testContentPackageWithFormattedDescAndHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"/>";

        testContentPackage(description, listHref, description, listId, TransformationMode.PUBLISH);
    }

    @Test
    public void testContentPackageWithNonFormattedDescAndDummyTextInLink() throws Exception {
        final String description = "Description";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"><?EM-dummyText ...?>\\r\\n</a>";

        testContentPackage(description, listHref, description, listId, TransformationMode.PUBLISH);
    }

    @Test
    public void testContentPackageWithEmptyDescription() throws Exception {
        final String description = "";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"/>";

        testContentPackage(description, listHref, null, listId, TransformationMode.PUBLISH);
    }

    @Test
    public void testContentPackageWithDummyDescription() throws Exception {
        final String description = "<?EM-dummyText ...?>";
        final String listId = UUID.randomUUID().toString();
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + listId + "\"/>";

        testContentPackage(description, listHref, null, listId, TransformationMode.PUBLISH);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageWithEmptyHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listHref = "";

        testContentPackage(description, listHref, null, null, TransformationMode.PUBLISH);
    }

    @Test
    public void testContentPackageWithEmptyHrefIsAllowedForSuggest() throws Exception {
        final String description = "<p>Description</p>";
        final String listHref = "";

        testContentPackage(description, listHref, description, null, TransformationMode.SUGGEST);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageWithNoUuidInHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc\"/>";

        testContentPackage(description, listHref, null, null, TransformationMode.PUBLISH);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageWithInvalidUuidInHref() throws Exception {
        final String description = "<p>Description</p>";
        final String listHref = "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=123\"/>";

        testContentPackage(description, listHref, null, null, TransformationMode.PUBLISH);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testContentPackageAttributeSetButNoValues() throws Exception {
        testContentPackage(null, null, null, null, TransformationMode.PUBLISH);
    }

    private void testContentPackage(final String description,
                                    final String listHref,
                                    final String expectedDescription,
                                    final String expectedListId,
                                    final TransformationMode mode) {
        final EomFile eomFile = createStandardEomFileWithContentPackage(UUID.randomUUID(), true, description, listHref);
        final Content content = eomFileProcessor.process(eomFile, mode, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(content.getType(), is(EomFileProcessor.Type.CONTENT_PACKAGE));
        assertThat(content.getDescription(), is(expectedDescription));
        assertThat(content.getContentPackage(), is(expectedListId));
        assertThat(content.getCanBeDistributed(), is(Distribution.VERIFY));
    }

    private EomFile createEomFileWithRandomContentPackage() {
        return createStandardEomFileWithContentPackage(
                uuid,
                Boolean.TRUE,
                "cp",
                "<a href=\"/FT/Content/Content%20Package/Live/content-package-test.dwc?uuid=" + UUID.randomUUID().toString() + "\"/>");
    }

    @Test
    public void testStoryPackage() {
        final UUID storyPackageUuid = UUID.randomUUID();
        final EomFile eomFile = createStandardEomFileWithStoryPackage(uuid, storyPackageUuid.toString());
        Content content = eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);

        assertThat(content.getStoryPackage(), notNullValue());
        assertThat(content.getStoryPackage(), equalTo(storyPackageUuid.toString()));
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testStoryPackageWithEmptyUuid() {
        final String storyPackageUuid = "";
        final EomFile eomFile = createStandardEomFileWithStoryPackage(uuid, storyPackageUuid);
        eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void testStoryPackageWithEmptyUuidIsAllowedForSuggest() {
        final String storyPackageUuid = "";
        final EomFile eomFile = createStandardEomFileWithStoryPackage(uuid, storyPackageUuid);
        Content content = eomFileProcessor.process(eomFile, TransformationMode.SUGGEST, TRANSACTION_ID, LAST_MODIFIED);
        
        assertThat(content.getStoryPackage(), nullValue());
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void testStoryPackageWithInvalidUuid() {
        final String storyPackageUuid = "123";
        final EomFile eomFile = createStandardEomFileWithStoryPackage(uuid, storyPackageUuid);
        eomFileProcessor.process(eomFile, TransformationMode.PUBLISH, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void testStoryPackageWithInvalidUuidIsAllowedForSuggest() {
        final String storyPackageUuid = "123";
        final EomFile eomFile = createStandardEomFileWithStoryPackage(uuid, storyPackageUuid);
        Content content = eomFileProcessor.process(eomFile, TransformationMode.SUGGEST, TRANSACTION_ID, LAST_MODIFIED);
        
        assertThat(content.getStoryPackage(), nullValue());
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
                                .withInternalAnalyticsTags(INTERNAL_ANALYTICS_TAGS)
                                .build())
                .withSystemAttributes(
                        buildEomFileSystemAttributes("FTcom", WORK_FOLDER_COMPANIES, SUB_FOLDER_RETAIL))
                .withWorkflowStatus(EomFile.WEB_READY)
                .build();
    }

    private EomFile createStandardEomFileWithContentPackage(UUID uuid, Boolean hasContentPackage, String contentPackageDesc, String contentPackageHref) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", WORK_FOLDER_COMPANIES, SUB_FOLDER_RETAIL, "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMStory.getTypeName(), "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL,
                hasContentPackage, contentPackageDesc, contentPackageHref, null, INTERNAL_ANALYTICS_TAGS);
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted, boolean embargoDateInTheFuture,
                                          String channel, String workFolder, String subFolder, String sourceCode, String workflowStatus,
                                          String lastPublicationDateAsString, String initialPublicationDateAsString,
                                          String commentsEnabled, String editorsPick, String exclusive, String scoop,
                                          String eomType, String contributorRights,
                                          String objectLocation, String subscriptionLevel,
                                          Boolean hasContentPackage, String contentPackageDesc, String contentPackageListHref,
                                          String imageSetID, String internalAnalyticsTags) {

        String embargoDate = "";
        if (embargoDateInTheFuture) {
            embargoDate = dateInTheFutureAsStringInMethodeFormat();
        }

        Map<String, Object> templateValues = new HashMap<>();
        if (hasContentPackage) {
            templateValues.put(TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE, Boolean.TRUE);
            templateValues.put(TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_DESC, StringUtils.stripToEmpty(contentPackageDesc));
            templateValues.put(TEMPLATE_PLACEHOLDER_CONTENT_PACKAGE_LIST_HREF, StringUtils.stripToEmpty(contentPackageListHref));
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
                        .withInternalAnalyticsTags(internalAnalyticsTags)
                        .build()
                )
                .withSystemAttributes(buildEomFileSystemAttributes(channel, workFolder, subFolder))
                .withWorkflowStatus(workflowStatus)
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
}
