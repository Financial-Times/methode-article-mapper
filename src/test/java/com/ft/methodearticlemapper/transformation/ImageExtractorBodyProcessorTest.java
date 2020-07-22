package com.ft.methodearticlemapper.transformation;

import static com.ft.methodetesting.xml.XmlMatcher.identicalXmlTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.ft.bodyprocessing.BodyProcessingContext;
import org.junit.Before;
import org.junit.Test;

public class ImageExtractorBodyProcessorTest {

  private BodyProcessingContext bodyProcessingContext;

  private ImageExtractorBodyProcessor imageExtractorBodyProcessor;

  @Before
  public void setUp() {
    imageExtractorBodyProcessor = new ImageExtractorBodyProcessor();
    bodyProcessingContext = new BodyProcessingContext() {};
  }

  @Test
  public void testProcess_ExtractImageSet() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><image-set id=\"U32503569610592JkB\"><image-small/><image-medium/><image-large/></image-set>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>"
            + "<image-set id=\"U32503569610592JkB\"><image-small/><image-medium/><image-large/></image-set><p>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractWebMaster() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><web-master id=\"U1090748823904auH\"/>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>"
            + "<web-master id=\"U1090748823904auH\"/><p>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractWebInlinePicture() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><web-inline-picture id=\"U1090748823904auH\"/>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>"
            + "<web-inline-picture id=\"U1090748823904auH\"/><p>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractImg() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><img src=\"img source\"/>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>"
            + "<img src=\"img source\"/><p>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractImgInsideATag() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><a href=\"\"><img src=\"img source\"/></a>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>"
            + "<img src=\"img source\"/><p>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_RemoveImgWithoutSrcInsideParagraph() {
    String body =
        "<body><p>Lorem ipsum</p>" + "<p><img/>Lorem ipsum</p>" + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>" + "<p>Lorem ipsum</p>" + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_RemoveImgWithEmptySrcInsideParagraph() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><img src=\"\"/>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>" + "<p>Lorem ipsum</p>" + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_RemoveImgWithoutSrcOutsideParagraph() {
    String body = "<body><p>Lorem ipsum</p>" + "<img/>" + "<p>Lorem ipsum</p></body>";
    String expected = "<body><p>Lorem ipsum</p>" + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_RemoveImgWithEmptySrcOutsideParagraph() {
    String body = "<body><p>Lorem ipsum</p>" + "<img src=\"\"/>" + "<p>Lorem ipsum</p></body>";
    String expected = "<body><p>Lorem ipsum</p>" + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_RemoveImgWithoutSrcInsideATag() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><a href=\"\"><img/></a>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>" + "<p>Lorem ipsum</p>" + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_RemoveImgWithEmptySrcInsideATag() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p><a href=\"\"><img src=\"\"/></a>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>" + "<p>Lorem ipsum</p>" + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractMultipleImgTagsInsideATagWithoutRemovingATag() {
    String body =
        "<body>"
            + "<p><a href=\"\"><img src=\"source\"/><img src=\"source\"/>Lorem ipsum</a></p>"
            + "</body>";
    String expected =
        "<body>"
            + "<img src=\"source\"/><img src=\"source\"/>"
            + "<p><a href=\"\">Lorem ipsum</a></p>"
            + "</body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractMultipleImgTagsInsideATagWithATagRemoval() {
    String body =
        "<body>" + "<p><a href=\"\"><img src=\"source\"/><img src=\"source\"/></a></p>" + "</body>";
    String expected = "<body>" + "<img src=\"source\"/><img src=\"source\"/><p/>" + "</body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }

  @Test
  public void testProcess_ExtractAllImageTypes() {
    String body =
        "<body><p>Lorem ipsum</p>"
            + "<p>"
            + "<image-set id=\"U32503569610592JkB\"><image-small/><image-medium/><image-large/></image-set>"
            + "<web-master id=\"U1090748823904auH\"/>"
            + "<web-inline-picture id=\"U1090748823904auH\"/>"
            + "<img src=\"img source\"/>"
            + "<a href=\"\"><img src=\"img source\"/></a>"
            + "Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";
    String expected =
        "<body><p>Lorem ipsum</p>"
            + "<image-set id=\"U32503569610592JkB\"><image-small/><image-medium/><image-large/></image-set>"
            + "<web-master id=\"U1090748823904auH\"/>"
            + "<web-inline-picture id=\"U1090748823904auH\"/>"
            + "<img src=\"img source\"/>"
            + "<img src=\"img source\"/>"
            + "<p>Lorem ipsum</p>"
            + "<p>Lorem ipsum</p></body>";

    String result = imageExtractorBodyProcessor.process(body, bodyProcessingContext);

    assertThat(result, is(identicalXmlTo(expected)));
  }
}
