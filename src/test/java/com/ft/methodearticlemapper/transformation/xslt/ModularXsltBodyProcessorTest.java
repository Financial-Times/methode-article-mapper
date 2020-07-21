package com.ft.methodearticlemapper.transformation.xslt;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.ft.bodyprocessing.DefaultTransactionIdBodyProcessingContext;
import org.junit.Test;

public class ModularXsltBodyProcessorTest {

  public static final String REPLACE_B_TEMPLATE =
      "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
          + "  <xsl:template match=\"b\">\n"
          + "    <%s>\n"
          + "      <xsl:apply-templates select=\"@*|node()\"/>\n"
          + "    </%s>\n"
          + "  </xsl:template>\n"
          + "</xsl:stylesheet>";

  public static final XsltFile CORRECT_B_TAG =
      new XsltFile("correctBTag", String.format(REPLACE_B_TEMPLATE, "strong", "strong"));

  public static final XsltFile HUGE_B_TAG =
      new XsltFile("hugeBTag", String.format(REPLACE_B_TEMPLATE, "HUGE", "HUGE"));

  public static final XsltFile WACKY_B_TAG =
      new XsltFile("wackyBTag", String.format(REPLACE_B_TEMPLATE, "wacky", "wacky"));

  public static final String WHITESPACE = "\t" + System.lineSeparator() + " ";

  @Test
  public void shouldNotAffectWhitespaceBetweenElementsByDefault() {
    ModularXsltBodyProcessor identityTransformer = new ModularXsltBodyProcessor();

    String exampleDoc = "<body><p>Some</p>" + WHITESPACE + "<p>Text</p></body>";

    String result =
        identityTransformer.process(
            exampleDoc, new DefaultTransactionIdBodyProcessingContext("test"));

    assertThat(result, containsString(WHITESPACE));
  }

  @Test
  public void shouldPassThroughTextByDefault() {
    ModularXsltBodyProcessor identityTransformer = new ModularXsltBodyProcessor();

    String exampleDoc = "<body><p>Some Text</p></body>";

    String result =
        identityTransformer.process(
            exampleDoc, new DefaultTransactionIdBodyProcessingContext("test"));

    assertThat(result, containsString("Some Text"));
  }

  @Test
  public void shouldPassThroughMarkupByDefault() {
    ModularXsltBodyProcessor identityTransformer = new ModularXsltBodyProcessor();

    String exampleDoc = "<body><img/></body>";

    String result =
        identityTransformer.process(
            exampleDoc, new DefaultTransactionIdBodyProcessingContext("test"));

    assertThat(result, equalTo(exampleDoc));
  }

  @Test
  public void shouldCombineAdditionalXsltAndKeepDefaultRules() {

    ModularXsltBodyProcessor bTagTransformer = new ModularXsltBodyProcessor(CORRECT_B_TAG);

    String exampleDoc = "<body><b>BOLD</b><img/>" + WHITESPACE + "<p>Some Text</p></body>";
    String expectedDoc =
        "<body><strong>BOLD</strong><img/>" + WHITESPACE + "<p>Some Text</p></body>";

    String result =
        bTagTransformer.process(exampleDoc, new DefaultTransactionIdBodyProcessingContext("test"));

    assertThat(result, equalTo(expectedDoc));
  }

  @Test
  public void shouldCombineMultipleXsltAndTakeMatchingTemplateListedFirst() {

    ModularXsltBodyProcessor bTagTransformer =
        new ModularXsltBodyProcessor(WACKY_B_TAG, HUGE_B_TAG, CORRECT_B_TAG);

    String exampleDoc = "<body><b>BOLD</b></body>";
    String expectedDoc = "<body><strong>BOLD</strong></body>";

    String result =
        bTagTransformer.process(exampleDoc, new DefaultTransactionIdBodyProcessingContext("test"));

    assertThat(result, equalTo(expectedDoc));
  }
}
