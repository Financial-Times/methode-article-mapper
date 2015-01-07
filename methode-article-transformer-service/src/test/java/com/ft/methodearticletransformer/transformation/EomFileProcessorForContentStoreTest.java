package com.ft.methodearticletransformer.transformation;

import static com.ft.methodearticletransformer.methode.EomFileType.EOMCompoundStory;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.TreeSet;

import com.ft.content.model.Brand;
import com.ft.content.model.Content;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.methode.EmbargoDateInTheFutureException;
import com.ft.methodearticletransformer.methode.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.MethodeMarkedDeletedException;
import com.ft.methodearticletransformer.methode.MethodeMissingFieldException;
import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.SourceNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.UnsupportedTypeException;
import com.ft.methodearticletransformer.methode.WorkflowStatusNotEligibleForPublishException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EomFileProcessorForContentStoreTest {

	public static final String METHODE = "http://www.ft.com/ontology/origin/FTComMethode";
	@Rule
    public ExpectedException expectedException = ExpectedException.none();

	private static final String lastPublicationDateAsString = "20130813145815";

	private static final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
	private static final String TRANSFORMED_BODY = "<p>some other random text</p>";
	private static final String TRANSFORMED_BYLINE = "By Gillian Tett";
    private static final String FINANCIAL_TIMES_BRAND = "http://api.ft.com/things/dbb0bdae-1f0c-11e4-b0cb-b2227cce2b54";

    public static final Charset UTF8 = Charset.forName("UTF-8");

    private FieldTransformer bodyTransformer;
    private FieldTransformer bylineTransformer;
    private Brand financialTimesBrand;
    
    private final UUID uuid = UUID.randomUUID();
    private EomFile standardEomFile;
    private Content standardExpectedContent;

    private EomFileProcessorForContentStore eomFileProcessorForContentStore;
	private static final String TRANSACTION_ID = "tid_test";

    public EomFileProcessorForContentStoreTest() {
    }

    @Before
    public void setUp() throws Exception {
        bodyTransformer = mock(FieldTransformer.class);
        when(bodyTransformer.transform(anyString(), anyString())).thenReturn(TRANSFORMED_BODY);
        
        bylineTransformer = mock(FieldTransformer.class);
        when(bylineTransformer.transform(anyString(), anyString())).thenReturn(TRANSFORMED_BYLINE);

        financialTimesBrand = new Brand (FINANCIAL_TIMES_BRAND);

        standardEomFile = createStandardEomFile(uuid);
        standardExpectedContent = createStandardExpectedContent();

        eomFileProcessorForContentStore = new EomFileProcessorForContentStore(bodyTransformer, bylineTransformer, financialTimesBrand);
    }

    @Test(expected = MethodeMarkedDeletedException.class)
    public void shouldThrowExceptionIfMarkedDeleted(){
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFile(uuid, "True"))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = EmbargoDateInTheFutureException.class)
    public void shouldThrowExceptionIfEmbargoDateInTheFuture(){
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithEmbargoDateInTheFuture(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = NotWebChannelException.class)
    public void shouldThrowExceptionIfNoFtComChannel(){
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWithNoFtChannel(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = SourceNotEligibleForPublishException.class)
    public void shouldThrowExceptionIfNotFtSource(){
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileNonFtSource(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = WorkflowStatusNotEligibleForPublishException.class)
    public void shouldThrowExceptionIfWorkflowStatusNotEligibleForPublishing(){
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createStandardEomFileWorkflowStatusNotEligible(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = UnsupportedTypeException.class)
    public void shouldThrowUnsupportedTypeExceptionIfPublishingDwc(){
        final EomFile eomFile = new EomFile.Builder()
                .withValuesFrom(createDwcComponentFile(uuid))
                .build();
        Content content = eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID);
        fail("Content should not be returned" + content.toString());
    }

    @Test(expected = MethodeMissingFieldException.class)
    public void shouldThrowExceptionIfNoLastPublicationDate(){
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

        verify(bodyTransformer).transform("<body><p>random text for now</p></body>", TRANSACTION_ID);
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
				.withContentOrigin(METHODE, uuid.toString())
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

    private EomFile createStandardEomFile(UUID uuid) {
        return createStandardEomFile(uuid, "False", false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString);
    }

    private EomFile createStandardEomFileNonFtSource(UUID uuid) {
        return createStandardEomFile(uuid, "False", false, "FTcom", "Pepsi", EomFile.WEB_READY, lastPublicationDateAsString);
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted) {
        return createStandardEomFile(uuid, markedDeleted, false, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString);
    }

    private EomFile createStandardEomFileWithNoFtChannel(UUID uuid) {
        return createStandardEomFile(uuid, "False", false, "NotFTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString);
    }

    private EomFile createStandardEomFileWithEmbargoDateInTheFuture(UUID uuid) {
        return createStandardEomFile(uuid, "False", true, "FTcom", "FT", EomFile.WEB_READY, lastPublicationDateAsString);
    }

    private EomFile createStandardEomFileWorkflowStatusNotEligible(UUID uuid) {
        return createStandardEomFile(uuid, "False", true, "FTcom", "FT", "Stories/Edit", lastPublicationDateAsString);
    }

    private EomFile createStandardEomFileWithNoLastPublicationDate(UUID uuid) {
        return createStandardEomFile(uuid, "False", false, "FTcom", "FT", EomFile.WEB_READY, "");
    }

    private EomFile createStandardEomFile(UUID uuid, String markedDeleted, boolean embargoDateInTheFuture,
                                          String channel, String sourceCode, String workflowStatus, String lastPublicationDateAsString) {

        String embargoDate = "";
        if (embargoDateInTheFuture) {
            embargoDate = dateInTheFutureAsStringInMethodeFormat();
        }

        return new EomFile.Builder()
        	.withUuid(uuid.toString())
        	.withType(EOMCompoundStory.getTypeName())
        	.withValue(("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<!DOCTYPE doc SYSTEM \"/SysConfig/Rules/ftpsi.dtd\">" +
                    "<doc><lead><lead-headline><headline><ln>" +
                    "And sacked chimney-sweep pumps boss full of mayonnaise." +
                    "</ln></headline></lead-headline></lead>" +
                    "<story><text><body><p>random text for now</p></body>" +
                    "</text></story></doc>").getBytes(UTF8))
        	.withAttributes("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE ObjectMetadata SYSTEM \"/SysConfig/Classify/FTStories/classify.dtd\"><ObjectMetadata>" +
                    "<OutputChannels>" +
                    "<DIFTcom><DIFTcomLastPublication>" + lastPublicationDateAsString + "</DIFTcomLastPublication>" +
                    "<DIFTcomMarkDeleted>" + markedDeleted + "</DIFTcomMarkDeleted></DIFTcom>" +
                    "</OutputChannels>" +
                    "<EditorialNotes><EmbargoDate>" + embargoDate + "</EmbargoDate><Sources><Source><SourceCode>" + sourceCode + "</SourceCode></Source></Sources></EditorialNotes></ObjectMetadata>")
			.withSystemAttributes("<props><productInfo><name>" + channel + "</name>\n" +
                    "<issueDate>20131219</issueDate>\n" +
                    "</productInfo>\n" +
                    "<workFolder>/FT/Companies</workFolder>\n" +
                    "<templateName>/SysConfig/Templates/FT/Base-Story.xml</templateName>\n" +
                    "<summary>text text text text text text text text text text text text text text text\n" +
                    " text text text text te...</summary><wordCount>417</wordCount></props>")
			.withWorkflowStatus(workflowStatus)
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
				.withContentOrigin(METHODE, uuid.toString())
                .withUuid(uuid).build();
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
}
