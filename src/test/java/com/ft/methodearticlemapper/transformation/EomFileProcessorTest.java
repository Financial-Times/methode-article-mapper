package com.ft.methodearticlemapper.transformation;

import com.ft.content.model.AccessLevel;
import com.ft.content.model.AlternativeTitles;
import com.ft.content.model.Brand;
import com.ft.content.model.Comments;
import com.ft.content.model.Content;
import com.ft.content.model.Identifier;
import com.ft.content.model.Standout;
import com.ft.content.model.Syndication;
import com.google.common.collect.ImmutableSortedSet;

import com.ft.common.FileUtils;
import com.ft.methodearticlemapper.exception.EmbargoDateInTheFutureException;
import com.ft.methodearticlemapper.exception.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.SourceNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.UnsupportedObjectTypeException;
import com.ft.methodearticlemapper.exception.UnsupportedEomTypeException;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.exception.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.util.ImageSetUuidGenerator;
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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class EomFileProcessorTest {
    public static final String FINANCIAL_TIMES_BRAND = "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54";
    
    private static final String ARTICLE_TEMPLATE = FileUtils.readFile("article/article_value.xml.mustache");
    private static final String ATTRIBUTES_TEMPLATE = FileUtils.readFile("article/article_attributes.xml.mustache");
    private static final String ATTRIBUTES_TEMPLATE_NO_CONTRIBUTOR_RIGHTS = FileUtils.readFile("article/article_attributes_no_contributor_rights.xml.mustache");
    private static final String SYSTEM_ATTRIBUTES_TEMPLATE = FileUtils.readFile("article/article_system_attributes.xml.mustache");

    private static final String TRANSACTION_ID = "tid_test";
    private static final String FALSE = "False";
    private static final String TRUE = "True";

    private static final String lastPublicationDateAsString = "20130813145815";
    private static final String initialPublicationDateAsString = "20120813145815";
    private static final String initialPublicationDateAsStringPreWfsEnforce = "20110513145815";

    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
    private static final String EXPECTED_TITLE = "And sacked chimney-sweep pumps boss full of mayonnaise.";
    private static final String OBJECT_LOCATION="/FT/Content/Companies/Stories/Live/Trump election victory business reaction WO 9.xml";
    
    private static final String TRANSFORMED_BODY = "<body><p>some other random text</p></body>";
    private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
    private static final String EMPTY_BODY = "<body></body>";
    private static final Date LAST_MODIFIED = new Date();
    private static final String SUBSCRIPTION_LEVEL = Integer.toString(FOLLOW_USUAL_RULES.getSubscriptionLevel());

    private final UUID uuid = UUID.randomUUID();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private FieldTransformer bodyTransformer;
    private FieldTransformer bylineTransformer;
    private Brand financialTimesBrand;
    private EomFile standardEomFile;
    private Content standardExpectedContent;

    private EomFileProcessor eomFileProcessor;

    public static EomFile createStandardEomFileWithMainImage(UUID uuid, UUID mainImageUuid, String articleImageMetadataFlag) {
        return new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType(EOMCompoundStory.getTypeName())
                .withValue(
                        buildEomFileValue(mainImageUuid, null, null, null))
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

    private static byte[] buildEomFileValue(
            UUID mainImageUuid,
            String promoTitle,
            String standfirst,
            String byline) {

        Template mustache = Mustache.compiler().escapeHTML(false).compile(ARTICLE_TEMPLATE);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("mainImageUuid", mainImageUuid);
        attributes.put("promoTitle", promoTitle);
        attributes.put("standfirst", standfirst);
        attributes.put("byline", byline);

        return mustache.execute(attributes).getBytes(UTF_8);
    }

    private static String buildEomFileSystemAttributes(
            String channel) {

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
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn(TRANSFORMED_BODY);

        bylineTransformer = mock(FieldTransformer.class);
        when(bylineTransformer.transform(anyString(), anyString())).thenReturn(TRANSFORMED_BYLINE);

        financialTimesBrand = new Brand(FINANCIAL_TIMES_BRAND);

        standardEomFile = createStandardEomFile(uuid);
        standardExpectedContent = createStandardExpectedContent();

        eomFileProcessor = new EomFileProcessor(bodyTransformer, bylineTransformer, financialTimesBrand);
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
                .withValuesFrom(createStandardEomFileNonFtSource(uuid))
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
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn(expectedBody);

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withFirstPublishedDate(toDate(initialPublicationDateAsStringPreWfsEnforce, DATE_TIME_FORMAT))
                .withXmlBody(expectedBody).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bodyTransformer, times(1)).transform(isA(String.class), isA(String.class));
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
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn(expectedBody);

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withXmlBody(expectedBody).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bodyTransformer, times(1)).transform(isA(String.class), isA(String.class));
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
        Content expectedContent = createStandardExpectedContent();
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

        verify(bodyTransformer, times(1)).transform(isA(String.class), isA(String.class));
        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void shouldAllowBodyWithAttributes() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .build();

        String expectedBody = "<body id=\"some-random-value\"><foo/></body>";
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn(expectedBody);

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withXmlBody(expectedBody).build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        verify(bodyTransformer, times(1)).transform(isA(String.class), isA(String.class));
        assertThat(content, equalTo(expectedContent));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfBodyTagIsMissingFromTransformedBody() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn("<p>some other random text</p>");
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void shouldThrowExceptionIfTransformedBodyIsBlank() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn("<body> \n \n \n </body>");
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test(expected = UntransformableMethodeContentException.class)
    public void shouldThrowExceptionIfTransformedBodyIsEmpty() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn(EMPTY_BODY);
        eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
    }

    @Test
    public void thatPreviewEmptyTransformedBodyIsAllowed() {
        final EomFile eomFile = createEomStoryFile(uuid);
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn(EMPTY_BODY);
        Content actual = eomFileProcessor.processPreview(eomFile, TRANSACTION_ID);
        assertThat(actual.getBody(), is(equalTo(EMPTY_BODY)));
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

        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .withValue(buildEomFileValue(null, null, null, byline))
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
        final UUID expectedMainImageUuid = ImageSetUuidGenerator.fromImageUuid(imageUuid);
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
        when(bodyTransformer.transform(anyString(), anyString())).then(returnsFirstArg());
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        String expectedBody = "<body>" +
                "                <p>random text for now</p>" +
                "            </body>";
        assertThat(content.getMainImage(), nullValue());
        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
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
                ContributorRights.FIFTY_FIFTY_NEW.getValue(),
                ContributorRights.FIFTY_FIFTY_OLD.getValue(),
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
                .withValue(buildEomFileValue(null, null, null, null))
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
        assertThat(content,notNullValue());
    }

    @Test
    public void thatStandfirstIsPresent() {
      final String expectedStandfirst = "Test standfirst";

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(null, null, expectedStandfirst, null))
          .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
      assertThat(content.getStandfirst(), is(equalToIgnoringWhiteSpace(expectedStandfirst)));
    }

    @Test
    public void thatWhitespaceStandfirstIsTreatedAsAbsent() {
      final String standfirst = "\n";

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(null, null, standfirst, null))
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
      String promoTitle = "Test Promo Title";

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(null, promoTitle, null, null))
          .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
      assertThat(content.getAlternativeTitles().getPromotionalTitle(), is(equalToIgnoringWhiteSpace(promoTitle)));
    }

    @Test
    public void thatAlternativeTitlesAreOptional() {
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        AlternativeTitles actual = content.getAlternativeTitles();
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getPromotionalTitle(), is(nullValue()));
    }

    @Test
    public void thatWhitespacePromoTitleIsTreatedAsAbsent() {
      String promoTitle = "\n";

        final EomFile eomFile = (new EomFile.Builder())
          .withValuesFrom(createStandardEomFile(uuid))
          .withValue(buildEomFileValue(null, promoTitle, null, null))
          .build();

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
      assertThat(content.getAlternativeTitles().getPromotionalTitle(), is(nullValue()));
    }

    @Test
    public void thatWebUrlIsPresent() {
        URI webUrl = URI.create("http://www.ft.com/a-fancy-url");
        final EomFile eomFile = createStandardEomFileWithWebUrl(uuid, webUrl);

        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);
        assertThat(content.getWebUrl(), is(equalTo(webUrl)));
    }
   
    private void testMainImageReferenceIsPutInBodyWithMetadataFlag(String articleImageMetadataFlag, String expectedTransformedBody) {
        when(bodyTransformer.transform(anyString(), anyString())).then(returnsFirstArg());
        final UUID imageUuid = UUID.randomUUID();
        final UUID expectedMainImageUuid = ImageSetUuidGenerator.fromImageUuid(imageUuid);
        final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid, articleImageMetadataFlag);
        Content content = eomFileProcessor.processPublication(eomFile, TRANSACTION_ID, LAST_MODIFIED);

        String expectedBody = String.format(expectedTransformedBody, expectedMainImageUuid);
        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }

    /**
     * Creates EomFile with an non-standard type EOM::Story as opposed to EOM::CompoundStory which is standard.
     *
     * @param uuid uuid of an article
     * @return EomFile
     */
    private EomFile createEomStoryFile(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFile(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFileWithObjectLocation(UUID uuid, String objectLocation) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, "", objectLocation, SUBSCRIPTION_LEVEL);
    }

    private EomFile createEomStoryFile(UUID uuid, String workflowStatus, String channel, String initialPublicationDate) {
        return createStandardEomFile(uuid, FALSE, false, channel, "FT", workflowStatus, lastPublicationDateAsString,
                initialPublicationDate, TRUE, "Yes", "Yes", "Yes", EOMStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFileNonFtSource(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "Pepsi", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted) {
        return createStandardEomFile(uuid, markedDeleted, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFileWithEmbargoDateInTheFuture(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, true, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFileWithNoLastPublicationDate(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, "", initialPublicationDateAsString,
                FALSE, "", "", "", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFileWithWebUrl(UUID uuid, URI webUrl) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, FALSE, "", "", "", EOMCompoundStory.getTypeName(), webUrl, "", OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFileWithContributorRights(UUID uuid, String contributorRights) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, contributorRights, OBJECT_LOCATION, SUBSCRIPTION_LEVEL);
    }

    private EomFile createStandardEomFileWithSubscriptionLevel(UUID uuid, String subscriptionLevel) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString,
                initialPublicationDateAsString, TRUE, "Yes", "Yes", "Yes", EOMCompoundStory.getTypeName(), null, "", OBJECT_LOCATION, subscriptionLevel);
    }
    
    private EomFile createStandardEomFile(UUID uuid, String markedDeleted, boolean embargoDateInTheFuture,
                                          String channel, String sourceCode, String workflowStatus,
                                          String lastPublicationDateAsString, String initialPublicationDateAsString,
                                          String commentsEnabled, String editorsPick, String exclusive, String scoop,
                                          String eomType, URI webUrl, String contributorRights, String objectLocation, String subscriptionLevel) {

        String embargoDate = "";
        if (embargoDateInTheFuture) {
            embargoDate = dateInTheFutureAsStringInMethodeFormat();
        }

        return new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType(eomType)
                .withValue(buildEomFileValue(null, null, null, null))
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

    private Content createStandardExpectedContent() {
        return Content.builder()
                .withTitle(EXPECTED_TITLE)
                .withXmlBody("<body><p>some other random text</p></body>")
                .withByline("")
                .withBrands(new TreeSet<>(Collections.singletonList(financialTimesBrand)))
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
                .build();
    }
}
