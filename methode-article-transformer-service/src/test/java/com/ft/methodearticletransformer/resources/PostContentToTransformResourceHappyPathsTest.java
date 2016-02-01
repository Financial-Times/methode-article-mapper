package com.ft.methodearticletransformer.resources;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that both article preview and published article get transformed by the POST endpoint.
 *
 * Created by julia.fernee on 28/01/2016.
 */
public class PostContentToTransformResourceHappyPathsTest {

    private static final String TRANSACTION_ID = "tid_test";

    private HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private EomFileProcessorForContentStore eomFileProcessorForContentStore  = mock(EomFileProcessorForContentStore.class);
    private EomFile eomFile = mock(EomFile.class);

    /* Class upder test. */
    private PostContentToTransformResource postContentToTransformResource = new PostContentToTransformResource(eomFileProcessorForContentStore);

    @Before
    public void preconditions() throws Exception {
        when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
    }

    /**
     * Tests that an unpublished article preview request results in processing it as a PREVIEW article.
     */
    @Test
    public void previewProcessedOk() {
        postContentToTransformResource.doTransform(UUID.randomUUID().toString(), true, eomFile, httpHeaders);
        verify(eomFileProcessorForContentStore, times(1)).processPreview(eomFile, TRANSACTION_ID);
    }

    /**
     * Tests that an published article request results in processing it as a PUBLISHED article.
     */
    @Test
    public void publicationProcessedOk() {
        postContentToTransformResource.doTransform(UUID.randomUUID().toString(), false, eomFile, httpHeaders);
        verify(eomFileProcessorForContentStore, times(1)).processPublication(eomFile, TRANSACTION_ID);
    }

}
