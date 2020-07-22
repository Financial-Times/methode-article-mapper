package com.ft.methodearticlemapper.transformation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.ft.bodyprocessing.BodyProcessingContext;
import org.junit.Before;
import org.junit.Test;

public class RemoveIfExactMatchBodyProcessorTest {
  private static final BodyProcessingContext bodyProcessingContext = null;

  private RemoveIfExactMatchBodyProcessor bodyProcessor;

  @Before
  public void setup() {
    bodyProcessor = new RemoveIfExactMatchBodyProcessor("By");
  }

  @Test
  public void shouldRemoveIfMatchesExactly() {
    String result = bodyProcessor.process("By", bodyProcessingContext);
    assertThat("whitespace not trimmed", result, is(equalTo("")));
  }

  @Test
  public void shouldNotRemoveIfNotExactMatch() {
    String result = bodyProcessor.process("ByBy", bodyProcessingContext);
    assertThat("whitespace not trimmed", result, is(equalTo("ByBy")));
  }

  @Test
  public void shouldReturnNullForNullBody() {
    String result = bodyProcessor.process(null, bodyProcessingContext);
    assertThat("whitespace not trimmed", result, is(equalTo(null)));
  }
}
