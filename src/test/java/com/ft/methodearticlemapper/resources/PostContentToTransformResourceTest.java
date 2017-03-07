package com.ft.methodearticlemapper.resources;

import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostContentToTransformResourceTest {

    private static final String TRANSACTION_ID = "tid_test";

    private HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private EomFileProcessor eomFileProcessor = mock(EomFileProcessor.class);
    private EomFile eomFile = mock(EomFile.class);
    private String uuid = UUID.randomUUID().toString();

    private PostContentToTransformResource postContentToTransformResource = new PostContentToTransformResource(eomFileProcessor);

    @Before
    public void preconditions() throws Exception {
        when(eomFile.getUuid()).thenReturn(uuid);
        when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
    }

    @Test
    public void thatIfMapPreviewParamIsTruePreviewProcessingIsTriggered() {
        boolean preview = true;
        postContentToTransformResource.map(eomFile, preview, httpHeaders);

        verify(eomFileProcessor).processPreview(eq(eomFile),eq(TRANSACTION_ID));
        verify(eomFileProcessor, never()).processPublication(any(), any(), any());
    }

    @Test
    public void thatIfMapPreviewParamIsFalsePublicationProcessingIsTriggered() {
        boolean notPreview = false;
        postContentToTransformResource.map(eomFile, notPreview, httpHeaders);

        verify(eomFileProcessor).processPublication(eq(eomFile),eq(TRANSACTION_ID), any());
        verify(eomFileProcessor, never()).processPreview(any(), any());
    }
}
