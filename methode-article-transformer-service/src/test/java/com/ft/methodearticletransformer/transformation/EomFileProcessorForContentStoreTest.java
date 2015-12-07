package com.ft.methodearticletransformer.transformation;

import com.ft.content.model.Brand;
import com.ft.content.model.Comments;
import com.ft.content.model.Content;
import com.ft.content.model.Identifier;
import com.ft.methodearticletransformer.methode.*;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.util.ImageSetUuidGenerator;
import com.google.common.collect.ImmutableSortedSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.ft.methodearticletransformer.methode.EomFileType.EOMCompoundStory;
import static com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore.METHODE;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class EomFileProcessorForContentStoreTest {

    private static final String FALSE = "False";
    private static final Date LAST_MODIFIED = new Date();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final String lastPublicationDateAsString = "20130813145815";

    private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
    private static final String TRANSFORMED_BODY = "<p>some other random text</p>";
    private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
    public static final String FINANCIAL_TIMES_BRAND = "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54";

    public static final Charset UTF8 = Charset.forName("UTF-8");

    private FieldTransformer bodyTransformer;
    private FieldTransformer bylineTransformer;
    private Brand financialTimesBrand;

    private final UUID uuid = UUID.randomUUID();
    private EomFile standardEomFile;
    private Content standardExpectedContent;

    private EomFileProcessorForContentStore eomFileProcessorForContentStore;
    private static final String TRANSACTION_ID = "tid_test";
    private static final String simpleArticleXmlTemplate = readFile("article/simple_article_value.xml");
    private static final String articleWithImagesXmlTemplate = readFile("article/article_value_with_image.xml");
    private static final String articleAttributesXml = readFile("article/article_attributes.xml");
    private static final String articleSystemAttributesXml = readFile("article/article_system_attributes.xml");

    public EomFileProcessorForContentStoreTest() {
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

        eomFileProcessorForContentStore = new EomFileProcessorForContentStore(bodyTransformer, bylineTransformer, financialTimesBrand);
    }

    @Test(expected = MethodeMarkedDeletedException.class)
    public void shouldThrowExceptionIfMarkedDeleted() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFile(uuid, "True"))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = EmbargoDateInTheFutureException.class)
    public void shouldThrowExceptionIfEmbargoDateInTheFuture() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithEmbargoDateInTheFuture(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = NotWebChannelException.class)
    public void shouldThrowExceptionIfNoFtComChannel() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithNoFtChannel(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = SourceNotEligibleForPublishException.class)
    public void shouldThrowExceptionIfNotFtSource() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileNonFtSource(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = WorkflowStatusNotEligibleForPublishException.class)
    public void shouldThrowExceptionIfWorkflowStatusNotEligibleForPublishing() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWorkflowStatusNotEligible(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = UnsupportedTypeException.class)
    public void shouldThrowUnsupportedTypeExceptionIfPublishingDwc() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createDwcComponentFile(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = MethodeMissingFieldException.class)
    public void shouldThrowExceptionIfNoLastPublicationDate() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithNoLastPublicationDate(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test
    public void shouldNotBarfOnExternalDtd() {
        Content content = eomFileProcessorForContentStore.process(standardEomFile, TRANSACTION_ID);
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

        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);

        verify(bodyTransformer, times(1)).transform(isA(String.class), isA(String.class));
        assertThat(content, equalTo(expectedContent));
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

        Content content = eomFileProcessorForContentStore.process(eomFile, reference);

        assertThat(content, equalTo(expectedContent));
    }

    @Test
    public void shouldTransformBylineWhenPresentOnPublish() {
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .withValue(("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<!DOCTYPE doc SYSTEM \"/SysConfig/Rules/ftpsi.dtd\">" +
                        "<doc><lead><lead-headline><headline><ln>" +
                        "And sacked chimney-sweep pumps boss full of mayonnaise." +
                        "</ln></headline></lead-headline></lead>" +
                        "<story><text><byline>By <author-name>Gillian Tett</author-name></byline>" +
                        "<body><p>random text for now</p></body>" +
                        "</text></story></doc>").getBytes(UTF8))
                .build();

        final Content expectedContent = Content.builder()
                .withValuesFrom(standardExpectedContent)
                .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
                .withByline(TRANSFORMED_BYLINE).build();

        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);

        verify(bylineTransformer).transform("<byline>By <author-name>Gillian Tett</author-name></byline>",
                TRANSACTION_ID);
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

        eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
    }

    @Test
    public void testShouldAddMainImageIfPresent() throws Exception {
        final UUID imageUuid = UUID.randomUUID();
        final UUID expectedMainImageUuid = ImageSetUuidGenerator.fromImageUuid(imageUuid);
        final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid, "Primary size");

        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        assertThat(content.getMainImage(), equalTo(expectedMainImageUuid.toString()));
    }

    @Test
    public void testMainImageIsNullIfMissing() throws Exception {
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
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

        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);

        String expectedBody = "<body>" +
                "                <p>random text for now</p>" +
                "            </body>";
        assertThat(content.getMainImage(), nullValue());
        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }

    @Test
    public void testCommentsArePresent() {
        final EomFile eomFile = createStandardEomFile(uuid);

        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);

        assertThat(content.getComments(), notNullValue());
        assertThat(content.getComments().isEnabled(), is(true));
    }

    @Test(expected = MethodeMissingBodyException.class)
    public void thatTransformationFailsIfThereIsNoBody()
            throws Exception {

        String value = readFile("article/article_value_with_no_body.xml");
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(standardEomFile)
                .withValue(value.getBytes(UTF8))
                .build();

        eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
    }

    private void testMainImageReferenceIsPutInBodyWithMetadataFlag(String articleImageMetadataFlag, String expectedTransformedBody) {
        when(bodyTransformer.transform(anyString(), anyString())).then(returnsFirstArg());
        final UUID imageUuid = UUID.randomUUID();
        final UUID expectedMainImageUuid = ImageSetUuidGenerator.fromImageUuid(imageUuid);
        final EomFile eomFile = createStandardEomFileWithMainImage(uuid, imageUuid, articleImageMetadataFlag);
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);

        String expectedBody = String.format(expectedTransformedBody, expectedMainImageUuid);
        assertThat(content.getBody(), equalToIgnoringWhiteSpace(expectedBody));
    }

    private EomFile createStandardEomFile(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString, "True");
    }

    public static EomFile createStandardEomFileWithMainImage(UUID uuid, UUID mainImageUuid, String articleImageMetadataFlag) {
        return new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType(EOMCompoundStory.getTypeName())
                .withValue(String.format(articleWithImagesXmlTemplate, mainImageUuid).getBytes(UTF8))
                .withAttributes(String.format(articleAttributesXml, lastPublicationDateAsString, FALSE, articleImageMetadataFlag, FALSE, "", "FT"))
                .withSystemAttributes(String.format(articleSystemAttributesXml, "FTcom"))
                .withWorkflowStatus(EomFile.WEB_READY)
                .build();
    }

    private EomFile createStandardEomFileNonFtSource(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "Pepsi", EomFile.WEB_READY, lastPublicationDateAsString, FALSE);
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted) {
        return createStandardEomFile(uuid, markedDeleted, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString, FALSE);
    }

    private EomFile createStandardEomFileWithNoFtChannel(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "NotFTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString, FALSE);
    }

    private EomFile createStandardEomFileWithEmbargoDateInTheFuture(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, true, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString, FALSE);
    }

    private EomFile createStandardEomFileWorkflowStatusNotEligible(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, true, "FTcom", "FT", "Stories/Edit", lastPublicationDateAsString, FALSE);
    }

    private EomFile createStandardEomFileWithNoLastPublicationDate(UUID uuid) {
        return createStandardEomFile(uuid, FALSE, false, "FTcom", "FT", EomFile.WEB_READY, "", FALSE);
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted, boolean embargoDateInTheFuture,
                                          String channel, String sourceCode, String workflowStatus, String lastPublicationDateAsString, String commentsEnabled) {

        String embargoDate = "";
        if (embargoDateInTheFuture) {
            embargoDate = dateInTheFutureAsStringInMethodeFormat();
        }

        return new EomFile.Builder()
                .withUuid(uuid.toString())
                .withType(EOMCompoundStory.getTypeName())
                .withValue(simpleArticleXmlTemplate.getBytes(UTF8))
                .withAttributes(String.format(articleAttributesXml, lastPublicationDateAsString, markedDeleted, "No picture", commentsEnabled, embargoDate, sourceCode))
                .withSystemAttributes(String.format(articleSystemAttributesXml, channel))
                .withWorkflowStatus(workflowStatus)
                .withLastModified(LAST_MODIFIED)
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
                        "</dwc>\n").getBytes(UTF8))
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
                .withTitle("And sacked chimney-sweep pumps boss full of mayonnaise.")
                .withXmlBody("<p>some other random text</p>")
                .withByline("")
                .withBrands(new TreeSet<>(Arrays.asList(financialTimesBrand)))
                .withPublishedDate(toDate(lastPublicationDateAsString, DATE_TIME_FORMAT))
                .withIdentifiers(ImmutableSortedSet.of(new Identifier(METHODE, uuid.toString())))
                .withComments(new Comments(true))
                .withUuid(uuid)
                .withPublishReference(TRANSACTION_ID)
                .withLastModified(LAST_MODIFIED)
                .build();
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

    private static String readFile(final String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(EomFileProcessorForContentStoreTest.class.getClassLoader().getResource(path).toURI())), "UTF-8");
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
