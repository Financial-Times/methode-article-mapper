package com.ft.methodearticletransformer.transformation;

import static com.ft.methodetesting.xml.XmlMatcher.identicalXmlTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

@RunWith(MockitoJUnitRunner.class)
public class BodyProcessingFieldTransformerFactoryTest {
	
	@Rule
    public ExpectedException expectedException = ExpectedException.none();

    private FieldTransformer bodyTransformer;

    @Mock private MethodeFileService methodeFileService;
    @Mock private ResilientClient semanticStoreContentReaderClient;
    @Mock private WebResource webResource;
    @Mock private Builder builder;
    @Mock private ClientResponse clientResponse;
    @Mock private InputStream inputStream;
    
	private static final String TRANSACTION_ID = "tid_test";

	@Before
    public void setup() {
        when(methodeFileService.assetTypes(anySet(), anyString())).thenReturn(Collections.<String, EomAssetType>emptyMap());
        bodyTransformer = new BodyProcessingFieldTransformerFactory(methodeFileService, semanticStoreContentReaderClient).newInstance();
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
                "<p><br/></p><p><img src=\"something.jpg\"/><br/> </p><p>Did she protest too much? " +
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
        final String expectedTransformedBody = "<body>\n<p>In Paris in the late 1940s, a publicity-hungry gallerist invited a young, beautiful, unknown Lebanese artist to pose for a photograph " +
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
                "reading about art history in “Les Peintres Célèbres”, or occupied with housework in “Chores”.</p>\n</body>";

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
    public void pullQuotesShouldBeReplacedWithAppopriateTags() {
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
                "<pull-quote-text>It suits the extremists to encourage healthy eating.</pull-quote-text>" +
                "<pull-quote-source>source1</pull-quote-source>" +
                "</pull-quote></body>";

        checkTransformation(pullQuoteFromMethode, processedPullQuote);
    }

    @Test
    public void bigNumbersShouldBeReplacedWithAppopriateTags() {
        String bigNumberFromMethode = "<body><p>patelka</p><promo-box class=\"numbers-component\" align=\"right\">" +
                "<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-headline><p class=\"title\">£350m</p>\n" +
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
                "<big-number-headline>£350m</big-number-headline>" +
                "<big-number-intro>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</big-number-intro>" +
                "</big-number></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void promoBoxWithPromoLinkIsNotBigNumber() {
        String bigNumberFromMethode = "<body><p>patelka</p><promo-box class=\"numbers-component\" align=\"right\">" +
                "<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-headline><p class=\"title\">£350m</p>\n" +
                "</promo-headline>\n" +
                "<promo-link>http://www.test.com</promo-link>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</promo-box></body>";

        String processedBigNumber = "<body><p>patelka</p></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void promoBoxWithEmptyPromoLinkIsBigNumber() {
        String bigNumberFromMethode = "<body><p>patelka</p><promo-box class=\"numbers-component\" align=\"right\">" +
                "<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-headline><p class=\"title\">£350m</p>\n" +
                "</promo-headline>\n" +
                "<promo-link></promo-link>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>\n" +
                "</promo-box></body>";

        String processedBigNumber = "<body><p>patelka</p><big-number>" +
                "<big-number-headline>£350m</big-number-headline>" +
                "<big-number-intro>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</big-number-intro>" +
                "</big-number></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void promoBoxWithPromoImageIsNotBigNumber() {
        String bigNumberFromMethode = "<body><p>patelka</p><p><promo-box align=\"left\">&lt;<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td>" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-headline><p>HEADLINE TEXT</p>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-image fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/Copy%20of%20Copy%20of%20secondaryimageccd1.jpg?uuid=220972be-972b-11e4-be20-002128161462\" tmx=\"167 96 167 96\"/>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>PROMOBOX BODY</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>&gt;</promo-box></p></body>";

        String processedBigNumber = "<body><p>patelka</p></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void promoBoxWithPromoTitleIsNotBigNumber() {
        String bigNumberFromMethode = "<body><p>patelka</p><p><promo-box align=\"left\">&lt;<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-title><p>PROMOBOX INDEPTH</p>\n" +
                "</promo-title>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-headline><p>HEADLINE TEXT</p>\n" +
                "</promo-headline>\n" +
                "</td>\n" +
                "</tr>\n" +
                "<tr><td><promo-intro><p>PROMOBOX BODY</p>\n" +
                "</promo-intro>\n" +
                "</td>\n" +
                "</tr>\n" +
                "</table>&gt;</promo-box></p></body>";

        String processedBigNumber = "<body><p>patelka</p></body>";

        checkTransformation(bigNumberFromMethode, processedBigNumber);
    }

    @Test
    public void promoBoxWithPromoTitleThatIsEmptyIsBigNumber() {
        String bigNumberFromMethode = "<body><p>patelka</p><p><promo-box align=\"left\">&lt;<table width=\"170px\" align=\"left\" cellpadding=\"6px\"><tr><td><promo-title>" +
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

        String processedBigNumber = "<body><p>patelka</p><p><big-number>" +
                "<big-number-headline>£350m</big-number-headline>" +
                "<big-number-intro>Cost of the rights expected to increase by one-third — or about £350m a year — although some anticipate inflation of up to 70%</big-number-intro>" +
                "</big-number></p></body>";

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
    public void slideshowShouldBeConvertedToPointToSlideshowOnWebsite() {
        String slideshowFromMethode = "<body><p>Embedded Slideshow</p>" +
                "<p><a type=\"slideshow\" dtxInsert=\"slideshow\" href=\"/FT/Content/Companies/Stories/Live/PlainSlideshow.gallery.xml?uuid=49336a18-051c-11e3-98a0-002128161462\">" + 
                "<DIHeadlineCopy>One typical, bog-standard slideshow headline update 2</DIHeadlineCopy></a></p></body>";
        
        String processedSlideshow = "<body><p>Embedded Slideshow</p>" +
        		"<p><a href=\"http://www.ft.com/cms/s/49336a18-051c-11e3-98a0-002128161462.html#slide0\"></a></p></body>";
        
        checkTransformation(slideshowFromMethode, processedSlideshow);
                
    }
    
    @Test
    public void timelineShouldBeRetained() {
        String timelineFromMethode = "<body><p>Intro text</p>" +
        		"<timeline><timeline-header>The battle for Simandou</timeline-header>\r\n" +
        		"<timeline-credits>AFP, Bloomberg, Shawn Curry, Company handouts</timeline-credits>" +
        		"\r\nFT Research\r\n" +
        		"<timeline-byline>Tom Burgis, Callum Locke, Katie Carnie, Steve Bernard</timeline-byline>\r\n" +
        		"<timeline-item>\r\n<timeline-image height=\"1152\" width=\"2048\"/>\r\n" +
        		"<timeline-date>1997-01-01 00:00:00</timeline-date>\r\n" +
        		"<timeline-title>1997</timeline-title>\r\n" +
        		"<timeline-body><p>Rio Tinto is granted rights to explore the Simandou deposit</p>\r\n</timeline-body>\r\n</timeline-item>\r\n" +
        		"</timeline></body>";
        
        String processedTimeline = "<body><p>Intro text</p>" +
                "<timeline><timeline-header>The battle for Simandou</timeline-header>\r\n" +
                "<timeline-credits>AFP, Bloomberg, Shawn Curry, Company handouts</timeline-credits>" +
                "\r\nFT Research\r\n" +
                "<timeline-byline>Tom Burgis, Callum Locke, Katie Carnie, Steve Bernard</timeline-byline>\r\n" +
                "<timeline-item>\r\n<timeline-image height=\"1152\" width=\"2048\"/>\r\n" +
                "<timeline-date>1997-01-01 00:00:00</timeline-date>\r\n" +
                "<timeline-title>1997</timeline-title>\r\n" +
                "<timeline-body><p>Rio Tinto is granted rights to explore the Simandou deposit</p>\r\n</timeline-body>\r\n</timeline-item>\r\n" +
                "</timeline></body>";
        
        checkTransformation(timelineFromMethode, processedTimeline);
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
