package com.ft.methodearticlemapper.transformation;

import static com.ft.methodetesting.xml.XmlMatcher.identicalXmlTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.ft.bodyprocessing.BodyProcessingContext;
import org.junit.Before;
import org.junit.Test;

public class PromoBoxExtractorBodyProcessorTest {

  private BodyProcessingContext bodyProcessingContext;
  private PromoBoxExtractorBodyProcessor promoBoxExtractorBodyProcessor;

  @Before
  public void setUp() {
    promoBoxExtractorBodyProcessor = new PromoBoxExtractorBodyProcessor();
    bodyProcessingContext = new BodyProcessingContext() {};
  }

  @Test
  public void testProcess_ExtractPromoBoxWhenParagraphChild() {
    String body =
        "<body>"
            + "<p>"
            + "<promo-box align=\"left\">&lt;"
            + "<table width=\"170px\" align=\"left\" cellpadding=\"6px\">"
            + "<tr>"
            + "<td>"
            + "<promo-title>"
            + "<p>In depth</p>"
            + "</promo-title>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-headline>"
            + "<p>"
            + "<a href=\"http://www.ft.com/intl/indepth/climatechange\" title=\"Climate change in depth - FT.com\">Climate change</a>"
            + "</p>"
            + "</promo-headline>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-image fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/10/SEC_POWE.jpg?uuid=5e542eaa-f042-11e0-96d2-00144feab49a\" tmx=\"167 96 167 96\"/>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-intro>"
            + "<p>The latest news and analysis on the world’s changing climate and the political moves afoot to tackle the problem</p>"
            + "</promo-intro>"
            + "</td>"
            + "</tr>"
            + "</table>&gt;</promo-box>"
            + "Who wins and who loses?"
            + "</p>"
            + "</body>";
    String expected =
        "<body>"
            + "<promo-box align=\"left\">&lt;"
            + "<table width=\"170px\" align=\"left\" cellpadding=\"6px\">"
            + "<tr>"
            + "<td>"
            + "<promo-title>"
            + "<p>In depth</p>"
            + "</promo-title>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-headline>"
            + "<p>"
            + "<a href=\"http://www.ft.com/intl/indepth/climatechange\" title=\"Climate change in depth - FT.com\">Climate change</a>"
            + "</p>"
            + "</promo-headline>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-image fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/10/SEC_POWE.jpg?uuid=5e542eaa-f042-11e0-96d2-00144feab49a\" tmx=\"167 96 167 96\"/>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-intro>"
            + "<p>The latest news and analysis on the world’s changing climate and the political moves afoot to tackle the problem</p>"
            + "</promo-intro>"
            + "</td>"
            + "</tr>"
            + "</table>&gt;</promo-box>"
            + "<p>Who wins and who loses?"
            + "</p>"
            + "</body>";

    String result = promoBoxExtractorBodyProcessor.process(body, bodyProcessingContext);
    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractPromoBoxWhenParagraphDescendant() {
    String body =
        "<body>"
            + "<p><b>"
            + "<promo-box align=\"left\">&lt;"
            + "<table width=\"170px\" align=\"left\" cellpadding=\"6px\">"
            + "<tr>"
            + "<td>"
            + "<promo-title>"
            + "<p>In depth</p>"
            + "</promo-title>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-headline>"
            + "<p>"
            + "<a href=\"http://www.ft.com/intl/indepth/climatechange\" title=\"Climate change in depth - FT.com\">Climate change</a>"
            + "</p>"
            + "</promo-headline>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-image fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/10/SEC_POWE.jpg?uuid=5e542eaa-f042-11e0-96d2-00144feab49a\" tmx=\"167 96 167 96\"/>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-intro>"
            + "<p>The latest news and analysis on the world’s changing climate and the political moves afoot to tackle the problem</p>"
            + "</promo-intro>"
            + "</td>"
            + "</tr>"
            + "</table>&gt;</promo-box>"
            + "Who wins and who loses?"
            + "</b></p>"
            + "</body>";
    String expected =
        "<body>"
            + "<promo-box align=\"left\">&lt;"
            + "<table width=\"170px\" align=\"left\" cellpadding=\"6px\">"
            + "<tr>"
            + "<td>"
            + "<promo-title>"
            + "<p>In depth</p>"
            + "</promo-title>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-headline>"
            + "<p>"
            + "<a href=\"http://www.ft.com/intl/indepth/climatechange\" title=\"Climate change in depth - FT.com\">Climate change</a>"
            + "</p>"
            + "</promo-headline>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-image fileref=\"/FT/Graphics/Online/Secondary_%26_Triplet_167x96/2011/10/SEC_POWE.jpg?uuid=5e542eaa-f042-11e0-96d2-00144feab49a\" tmx=\"167 96 167 96\"/>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td>"
            + "<promo-intro>"
            + "<p>The latest news and analysis on the world’s changing climate and the political moves afoot to tackle the problem</p>"
            + "</promo-intro>"
            + "</td>"
            + "</tr>"
            + "</table>&gt;</promo-box>"
            + "<p><b>Who wins and who loses?</b>"
            + "</p>"
            + "</body>";

    String result = promoBoxExtractorBodyProcessor.process(body, bodyProcessingContext);
    System.out.println(result);
    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractMultiplePromoBoxElements() {
    String body =
        "<body>"
            + "<p>Text before first promoBox"
            + "<promo-box>First promoBox</promo-box>"
            + "Text after first promoBox"
            + "<promo-box>Second promoBox</promo-box>"
            + "Text after second promoBox"
            + "</p>"
            + "</body>";
    String expected =
        "<body>"
            + "<promo-box>First promoBox</promo-box>"
            + "<promo-box>Second promoBox</promo-box>"
            + "<p>Text before first promoBox"
            + "Text after first promoBox"
            + "Text after second promoBox"
            + "</p>"
            + "</body>";

    String result = promoBoxExtractorBodyProcessor.process(body, bodyProcessingContext);
    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractPromoBoxNestedInsideMultipleParagraphs() {
    String body = "<body>" + "<p><p>" + "<promo-box>promobBox</promo-box>" + "</p></p>" + "</body>";
    String expected =
        "<body>" + "<promo-box>promobBox</promo-box>" + "<p><p>" + "</p></p>" + "</body>";

    String result = promoBoxExtractorBodyProcessor.process(body, bodyProcessingContext);
    assertThat(result, is(identicalXmlTo(expected)));
  }
}
