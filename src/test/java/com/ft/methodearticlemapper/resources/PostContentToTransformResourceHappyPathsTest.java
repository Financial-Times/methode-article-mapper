package com.ft.methodearticlemapper.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ft.methodearticlemapper.configuration.PropertySource;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

/**
 * Tests that both article preview and published article get transformed by the POST endpoint.
 *
 * <p>Created by julia.fernee on 28/01/2016.
 */
public class PostContentToTransformResourceHappyPathsTest {

  private static final String TRANSACTION_ID = "tid_test";

  private EomFileProcessor eomFileProcessor = mock(EomFileProcessor.class);
  private EomFile eomFile = mock(EomFile.class);
  private String uuid = UUID.randomUUID().toString();

  /* Class upder test. */
  private PostContentToTransformResource postContentToTransformResource =
      new PostContentToTransformResource(
          eomFileProcessor,
          PropertySource.fromTransaction,
          PropertySource.fromTransaction,
          "publishReference");

  @Before
  public void preconditions() throws Exception {
    when(eomFile.getUuid()).thenReturn(uuid);
    MDC.put("transaction_id", "transaction_id=" + TRANSACTION_ID);
  }

  /**
   * Tests that an unpublished article preview request results in processing it as a PREVIEW
   * article.
   */
  @Test
  public void previewProcessedOk() {
    postContentToTransformResource.map(eomFile, true, null);
    verify(eomFileProcessor, times(1))
        .process(eq(eomFile), eq(TransformationMode.PREVIEW), eq(TRANSACTION_ID), any());
  }

  /** Tests that an published article request results in processing it as a PUBLISHED article. */
  @Test
  public void publicationProcessedOk() {
    postContentToTransformResource.map(eomFile, false, null);
    verify(eomFileProcessor, times(1))
        .process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any());
  }
}
