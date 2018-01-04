package com.ft.methodearticlemapper.resources;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests that both article preview and published article get transformed by the POST endpoint.
 *
 * Created by julia.fernee on 28/01/2016.
 */
public class PostContentToTransformResourceHappyPathsTest {

    private static final String TRANSACTION_ID = "tid_test";

    private HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private EomFileProcessor eomFileProcessor = mock(EomFileProcessor.class);
    private EomFile eomFile = mock(EomFile.class);
    private String uuid = UUID.randomUUID().toString();

    /* Class upder test. */
    private PostContentToTransformResource postContentToTransformResource = new PostContentToTransformResource(eomFileProcessor);

    @Before
    public void preconditions() throws Exception {
        when(eomFile.getUuid()).thenReturn(uuid);
        when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
    }

    /**
     * Tests that an unpublished article preview request results in processing it as a PREVIEW article.
     */
    @Test
    public void previewProcessedOk() {
        postContentToTransformResource.map(eomFile, true, null, httpHeaders);
        verify(eomFileProcessor, times(1)).process(eq(eomFile), eq(TransformationMode.PREVIEW), eq(TRANSACTION_ID), any());
    }

    /**
     * Tests that an published article request results in processing it as a PUBLISHED article.
     */
    @Test
    public void publicationProcessedOk() {
        postContentToTransformResource.map(eomFile, false, null, httpHeaders);
        verify(eomFileProcessor, times(1)).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any());
    }

}
