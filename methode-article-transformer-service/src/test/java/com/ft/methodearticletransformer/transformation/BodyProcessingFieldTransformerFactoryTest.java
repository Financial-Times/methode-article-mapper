package com.ft.methodearticletransformer.transformation;

import static com.ft.methodetesting.xml.XmlMatcher.identicalXmlTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;

import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.bodyprocessing.richcontent.RichContentItem;
import com.ft.bodyprocessing.richcontent.Video;
import com.ft.bodyprocessing.richcontent.VideoMatcher;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.util.ImageSetUuidGenerator;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BodyProcessingFieldTransformerFactoryTest {
	
	@Rule
    public ExpectedException expectedException = ExpectedException.none();

    private FieldTransformer bodyTransformer;

    @Mock private MethodeFileService methodeFileService;
    @Mock private ResilientClient semanticStoreContentReaderClient;
    @Mock private VideoMatcher videoMatcher;
    @Mock private WebResource webResource;
    @Mock private Builder builder;
    @Mock private ClientResponse clientResponse;
    @Mock private InputStream inputStream;
    
	private static final String TRANSACTION_ID = "tid_test";
    private Video exampleYouTubeVideo;
    private Video exampleVimeoVideo;

    @Before
    public void setup() {
        when(methodeFileService.assetTypes(anySetOf(String.class), anyString())).thenReturn(Collections.<String, EomAssetType>emptyMap());
        EomAssetType storyAsset1 = new EomAssetType.Builder()
                .uuid("49336a18-051c-11e3-98a0-002128161462")
                .type("EOM::Story")
                .build();
        EomAssetType storyAsset2 = new EomAssetType.Builder()
                .uuid("49336a18-051c-11e3-98a0-001234567890")
                .type("EOM::Story")
                .build();
        when(methodeFileService.assetTypes(Collections.singleton("49336a18-051c-11e3-98a0-002128161462"), TRANSACTION_ID))
                .thenReturn(Collections.singletonMap("49336a18-051c-11e3-98a0-002128161462", storyAsset1));
        Set<String> setWithBothUuids = new HashSet<>();
        setWithBothUuids.add("49336a18-051c-11e3-98a0-002128161462");
        setWithBothUuids.add("49336a18-051c-11e3-98a0-001234567890");
        Map<String, EomAssetType> mapWithBothUuids = new HashMap<>();
        mapWithBothUuids.put("49336a18-051c-11e3-98a0-002128161462", storyAsset1);
        mapWithBothUuids.put("49336a18-051c-11e3-98a0-001234567890", storyAsset2);
        when(methodeFileService.assetTypes(setWithBothUuids, TRANSACTION_ID))
                .thenReturn(mapWithBothUuids);

        exampleVimeoVideo = new Video();
        exampleVimeoVideo.setUrl("https://www.vimeo.com/77761436");
        exampleVimeoVideo.setEmbedded(true);

        exampleYouTubeVideo = new Video();
        exampleYouTubeVideo.setUrl("https://www.youtube.com/watch?v=77761436");
        exampleYouTubeVideo.setEmbedded(true);

        bodyTransformer = new BodyProcessingFieldTransformerFactory(methodeFileService, semanticStoreContentReaderClient, videoMatcher).newInstance();
        when(semanticStoreContentReaderClient.resource((URI)any())).thenReturn(webResource);
        when(webResource.accept(MediaType.APPLICATION_JSON_TYPE)).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.get(ClientResponse.class)).thenReturn(clientResponse);
        when(clientResponse.getStatus()).thenReturn(404);
        when(clientResponse.getEntityInputStream()).thenReturn(inputStream);
    }

    @Test
    public void tagsShouldBeTransformed() {
        final String originalBody = "<body><p><web-inline-picture fileref=\"/FT/Graphics/Online/Z_" +
                "Undefined/2013/04/600-Saloua-Raouda-Choucair-02.jpg?uuid=7784185e-a888-11e2-8e5d-00144feabdc0\" " +
                "tmx=\"600 445 600 445\"/>\n</p>\n<p id=\"U1060483110029GKD\">In Paris in the late 1940s, a publicity-hungry gallerist " +
                "invited a young, beautiful, unknown Lebanese artist to pose for a photograph alongside Picasso, “before death overtakes him”. " +
                "Without hesitation, Saloua Raouda Choucair said, “As far as I’m concerned, he’s already dead.”</p>\n\n\n" +
                "<p><br/></p><p><br/> </p><p>Did she protest too much? " +
                "Tate’s poster image for the retrospective <i>Saloua Raouda Choucair</i> is a classic post-cubist self-portrait. The artist " +
                "has simplified her features into a mask-like countenance; her clothes – white turban, green sweater, ochre jacket – are " +
                "composed of angular, geometric elements; a background of interlocking jagged shapes underlines the formality of the endeavour. " +
                "It is an engaging image, dominated by the fierce, unswerving gaze of the almond-eyes and the delicately painted turban, " +
                "enclosing the head as if to announce self-reliance, the containment of an inner life. Daring you to want to know more, " +
                "it also keeps you at a distance.</p>\n<p>Raouda Choucair is still unknown, and you can see why Tate Modern selected this " +
                "image to advertise her first western retrospective, which opened this week. But it is a disingenuous choice: the painting " +
                "is the sole portrait in the show, and a rare figurative work. The only others are nudes, made while Raouda Choucair studied " +
                "with “tubist” painter Fernand Léger; they subvert his muscly female figures into awkwardly posed blocks of flesh, " +
                "breasts and faces sketched rudimentarily, to imply a feminist agenda – models reading about art history " +
                "in “Les Peintres Célèbres”, or occupied with housework in “Chores”.</p>\n</body>";

        //Does include some strange extra spaces in the output file
        final String expectedTransformedBody = String.format("<body><p><content data-embedded=\"true\" id=\"%s\" type=\"http://www.ft.com/ontology/content/ImageSet\"/>\n</p>\n" +
                "<p>In Paris in the late 1940s, a publicity-hungry gallerist invited a young, beautiful, unknown Lebanese artist to pose for a photograph " +
                "alongside Picasso, “before death overtakes him”. Without hesitation, Saloua Raouda Choucair said, “As far as I’m concerned, he’s already dead.”</p>\n" +
                "<p>Did she protest too much? Tate’s poster image for the retrospective <em>Saloua Raouda Choucair</em> is a classic post-cubist self-portrait. " +
                "The artist has simplified her features into a mask-like countenance; her clothes – white turban, green sweater, " +
                "ochre jacket – are composed of angular, geometric elements; a background of interlocking jagged shapes underlines the formality of the endeavour. " +
                "It is an engaging image, dominated by the fierce, unswerving gaze of the almond-eyes and the delicately painted turban, enclosing " +
                "the head as if to announce self-reliance, the containment of an inner life. Daring you to want to know more, it also keeps you at a distance.</p>" +
                "\n<p>Raouda Choucair is still unknown, and you can see why Tate Modern selected this image to advertise her first western retrospective, " +
                "which opened this week. But it is a disingenuous choice: the painting is the sole portrait in the show, and a rare figurative work. " +
                "The only others are nudes, made while Raouda Choucair studied with “tubist” painter Fernand Léger; they subvert his muscly female figures into awkwardly " +
                "posed blocks of flesh, breasts and faces sketched rudimentarily, to imply a feminist agenda – models " +
                "reading about art history in “Les Peintres Célèbres”, or occupied with housework in “Chores”.</p>\n</body>",
                ImageSetUuidGenerator.fromImageUuid(java.util.UUID.fromString("7784185e-a888-11e2-8e5d-00144feabdc0")).toString());

        checkTransformation(originalBody, expectedTransformedBody);
    }
    
    @Test
    public void shouldThrowExceptionIfBodyNull() {
    	expectedException.expect(BodyProcessingException.class);
        expectedException.expect(hasProperty("message", equalTo("Body is null")));
    	checkTransformation(null, "");
    }
    
    @Test
    public void paraWithOnlyNewlineShouldBeRemoved() {
    	checkTransformationToEmpty("<p>\n</p>");
    }

    @Test
    public void paraWithAllElementsRemovedShouldBeRemoved() {
    	checkTransformationToEmpty("<p><canvas>Canvas is removed</canvas></p>");
    }

    @Test
    public void encodedNbspShouldBeReplacedWithSpace() {
        checkTransformation("<body>This is a sentence .</body>",
				String.format("<body>This is a sentence%s.</body>", String.valueOf('\u00A0')));
    }

    @Test
    public void pullQuotesShouldBeReplacedWithAppropriateTags() {
        String pullQuoteFromMethode = "<body><p>patelka</p><web-pull-quote align=\"left\" channel=\"FTcom\">&lt;\n" +
                "\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-text>\n" +
                "\t\t\t\t\t<p>It suits the extremists to encourage healthy eating.</p>\n" +
                "\t\t\t\t</web-pull-quote-text>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-source>source1</web-pull-quote-source>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</table>&gt;\n" +
                "</web-pull-quote></body>";

        String processedPullQuote = "<body><p>patelka</p><pull-quote>" +
                "<pull-quote-text><p>It suits the extremists to encourage healthy eating.</p></pull-quote-text>" +
                "<pull-quote-source>source1</pull-quote-source>" +
                "</pull-quote></body>";

        checkTransformation(pullQuoteFromMethode, processedPullQuote);
    }

    @Test
    public void pullQuotesWrittenOutsidePtags() {
        String pullQuoteFromMethode = "<body><p>patelka</p><p><web-pull-quote align=\"left\" channel=\"FTcom\">&lt;\n" +
                "\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-text>\n" +
                "\t\t\t\t\t<p>It suits the extremists to encourage healthy eating.</p>\n" +
                "\t\t\t\t</web-pull-quote-text>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-source>source1</web-pull-quote-source>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</table>&gt;\n" +
                "</web-pull-quote></p></body>";

        String processedPullQuote = "<body><p>patelka</p><pull-quote>" +
                "<pull-quote-text><p>It suits the extremists to encourage healthy eating.</p></pull-quote-text>" +
                "<pull-quote-source>source1</pull-quote-source>" +
                "</pull-quote></body>";

        checkTransformation(pullQuoteFromMethode, processedPullQuote);
    }


    @Test
	public void markupInsidePullQuotesShouldBeTransformed() {
		String pullQuoteFromMethode = "<body><p>patelka</p><web-pull-quote align=\"left\" channel=\"FTcom\">&lt;\n" +
				"\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
				"\t\t<tr>\n" +
				"\t\t\t<td>\n" +
				"\t\t\t\t<web-pull-quote-text>\n" +
				"\t\t\t\t\t<p>It suits the extremists to encourage <b>healthy</b> eating.</p>\n" +
				"\t\t\t\t</web-pull-quote-text>\n" +
				"\t\t\t</td>\n" +
				"\t\t</tr>\n" +
				"\t\t<tr>\n" +
				"\t\t\t<td>\n" +
				"\t\t\t\t<web-pull-quote-source><b>source1</b></web-pull-quote-source>\n" +
				"\t\t\t</td>\n" +
				"\t\t</tr>\n" +
				"\t</table>&gt;\n" +
				"</web-pull-quote></body>";

		String processedPullQuote = "<body><p>patelka</p><pull-quote>" +
				"<pull-quote-text><p>It suits the extremists to encourage <strong>healthy</strong> eating.</p></pull-quote-text>" +
				"<pull-quote-source><strong>source1</strong></pull-quote-source>" +
				"</pull-quote></body>";

		checkTransformation(pullQuoteFromMethode, processedPullQuote);
	}

    @Test
    public void pullQuotesShouldReturnEmptySrcIfDummySource() {
        String pullQuoteFromMethode = "<body><p>patelka</p><web-pull-quote align=\"left\" channel=\"FTcom\">&lt;\n" +
                "\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-text>\n" +
                "\t\t\t\t\t<p>It suits the extremists to encourage healthy eating.</p>\n" +
                "\t\t\t\t</web-pull-quote-text>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-source><?EM-dummyText [Insert source here]?></web-pull-quote-source>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</table>&gt;\n" +
                "</web-pull-quote></body>";

        String processedPullQuote = "<body><p>patelka</p><pull-quote>" +
                "<pull-quote-text><p>It suits the extremists to encourage healthy eating.</p></pull-quote-text>" +
                "<pull-quote-source></pull-quote-source>" +
                "</pull-quote></body>";

        checkTransformation(pullQuoteFromMethode, processedPullQuote);
    }

    @Test
    public void shouldNotBarfOnTwoPullQuotes() {
        String pullQuoteFromMethode = "<body><p>patelka</p><web-pull-quote align=\"left\" channel=\"FTcom\">&lt;\n" +
                "\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-text>\n" +
                "\t\t\t\t\t<p>It suits the extremists to encourage healthy eating.</p>\n" +
                "\t\t\t\t</web-pull-quote-text>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-source>source1</web-pull-quote-source>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</table>&gt;\n" +
                "</web-pull-quote>" +
                "<p>" +
                "<web-pull-quote align=\"left\" channel=\"FTcom\">" +
                "\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-text>\n" +
                "\t\t\t\t\t<p>It suits the people to encourage drinking.</p>\n" +
                "\t\t\t\t</web-pull-quote-text>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-source>source2</web-pull-quote-source>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</table>&gt;\n" +
                "</web-pull-quote>" +
                "</p></body>";

        String processedPullQuote = "<body><p>patelka</p><pull-quote>" +
                "<pull-quote-text><p>It suits the extremists to encourage healthy eating.</p></pull-quote-text>" +
                "<pull-quote-source>source1</pull-quote-source>" +
                "</pull-quote>" +
                "<pull-quote>" +
                "<pull-quote-text><p>It suits the people to encourage drinking.</p></pull-quote-text>" +
                "<pull-quote-source>source2</pull-quote-source>" +
                "</pull-quote>" +
                "</body>";

        checkTransformation(pullQuoteFromMethode, processedPullQuote);
    }

    @Test
    public void bigNumbersShouldBeReplacedWithAppopriateTags() {
        String bigNumberFromMethode = "<body><p>patelka</p><promo-box class=\"numbers-component\" align=\"right\">" +
                "<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-headline><p class=\"title\">£350m</p>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>Cost of the rights expected to increase by one-third — or about <b>£350m</b> a year — although some anticipate inflation of up to 70%</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</promo-box></body>";

        String processedBigNumber = "<body><p>patelka</p><big-number>" +
                "<big-number-headline><p>£350m</p></big-number-headline>" +
                "<big-number-intro><p>Cost of the rights expected to increase by one-third — or about <strong>£350m</strong> a year — although some anticipate inflation of up to 70%</p></big-number-intro>" +
                "</big-number></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void bigNumbersShouldReturnEmptyHeadlineIfHeadlineIsEmpty() {
        String bigNumberFromMethode = "<body><p>patelka</p><promo-box class=\"numbers-component\" align=\"right\">" +
                "<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-headline>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</promo-box></body>";

        String processedBigNumber = "<body><p>patelka</p><big-number>" +
                "<big-number-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p></big-number-intro>" +
                "</big-number></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

	@Test
	public void nonClassNumbersComponentIsPromoBoxAndImagePreservedIfPresent() {
		String bigNumberFromMethode = "<body><p>This is the beginning of a sentence.<promo-box align=\"left\">" +
				"<table align=\"left\" cellpadding=\"6px\" width=\"170px\"><tr><td>" +
				"<promo-title><p><a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"</td></tr><tr><td><promo-headline><p>Headline</p></promo-headline></td></tr><tr><td>" +
				"<promo-image uuid=\"432b5632-9e79-11e0-9469-00144feabdc0\" fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/06/SEC_ft500.jpg?uuid=432b5632-9e79-11e0-9469-00144feabdc0\"/>" +
				"</td></tr><tr><td><promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro>" +
				"</td></tr><tr><td><promo-link><p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"/></p></promo-link>" +
				"</td></tr></table></promo-box>This is the end of the sentence.</p></body>";

		String processedPromoBox = "<body><p>This is the beginning of a sentence.</p><promo-box><promo-title><p>" +
				"<a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"<promo-headline><p>Headline</p></promo-headline><promo-image>" +
				"<content data-embedded=\"true\" id=\"432b5632-9e79-11e0-0a0f-978e959e1689\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content></promo-image>" +
				"<promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro><promo-link>" +
				"<p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"></a></p></promo-link></promo-box>" +
                "<p>This is the end of the sentence.</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}

	@Test
	public void nonClassNumbersComponentIsPromoBoxAndTitleRemovedIfDummyText() {
		String bigNumberFromMethode = "<body><p>This is the beginning of a sentence.<promo-box align=\"right\" channel=\"FTcom\"><table width=\"156px\" align=\"right\" cellpadding=\"4px\"><tr><td align=\"left\"><promo-title><p><?EM-dummyText Sidebar title ?>\n" +
				"</p>\n" +
				"</promo-title>\n" +
				"</td>\n" +
				"</tr>\n" +
				"<tr><td align=\"left\"><promo-headline><p>Labour attacks ministerial role of former HSBC chairman</p>\n" +
				"</promo-headline>\n" +
				"</td>\n" +
				"</tr>\n" +
				"<tr><td><web-master xtransform=\"scale(0.1538 0.1538)\" tmx=\"2048 1152 315 177\" width=\"2048\" height=\"1152\" fileref=\"/FT/Graphics/Online/Master_2048x1152/Martin/butterfly-2048x1152.jpg?uuid=17ee1f24-ff46-11e2-9b3b-002128161462\" dtxInsert=\"Web Master\" id=\"U112075852229295\"/>\n" +
				"</td>\n" +
				"</tr>\n" +
				"<tr><td align=\"left\"><promo-intro><p>The revelations about HSBC’s Swiss operations reverberated around Westminster on bold <b>Monday</b>, with Labour claiming the coalition was alerted in 2010 to strikeout <span channel=\"!\">alleged </span>malpractice at the bank and took no action.</p>\n" +
				"<p><a dtxInsert=\"Sidebar - right aligned\" href=\"/FT/Content/World%20News/Stories/Live/hsbcpoltix.uk.9.xml?uuid=2f9b640c-b056-11e4-a2cc-00144feab7de\">Continue reading</a>" +
				"</p>\n" +
				"</promo-intro>\n" +
				"</td>\n" +
				"</tr>\n" +
				"</table>\n" +
				"</promo-box>This is the end of the sentence.</p></body>";

		String processedPromoBox = "<body><p>This is the beginning of a sentence.</p><promo-box>" +
				"<promo-headline><p>Labour attacks ministerial role of former HSBC chairman</p></promo-headline><promo-image>" +
				"<content data-embedded=\"true\" id=\"17ee1f24-ff46-11e2-055d-97bbf262bf2b\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content></promo-image>" +
				"<promo-intro><p>The revelations about HSBC’s Swiss operations reverberated around Westminster on bold <strong>Monday</strong>, with Labour claiming the coalition was alerted in 2010 to strikeout malpractice at the bank and took no action.</p>\n" +
				"<p><a href=\"/FT/Content/World%20News/Stories/Live/hsbcpoltix.uk.9.xml?uuid=2f9b640c-b056-11e4-a2cc-00144feab7de\">Continue reading</a></p></promo-intro>" +
				"</promo-box><p>This is the end of the sentence.</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}

	@Test
	public void nonClassNumbersComponentIsPromoBoxAndBIsConvertedToStrong() {
		String bigNumberFromMethode = "<body><p>This is the beginning of a sentence.<promo-box align=\"left\">" +
				"<table align=\"left\" cellpadding=\"6px\" width=\"170px\"><tr><td>" +
				"<promo-title><p><a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"</td></tr><tr><td><promo-headline><p>Headline</p></promo-headline></td></tr><tr><td>" +
				"<promo-image uuid=\"432b5632-9e79-11e0-9469-00144feabdc0\" fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/06/SEC_ft500.jpg?uuid=432b5632-9e79-11e0-9469-00144feabdc0\"/>" +
				"</td></tr><tr><td><promo-intro><p>The risers and fallers in our <b>annual</b> list of the world’s biggest companies</p></promo-intro>" +
				"</td></tr><tr><td><promo-link><p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"/></p></promo-link>" +
				"</td></tr></table></promo-box>This is the end of the sentence.</p></body>";

		String processedPromoBox = "<body><p>This is the beginning of a sentence.</p><promo-box><promo-title><p>" +
				"<a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"<promo-headline><p>Headline</p></promo-headline><promo-image>" +
				"<content data-embedded=\"true\" id=\"432b5632-9e79-11e0-0a0f-978e959e1689\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content></promo-image>" +
				"<promo-intro><p>The risers and fallers in our <strong>annual</strong> list of the world’s biggest companies</p></promo-intro><promo-link>" +
				"<p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"></a></p></promo-link></promo-box>" +
                "<p>This is the end of the sentence.</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}

	@Test
	public void nonClassNumbersComponentIsOmittedIfNoValuesPresent() {
		String bigNumberFromMethode = "<body><p>patelka</p><promo-box align=\"left\">" +
				"<table align=\"left\" cellpadding=\"6px\" width=\"170px\"><tr><td>" +
				"<promo-title><p></p></promo-title>" +
				"</td></tr><tr><td><promo-headline><p></p></promo-headline></td></tr><tr><td>" +
				"</td></tr><tr><td><promo-intro><p></p></promo-intro>" +
				"</td></tr><tr><td><promo-link><p></p></promo-link>" +
				"</td></tr></table></promo-box></body>";

		String processedPromoBox = "<body><p>patelka</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}

	@Test
	public void nonClassNumbersComponentIsPromoBoxEvenWhenTitleEmpty() {
		String bigNumberFromMethode = "<body><p>This is the beginning of a sentence.<promo-box align=\"left\">" +
				"<table align=\"left\" cellpadding=\"6px\" width=\"170px\"><tr><td>" +
				"<promo-title><p></p></promo-title>" +
				"</td></tr><tr><td><promo-headline><p>Headline</p></promo-headline></td></tr><tr><td>" +
				"<promo-image uuid=\"432b5632-9e79-11e0-9469-00144feabdc0\" fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/06/SEC_ft500.jpg?uuid=432b5632-9e79-11e0-9469-00144feabdc0\"/>" +
				"</td></tr><tr><td><promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro>" +
				"</td></tr><tr><td><promo-link><p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"/></p></promo-link>" +
				"</td></tr></table></promo-box>This is the end of the sentence.</p></body>";

		String processedPromoBox = "<body><p>This is the beginning of a sentence.</p><promo-box>" +
				"<promo-headline><p>Headline</p></promo-headline><promo-image>" +
				"<content data-embedded=\"true\" id=\"432b5632-9e79-11e0-0a0f-978e959e1689\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content></promo-image>" +
				"<promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro><promo-link>" +
				"<p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"></a></p></promo-link></promo-box>" +
                "<p>This is the end of the sentence.</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}


	@Test
	public void nonClassNumbersComponentIsPromoBoxEvenWhenTitleMissing() {
		String bigNumberFromMethode = "<body><p>This is the beginning of a sentence.<promo-box align=\"left\">" +
				"<table align=\"left\" cellpadding=\"6px\" width=\"170px\"><tr><td>" +
				"</td></tr><tr><td><promo-headline><p>Headline</p></promo-headline></td></tr><tr><td>" +
				"<promo-image uuid=\"432b5632-9e79-11e0-9469-00144feabdc0\" fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/06/SEC_ft500.jpg?uuid=432b5632-9e79-11e0-9469-00144feabdc0\"/>" +
				"</td></tr><tr><td><promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro>" +
				"</td></tr><tr><td><promo-link><p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"/></p></promo-link>" +
				"</td></tr></table></promo-box>This is the end of the sentence.</p></body>";

		String processedPromoBox = "<body><p>This is the beginning of a sentence.</p><promo-box>" +
				"<promo-headline><p>Headline</p></promo-headline><promo-image>" +
				"<content data-embedded=\"true\" id=\"432b5632-9e79-11e0-0a0f-978e959e1689\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content></promo-image>" +
				"<promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro><promo-link>" +
				"<p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"></a></p></promo-link></promo-box>" +
                "<p>This is the end of the sentence.</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}

	@Test
	public void nonClassNumbersComponentIsPromoBoxAndImageNotPreservedIfNotFileRefEmpty() {
		String bigNumberFromMethode = "<body><p>This is the beginning of a sentence.<promo-box align=\"left\">" +
				"<table align=\"left\" cellpadding=\"6px\" width=\"170px\"><tr><td>" +
				"<promo-title><p><a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"</td></tr><tr><td><promo-headline><p>Headline</p></promo-headline></td></tr><tr><td>" +
				"<promo-image fileref=\"\"/>" +
				"</td></tr><tr><td><promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro>" +
				"</td></tr><tr><td><promo-link><p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"/></p></promo-link>" +
				"</td></tr></table></promo-box>This is the end of the sentence.</p></body>";

		String processedPromoBox = "<body><p>This is the beginning of a sentence.</p><promo-box><promo-title><p>" +
				"<a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"<promo-headline><p>Headline</p></promo-headline>" +
				"<promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro><promo-link>" +
				"<p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"></a></p></promo-link></promo-box>" +
                "<p>This is the end of the sentence.</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}

	@Test
	public void nonClassNumbersComponentIsPromoBoxAndImageNotPreservedIfNotPresent() {
		String bigNumberFromMethode = "<body><p>This is the beginning of a sentence.<promo-box align=\"left\">" +
				"<table align=\"left\" cellpadding=\"6px\" width=\"170px\"><tr><td>" +
				"<promo-title><p><a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"</td></tr><tr><td><promo-headline><p>Headline</p></promo-headline></td></tr><tr><td>" +
				"</td></tr><tr><td><promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro>" +
				"</td></tr><tr><td><promo-link><p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"/></p></promo-link>" +
				"</td></tr></table></promo-box>This is the end of the sentence.</p></body>";

		String processedPromoBox = "<body><p>This is the beginning of a sentence.</p><promo-box><promo-title><p>" +
				"<a href=\"http://www.ft.com/reports/ft-500-2011\" title=\"www.ft.com\">FT 500</a></p></promo-title>" +
				"<promo-headline><p>Headline</p></promo-headline>" +
				"<promo-intro><p>The risers and fallers in our annual list of the world’s biggest companies</p></promo-intro><promo-link>" +
				"<p><a href=\"http://www.ft.com/cms/s/0/0bdf4bb6-6676-11e4-8bf6-00144feabdc0.html\"></a></p></promo-link></promo-box>" +
                "<p>This is the end of the sentence.</p></body>";

		checkTransformation(bigNumberFromMethode, processedPromoBox);
	}

    @Test
    public void shouldNotBarfOnTwoPromoBoxes() {
        String bigNumberFromMethode = "<body><p>A big number!</p>\n" +
                "<p><promo-box align=\"left\" class=\"numbers-component\"><table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-headline><p class=\"title\">£350M</p>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>The cost of eating at Leon and Tossed every single day.</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</promo-box></p>\n" +
                "<p></p>\n" +
                "<p></p>\n" +
                "<p>A big number right aligned.</p>\n" +
                "<p><promo-box align=\"right\" class=\"numbers-component\"><table width=\"170px\" align=\"right\" cellpadding=\"6px\"><tr><td><promo-headline><p>52p</p>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>The weekly saving made by making your own lunch.</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</promo-box>\n" +
                "</p>\n" +
                "</body>";

        String processedBigNumber = "<body><p>A big number!</p>\n" +
                "<big-number><big-number-headline><p>£350M</p></big-number-headline>" +
                "<big-number-intro><p>The cost of eating at Leon and Tossed every single day.</p></big-number-intro>" +
                "</big-number>\n\n\n" +
                "<p>A big number right aligned.</p>\n" +
                "<big-number>" +
                "<big-number-headline><p>52p</p></big-number-headline>" +
                "<big-number-intro><p>The weekly saving made by making your own lunch.</p></big-number-intro>" +
                "</big-number>\n" +
                "</body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void shouldTransformDataTableWithDifferentFormatting() {
        String dataTableFromMethode = "<body><div><web-table><table class=\"data-table\" width=\"100%\"><caption>Table (Falcon Style)</caption>\n" +
                "<thead><tr><th>Column A</th>\n" +
                "<th>Column B</th>\n" +
                "<th><b>Column C</b></th>\n" +
                "<th>Column D</th>\n" +
                "</tr>\n" +
                "</thead>\n" +
                "<tbody><tr><td>0</td>\n" +
                "<td>1</td>\n" +
                "<td><b>2</b></td>\n" +
                "<td>3</td>\n" +
                "</tr>\n" +
                "<tr><td>4</td>\n" +
                "<td>5</td>\n" +
                "<td>6</td>\n" +
                "<td>7</td>\n" +
                "</tr>\n" +
                "</tbody>\n" +
                "</table>\n" +
                "</web-table>\n" +
                "</div></body>";

        String processedDataTable = "<body><table class=\"data-table\"><caption>Table (Falcon Style)</caption>\n" +
                "<thead><tr><th>Column A</th>\n" +
                "<th>Column B</th>\n" +
                "<th><strong>Column C</strong></th>\n" +
                "<th>Column D</th>\n" +
                "</tr>\n" +
                "</thead>\n" +
                "<tbody><tr><td>0</td>\n" +
                "<td>1</td>\n" +
                "<td><strong>2</strong></td>\n" +
                "<td>3</td>\n" +
                "</tr>\n" +
                "<tr><td>4</td>\n" +
                "<td>5</td>\n" +
                "<td>6</td>\n" +
                "<td>7</td>\n" +
                "</tr>\n" +
                "</tbody>\n" +
                "</table>\n\n</body>";

        checkTransformation(dataTableFromMethode, processedDataTable);


    }

    @Test
    public void shouldTransformDataTable() {
        String dataTableFromMethode = "<body><div><table class=\"data-table\" border=\"\" cellspacing=\"\" cellpadding=\"\" " +
                "id=\"U1817116616509jH\" width=\"100%\"><caption id=\"k63G\"><span id=\"U181711661650mIC\">KarCrash Q1  02/2014- period from to 09/2014</span>\n" +
                "</caption>\n" +
                "<tr><th width=\"25%\">Sales</th>\n" +
                "<th width=\"25%\">Net profit</th>\n" +
                "<th width=\"25%\">Earnings per share</th>\n" +
                "<th width=\"25%\">Dividend</th>\n" +
                "</tr>\n" +
                "<tr><td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "</tr>\n" +
                "<tr><td align=\"center\" width=\"25%\" valign=\"middle\">324↑ ↓324</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">453↑ ↓435</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">123↑ ↓989</td>\n" +
                "<td width=\"25%\" align=\"center\" valign=\"middle\">748↑ ↓986</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</div></body>";

        String processedDataTable = "<body><table class=\"data-table\">" +
                "<caption>KarCrash Q1  02/2014- period from to 09/2014\n" +
                "</caption>\n" +
                "<tr><th>Sales</th>\n" +
                "<th>Net profit</th>\n" +
                "<th>Earnings per share</th>\n" +
                "<th>Dividend</th>\n" +
                "</tr>\n" +
                "<tr><td>€</td>\n" +
                "<td>€</td>\n" +
                "<td>€</td>\n" +
                "<td>€</td>\n" +
                "</tr>\n" +
                "<tr><td>324↑ ↓324</td>\n" +
                "<td>453↑ ↓435</td>\n" +
                "<td>123↑ ↓989</td>\n" +
                "<td>748↑ ↓986</td>\n" +
                "</tr>\n" +
                "</table>\n</body>";

        checkTransformation(dataTableFromMethode, processedDataTable);
    }

    @Test
    public void shouldNotTransformTable() {
        String tableFromMethode = "<body><div><table class=\"pseudo-data-table\" border=\"\" cellspacing=\"\" cellpadding=\"\" " +
                "id=\"U1817116616509jH\" width=\"100%\"><caption id=\"k63G\"><span id=\"U181711661650mIC\">KarCrash Q1  02/2014- period from to 09/2014</span>\n" +
                "</caption>\n" +
                "<tr><th width=\"25%\">Sales</th>\n" +
                "<th width=\"25%\">Net profit</th>\n" +
                "<th width=\"25%\">Earnings per share</th>\n" +
                "<th width=\"25%\">Dividend</th>\n" +
                "</tr>\n" +
                "<tr><td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">€</td>\n" +
                "</tr>\n" +
                "<tr><td align=\"center\" width=\"25%\" valign=\"middle\">324↑ ↓324</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">453↑ ↓435</td>\n" +
                "<td align=\"center\" width=\"25%\" valign=\"middle\">123↑ ↓989</td>\n" +
                "<td width=\"25%\" align=\"center\" valign=\"middle\">748↑ ↓986</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</div></body>";

        String processedTable = "<body>\n</body>";

        checkTransformation(tableFromMethode, processedTable);
    }

    @Test
    public void promoBoxWithPromoTitleThatIsEmptyIsBigNumber() {
        String bigNumberFromMethode = "<body><p>patelka</p><p><promo-box class=\"numbers-component\" align=\"left\">&lt;<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-title>" +
                "</promo-title>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-headline><p>£350m</p>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>&gt;</promo-box></p></body>";

        String processedBigNumber = "<body><p>patelka</p><big-number>" +
                "<big-number-headline><p>£350m</p></big-number-headline>" +
                "<big-number-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p></big-number-intro>" +
                "</big-number></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void emptyBigNumbersShouldBeOmitted() {
        String bigNumberFromMethode = "<body><p>patelka</p><promo-box class=\"numbers-component\" align=\"right\">" +
                "<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-headline><p class=\"title\"></p>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p></p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</promo-box></body>";

        String processedBigNumber = "<body><p>patelka</p></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void emptyPullQuotesShouldNotBeWritten() {
        String pullQuoteFromMethode = "<body><p>patelka</p><web-pull-quote align=\"left\" channel=\"FTcom\">&lt;\n" +
                "\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-text>\n" +
                "\t\t\t\t</web-pull-quote-text>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-source></web-pull-quote-source>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</table>&gt;\n" +
                "</web-pull-quote></body>";

        String processedPullQuote = "<body><p>patelka</p></body>";

        checkTransformation(pullQuoteFromMethode, processedPullQuote);
    }

    @Test
    public void pullQuotesWithNoSourceShouldBeWritten() {
        String pullQuoteFromMethode = "<body><p>patelka</p><web-pull-quote align=\"left\" channel=\"FTcom\">&lt;\n" +
                "\t<table align=\"left\" cellpadding=\"6px\" width=\"170px\">\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-text>It suits the extremists to encourage healthy eating.\n" +
                "\t\t\t\t</web-pull-quote-text>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t\t<tr>\n" +
                "\t\t\t<td>\n" +
                "\t\t\t\t<web-pull-quote-source></web-pull-quote-source>\n" +
                "\t\t\t</td>\n" +
                "\t\t</tr>\n" +
                "\t</table>&gt;\n" +
                "</web-pull-quote></body>";

        String processedPullQuote = "<body><p>patelka</p><pull-quote>" +
                "<pull-quote-text>It suits the extremists to encourage healthy eating.</pull-quote-text>" +
                "<pull-quote-source></pull-quote-source>" +
                "</pull-quote>" +
                "</body>";

        checkTransformation(pullQuoteFromMethode, processedPullQuote);
    }
    
    @Test
    public void slideshowShouldBeConvertedToPointToSlideshowOnWebsite() {
        String slideshowFromMethode = "<body><p>Embedded Slideshow</p>" +
                "<p><a type=\"slideshow\" dtxInsert=\"slideshow\" href=\"/FT/Content/Companies/Stories/Live/PlainSlideshow.gallery.xml?uuid=49336a18-051c-11e3-98a0-002128161462\">" + 
                "<DIHeadlineCopy>One typical, bog-standard slideshow headline update 2</DIHeadlineCopy></a></p></body>";
        
        String processedSlideshow = "<body><p>Embedded Slideshow</p>" +
        		"<p><a data-asset-type=\"slideshow\" data-embedded=\"true\" href=\"http://www.ft.com/cms/s/49336a18-051c-11e3-98a0-002128161462.html#slide0\"></a></p></body>";
        
        checkTransformation(slideshowFromMethode, processedSlideshow);
    }

    @Test
    public void slideshowWithEmptyHeadlineShouldBeConvertedToPointToSlideshowOnWebsite() {
        String slideshowFromMethode = "<body><p>Embedded Slideshow</p>" +
                "<p><a type=\"slideshow\" dtxInsert=\"slideshow\" href=\"/FT/Content/Companies/Stories/Live/PlainSlideshow.gallery.xml?uuid=49336a18-051c-11e3-98a0-002128161462\">" +
                "<DIHeadlineCopy/></a></p></body>";

        String processedSlideshow = "<body><p>Embedded Slideshow</p>" +
                "<p><a data-asset-type=\"slideshow\" data-embedded=\"true\" href=\"http://www.ft.com/cms/s/49336a18-051c-11e3-98a0-002128161462.html#slide0\"></a></p></body>";

        checkTransformation(slideshowFromMethode, processedSlideshow);
    }

    @Test
    public void shouldNotBarfOnTwoSlideshows() {
        String slideshowFromMethode = "<body><p>Embedded Slideshow</p>" +
                "<p><a type=\"slideshow\" dtxInsert=\"slideshow\" href=\"/FT/Content/Companies/Stories/Live/PlainSlideshow.gallery.xml?uuid=49336a18-051c-11e3-98a0-002128161462\">" +
                "<DIHeadlineCopy>One typical, bog-standard slideshow headline update 1</DIHeadlineCopy></a></p>" +
                "<p><a type=\"slideshow\" dtxInsert=\"slideshow\" href=\"/FT/Content/Companies/Stories/Live/PlainSlideshow.gallery.xml?uuid=49336a18-051c-11e3-98a0-001234567890\">" +
                "<DIHeadlineCopy>One typical, bog-standard slideshow headline update 2</DIHeadlineCopy></a></p></body>";

        String processedSlideshow = "<body><p>Embedded Slideshow</p>" +
                "<p><a data-asset-type=\"slideshow\" data-embedded=\"true\" href=\"http://www.ft.com/cms/s/49336a18-051c-11e3-98a0-002128161462.html#slide0\"></a></p>" +
                "<p><a data-asset-type=\"slideshow\" data-embedded=\"true\" href=\"http://www.ft.com/cms/s/49336a18-051c-11e3-98a0-001234567890.html#slide0\"></a></p>" +
                "</body>";

        checkTransformation(slideshowFromMethode, processedSlideshow);

    }
    
    @Test
    public void timelineShouldBeRetained() {
        String timelineFromMethode = "<body><p>Intro text</p>" +
        		"<timeline><timeline-header>The battle for Simandou</timeline-header>\n" +
        		"<timeline-credits>AFP, Bloomberg, Shawn Curry, Company handouts</timeline-credits>\n" +
        		"<timeline-sources>FT Research</timeline-sources>\n" +
        		"<timeline-byline>Tom Burgis, Callum Locke, Katie Carnie, Steve Bernard</timeline-byline>\n" +
        		"<timeline-item>\n<timeline-image fileref=\"/FT/Graphics/Online/Master_2048x1152/Martin/mas_Microsoft-Surface-tablet--566x318.jpg?uuid=213bb10c-71fe-11e2-8104-002128161462\" height=\"1152\" tmx=\"566 318 164 92\" width=\"2048\" xtransform=\" scale(0.2897527 0.2897527)\"></timeline-image>\n" +
        		"<timeline-date>1997-01-01 00:00:00</timeline-date>\n" +
        		"<timeline-title>1997</timeline-title>\n" +
        		"<timeline-body><p>Rio Tinto is granted rights to explore the Simandou deposit</p>\n</timeline-body>\n</timeline-item>\n" +
        		"</timeline></body>";
        
        String processedTimeline = "<body><p>Intro text</p>" +
                "<ft-timeline><timeline-header>The battle for Simandou</timeline-header>\n" +
                "<timeline-credits>AFP, Bloomberg, Shawn Curry, Company handouts</timeline-credits>\n" +
                "<timeline-sources>FT Research</timeline-sources>\n" +
                "<timeline-byline>Tom Burgis, Callum Locke, Katie Carnie, Steve Bernard</timeline-byline>\n" +
                "<timeline-item>\n<timeline-image><content data-embedded=\"true\" id=\"213bb10c-71fe-11e2-1f62-97bbf262bf2b\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content></timeline-image>\n" +
                "<timeline-date>1997-01-01 00:00:00</timeline-date>\n" +
                "<timeline-title>1997</timeline-title>\n" +
                "<timeline-body><p>Rio Tinto is granted rights to explore the Simandou deposit</p>\n</timeline-body>\n</timeline-item>" +
                "</ft-timeline></body>";
        
        checkTransformation(timelineFromMethode, processedTimeline);
    }


    @Test
    public void timelineShouldBeWrittenOutsideOfPtags() {
        String timelineFromMethode = "<body><p>Intro text</p><p>" +
                "<timeline><timeline-header>The battle for Simandou</timeline-header>\n" +
                "<timeline-credits>AFP, Bloomberg, Shawn Curry, Company handouts</timeline-credits>\n" +
                "<timeline-sources>FT Research</timeline-sources>\n" +
                "<timeline-byline>Tom Burgis, Callum Locke, Katie Carnie, Steve Bernard</timeline-byline>\n" +
                "<timeline-item>\n<timeline-image fileref=\"/FT/Graphics/Online/Master_2048x1152/Martin/mas_Microsoft-Surface-tablet--566x318.jpg?uuid=213bb10c-71fe-11e2-8104-002128161462\" height=\"1152\" tmx=\"566 318 164 92\" width=\"2048\" xtransform=\" scale(0.2897527 0.2897527)\"></timeline-image>\n" +
                "<timeline-date>1997-01-01 00:00:00</timeline-date>\n" +
                "<timeline-title>1997</timeline-title>\n" +
                "<timeline-body><p>Rio Tinto is granted rights to explore the Simandou deposit</p>\n</timeline-body>\n</timeline-item>\n" +
                "</timeline></p></body>";

        String processedTimeline = "<body><p>Intro text</p>" +
                "<ft-timeline><timeline-header>The battle for Simandou</timeline-header>\n" +
                "<timeline-credits>AFP, Bloomberg, Shawn Curry, Company handouts</timeline-credits>\n" +
                "<timeline-sources>FT Research</timeline-sources>\n" +
                "<timeline-byline>Tom Burgis, Callum Locke, Katie Carnie, Steve Bernard</timeline-byline>\n" +
                "<timeline-item>\n<timeline-image><content data-embedded=\"true\" id=\"213bb10c-71fe-11e2-1f62-97bbf262bf2b\" type=\"http://www.ft.com/ontology/content/ImageSet\"></content></timeline-image>\n" +
                "<timeline-date>1997-01-01 00:00:00</timeline-date>\n" +
                "<timeline-title>1997</timeline-title>\n" +
                "<timeline-body><p>Rio Tinto is granted rights to explore the Simandou deposit</p>\n</timeline-body>\n</timeline-item>" +
                "</ft-timeline></body>";

        checkTransformation(timelineFromMethode, processedTimeline);
    }


    @Test
    public void shouldRenameTagAndCloseOpenPtags() throws Exception {

    }

    @Test
     public void shouldProcessPodcastsCorrectly() {
        String podcastFromMethode = "<body><script type=\"text/javascript\" src=\"http://podcast.ft.com/embed.js\">\n" +
                "</script><script type=\"text/javascript\">/* <![CDATA[ */window.onload=function(){embedLink('podcast.ft.com','2463','18','lucy060115.mp3','Golden Flannel of the year award','Under Tim Cook’s leadership, Apple succumbed to drivel, says Lucy Kellaway','ep_2463','share_2463');}/* ]]> */\n" +
                "</script></body>";
        String processedPodcast = "<body><a data-asset-type=\"podcast\" data-embedded=\"true\" href=\"http://podcast.ft.com/p/2463\" title=\"Golden Flannel of the year award\"></a></body>";
        checkTransformation(podcastFromMethode, processedPodcast);
    }

    @Test
     public void shouldProcessMultiplePodcastsCorrectly() {
        String podcastFromMethode = "<body><script type=\"text/javascript\" src=\"http://podcast.ft.com/embed.js\">\n" +
                "</script><script type=\"text/javascript\">/* <![CDATA[ */window.onload=function(){embedLink('podcast.ft.com','2463','18','lucy060115.mp3','Golden Flannel of the year award','Under Tim Cook’s leadership, Apple succumbed to drivel, says Lucy Kellaway','ep_2463','share_2463');}/* ]]> */\n</script>" +
                "<script type=\"text/javascript\" src=\"http://podcast.ft.com/embed.js\">\n" +
                "</script><script type=\"text/javascript\">/* <![CDATA[ */window.onload=function(){embedLink('podcast.ft.com','2463','18','lucy060115.mp3','Golden Flannel of the year award 2','Under Tim Cook’s leadership, Apple succumbed to drivel, says Lucy Kellaway','ep_2463','share_2463');}/* ]]> */\n" +
                "</script></body>";
        String processedPodcast = "<body><a data-asset-type=\"podcast\" data-embedded=\"true\" href=\"http://podcast.ft.com/p/2463\" title=\"Golden Flannel of the year award\"></a>" +
				"<a data-asset-type=\"podcast\" data-embedded=\"true\" href=\"http://podcast.ft.com/p/2463\" title=\"Golden Flannel of the year award 2\"></a></body>";
        checkTransformation(podcastFromMethode, processedPodcast);
    }

    @Test
    public void brightcove_shouldProcessVideoTagCorrectly() {
        String videoTextfromMethode = "<body>" +
                "<videoPlayer videoID=\"3920663836001\">" +
                "<web-inline-picture id=\"U2113113643377jlC\" width=\"150\" fileref=\"/FT/Graphics/Online/Z_Undefined/FT-video-story.jpg?uuid=91b39ae8-ccff-11e1-92c1-00144feabdc0\" tmx=\"150 100 150 100\"/>\n" +
                "</videoPlayer>" +
                "</body>";
        String processedVideoText = "<body><a href=\"http://video.ft.com/3920663836001\" data-embedded=\"true\" data-asset-type=\"video\"></a></body>";
        checkTransformation(videoTextfromMethode, processedVideoText);
    }

    @Test
    public void brightcove_shouldFallbackWhenVideoTagErrors() {
        String videoTextfromMethode = "<body><videoPlayer><web-inline-picture id=\"U2113113643377jlC\" width=\"150\" fileref=\"/FT/Graphics/Online/Z_Undefined/FT-video-story.jpg?uuid=91b39ae8-ccff-11e1-92c1-00144feabdc0\" tmx=\"150 100 150 100\"/>\n" +
                "</videoPlayer></body>";
        String processedVideoText = "<body></body>";
        checkTransformation(videoTextfromMethode, processedVideoText);
    }

    @Test
    public void brightcove_shouldFallbackWhenVideoTagErrors2() {
        String videoTextfromMethode = "<body><videoPlayer videoID=\"\"><web-inline-picture id=\"U2113113643377jlC\" width=\"150\" fileref=\"/FT/Graphics/Online/Z_Undefined/FT-video-story.jpg?uuid=91b39ae8-ccff-11e1-92c1-00144feabdc0\" tmx=\"150 100 150 100\"/>\n" +
                "</videoPlayer></body>";
        String processedVideoText = "<body></body>";
        checkTransformation(videoTextfromMethode, processedVideoText);
    }


    @Test
    public void shouldProcessVimeoTagCorrectly() {
        String videoTextfromMethode = "<body><p align=\"left\" channel=\"FTcom\">Vimeo Video<iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"http://player.vimeo.com/video/77761436\" width=\"600\"></iframe></p></body>";
        String processedVideoText = "<body><p>Vimeo Video<a href=\"https://www.vimeo.com/77761436\" data-embedded=\"true\" data-asset-type=\"video\"></a></p></body>";
        when(videoMatcher.filterVideo(any(RichContentItem.class))).thenReturn(exampleVimeoVideo);
        checkTransformation(videoTextfromMethode, processedVideoText);
    }

    @Test
    public void shouldProcessVimeoTagWithNoProtocolCorrectly() {
        String videoTextfromMethode = "<body><p align=\"left\" channel=\"FTcom\">Vimeo Video<iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"//player.vimeo.com/video/77761436\" width=\"600\"></iframe></p></body>";
        String processedVideoText = "<body><p>Vimeo Video<a href=\"https://www.vimeo.com/77761436\" data-embedded=\"true\" data-asset-type=\"video\"></a></p></body>";
        when(videoMatcher.filterVideo(any(RichContentItem.class))).thenReturn(exampleVimeoVideo);
        checkTransformation(videoTextfromMethode, processedVideoText);
    }

    @Test
    public void shouldProcessYouTubeVideoCorrectly_withPChannel() {
        String videoTextfromMethode = "<body><p align=\"left\" channel=\"FTcom\">Youtube Video<iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"http://www.youtube.com/embed/77761436\" width=\"600\"></iframe></p></body>";
        String processedVideoText = "<body><p>Youtube Video<a href=\"https://www.youtube.com/watch?v=77761436\" data-embedded=\"true\" data-asset-type=\"video\"></a></p></body>";
        when(videoMatcher.filterVideo(any(RichContentItem.class))).thenReturn(exampleYouTubeVideo);
        checkTransformation(videoTextfromMethode, processedVideoText);
    }

    @Test
    public void shouldProcessYouTubeVideoCorrectly_withNoPChannel() {
        String videoTextfromMethode = "<body><p>Youtube Video<iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"http://www.youtube.com/embed/77761436\" width=\"600\"></iframe></p></body>";
        String processedVideoText = "<body><p>Youtube Video<a href=\"https://www.youtube.com/watch?v=77761436\" data-embedded=\"true\" data-asset-type=\"video\"></a></p></body>";
        when(videoMatcher.filterVideo(any(RichContentItem.class))).thenReturn(exampleYouTubeVideo);
        checkTransformation(videoTextfromMethode, processedVideoText);
    }

    @Test
    public void shouldProcessYouTubeVideoWithHttpsCorrectly() {
        String videoTextfromMethode = "<body><p align=\"left\" channel=\"FTcom\">Youtube Video<iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"https://www.youtube.com/embed/77761436\" width=\"600\"></iframe></p></body>";
        String processedVideoText = "<body><p>Youtube Video<a href=\"https://www.youtube.com/watch?v=77761436\" data-embedded=\"true\" data-asset-type=\"video\"></a></p></body>";
        when(videoMatcher.filterVideo(any(RichContentItem.class))).thenReturn(exampleYouTubeVideo);
        checkTransformation(videoTextfromMethode, processedVideoText);
    }


    @Test
    public void shouldNotProcessOtherIframes() {
        String videoTextfromMethode = "<body><p align=\"left\" channel=\"FTcom\"><iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"http://www.bbc.co.uk/video/77761436\" width=\"600\"></iframe></p></body>";
        String processedVideoText = "<body></body>";
        checkTransformation(videoTextfromMethode, processedVideoText);
    }


    @Test
    public void shouldNotProcessOtherIframes_withTextInbetween() {
        String videoTextfromMethode = "<body><p channel=\"FTcom\">Video<iframe height=\"245\" frameborder=\"0\" allowfullscreen=\"\" src=\"http://www.notyoutube.com/junk\" width=\"600\"></iframe></p></body>";
        String processedVideoText = "<body><p>Video</p></body>";
        checkTransformation(videoTextfromMethode, processedVideoText);
    }

    @Test
    public void shouldNotProcessOtherScriptTags() {
        String podcastFromMethode = "<body><script type=\"text/javascript\">/* <![CDATA[ */window.onload=function(){alert('Something!')}/* ]]> */\n" +
                "\"</script></body>";
        String processedPodcast = "<body></body>";
        checkTransformation(podcastFromMethode, processedPodcast);
    }

    @Test
    public void shouldNotProcessOtherScriptTags2() {
        String podcastFromMethode = "<body><script type=\"text/javascript\" src=\"http://someStyleSheet\"></script></body>";
        String processedPodcast = "<body></body>";
        checkTransformation(podcastFromMethode, processedPodcast);
    }

    @Test
    public void removeExcessSpacesBetweenParagraphBlocks() {
        String expectedSentence = String.format("<body><p>This</p>\n<p>is</p>\n<p>a test</p></body>");
        checkTransformation("<body><p>This</p>\n\n\n\n<p>is</p>\n<p>a test</p></body>",expectedSentence);
    }

    @Test
    public void removeEmptyParagraphs() {
        String expectedSentence = String.format("<body><p>This one is not empty</p>" +
                "<p>This should not be removed <br/></p></body>");
        checkTransformation("<body><p>This one is not empty</p><p>This should not be removed <br/></p><p><br/></p><p> <br/> </p></body>",expectedSentence);
    }

	@Test
	public void shouldTransformSubheadIntoH3ClassFtSubhead() {
		String subheadFromMethode = "<body><p>his is some normal text.</p><p>More text</p><subhead>This is a subhead.</subhead><p>This is some more normal text.</p><subhead>This is another subhead.</subhead></body>";
		String processedSubhead = "<body><p>his is some normal text.</p><p>More text</p><h3 class=\"ft-subhead\">This is a subhead.</h3><p>This is some more normal text.</p><h3 class=\"ft-subhead\">This is another subhead.</h3></body>";
		checkTransformation(subheadFromMethode, processedSubhead);
	}

    @Test
    public void shouldRemoveElementsAndContentWithChannelAttributeThatAreStrikeouts() {
        String contentWithStrikeouts = "<body><b channel=\"Financial Times\">Should be removed</b><b>Should Stay</b><p channel=\"!Strikeout\">Should be removed</p><p>Text inside normal p tag should remain</p></body>";
        String transformedContent = "<body><strong>Should Stay</strong><p>Text inside normal p tag should remain</p></body>";
        checkTransformation(contentWithStrikeouts, transformedContent);
    }

    @Test
    public void shouldRetainElementsAndContentWithChannelAttributesThatAreNotStrikeouts() {
        String contentWithStrikeouts = "<body><p channel=\"FTcom\">Random Text<iframe src=\"http://www.youtube.com/embed/77761436\"></iframe></p><b channel=\"!Financial Times\">Not Financial Times</b></body>";
        String transformedContent = "<body><p>Random Text<a href=\"https://www.youtube.com/watch?v=77761436\" data-asset-type=\"video\" data-embedded=\"true\"/></p><strong>Not Financial Times</strong></body>";
        when(videoMatcher.filterVideo(any(RichContentItem.class))).thenReturn(exampleYouTubeVideo);
        checkTransformation(contentWithStrikeouts, transformedContent);
    }

    @Test
    public void testShouldRetainImgTagAndValidAttributes() throws Exception {
        String contentExternalImage = "<body><img src=\"someImage.jpg\" alt=\"someAltText\" width=\"200\" height=\"200\" align=\"left\"/></body>";
        String transformedContent = "<body><img src=\"someImage.jpg\" alt=\"someAltText\" width=\"200\" height=\"200\"/></body>";
        checkTransformation(contentExternalImage, transformedContent);

    }

    private void checkTransformation(String originalBody, String expectedTransformedBody) {
        String actualTransformedBody = bodyTransformer.transform(originalBody, TRANSACTION_ID);

        System.out.println("TRANSFORMED BODY:\n" + actualTransformedBody);

        assertThat(actualTransformedBody, is(identicalXmlTo(expectedTransformedBody)));
    }

    private void checkTransformationToEmpty(String originalBody) {
        String actualTransformedBody = bodyTransformer.transform(originalBody, TRANSACTION_ID);
        assertThat(actualTransformedBody, is(""));
    }

}
