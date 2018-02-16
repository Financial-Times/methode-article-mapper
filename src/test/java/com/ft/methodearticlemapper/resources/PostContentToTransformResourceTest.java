package com.ft.methodearticlemapper.resources;

import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.methodearticlemapper.configuration.PropertySource;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostContentToTransformResourceTest {

    private static final String TRANSACTION_ID = "tid_test";

    private EomFileProcessor eomFileProcessor = mock(EomFileProcessor.class);
    private EomFile eomFile = mock(EomFile.class);
    private String uuid = UUID.randomUUID().toString();

    @Before
    public void preconditions() throws Exception {
        when(eomFile.getUuid()).thenReturn(uuid);
        MDC.put("transaction_id", "transaction_id=" + TRANSACTION_ID);
    }

    @Test
    public void thatIfMapPreviewParamIsTruePreviewProcessingIsTriggered() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        
        boolean preview = true;
        resource.map(eomFile, preview, null);

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PREVIEW), eq(TRANSACTION_ID), any());
        verify(eomFileProcessor, never()).process(any(), eq(TransformationMode.PUBLISH), any(), any());
    }

    @Test
    public void thatModeQueryParameterIsPassedThrough() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        
        resource.map(eomFile, false, TransformationMode.SUGGEST.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.SUGGEST), eq(TRANSACTION_ID), any());
    }

    @Test
    public void thatModeQueryParameterIsCaseInsensitive() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        
        resource.map(eomFile, false, TransformationMode.SUGGEST.toString());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.SUGGEST), eq(TRANSACTION_ID), any());
    }

    @Test(expected=WebApplicationClientException.class)
    public void thatModeQueryParameterIsValidated() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        
        resource.map(eomFile, false, "foobar");
    }

    @Test
    public void thatModeQueryParameterTrumpsConflictingPreviewFlagFalse() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        
        resource.map(eomFile, false, TransformationMode.PREVIEW.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PREVIEW), eq(TRANSACTION_ID), any());
    }

    @Test
    public void thatModeQueryParameterTrumpsConflictingPreviewFlagTrue() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any());
    }

    @Test
    public void thatLastModifiedComesFromNativeWhenSourceIsNative() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromNative, PropertySource.fromTransaction, "publishReference");
        String lastModified = "2017-12-01T14:32:27Z";
        Date lastModifiedDate = Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(lastModified)));
        
        when(eomFile.getAdditionalProperties()).thenReturn(Collections.singletonMap("lastModified", lastModified));
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), eq(lastModifiedDate));
    }

    @Test
    public void thatLastModifiedNullIsAllowedWhenSourceIsNative() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromNative, PropertySource.fromTransaction, "publishReference");
        
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), isNull(Date.class));
    }

    @Test
    public void thatLastModifiedInvalidBecomesNullWhenSourceIsNative() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromNative, PropertySource.fromTransaction, "publishReference");
        String lastModified = "foobar";
        
        when(eomFile.getAdditionalProperties()).thenReturn(Collections.singletonMap("lastModified", lastModified));
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), isNull(Date.class));
    }

    @Test
    public void thatLastModifiedIsNowWhenSourceIsTransaction() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        String lastModified = "2017-12-01T14:32:27Z";
        
        when(eomFile.getAdditionalProperties()).thenReturn(Collections.singletonMap("lastModified", lastModified));
        
        Date before = new Date();
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());
        Date after = new Date();
        
        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), argThat(allOf(greaterThanOrEqualTo(before), lessThanOrEqualTo(after))));
    }

    @Test
    public void thatTransactionIdComesFromNativeWhenSourceIsNative() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromNative, "publishReference");
        String txId = "tid_foobar";
        when(eomFile.getAdditionalProperties()).thenReturn(Collections.singletonMap("publishReference", txId));
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(txId), any());
    }

    @Test
    public void thatTransactionIdComesFromTransactionWhenSourceIsTransaction() {
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        String txId = "tid_foobar";
        when(eomFile.getAdditionalProperties()).thenReturn(Collections.singletonMap("publishReference", txId));
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());

        verify(eomFileProcessor).process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any());
    }

    @Test(expected=NullPointerException.class)
    public void thatTransactionIdIsRequiredWhenSourceIsTransaction() {
        MDC.clear();
        PostContentToTransformResource resource = new PostContentToTransformResource(eomFileProcessor, PropertySource.fromTransaction, PropertySource.fromTransaction, "publishReference");
        String txId = "tid_foobar";
        when(eomFile.getAdditionalProperties()).thenReturn(Collections.singletonMap("publishReference", txId));
        resource.map(eomFile, true, TransformationMode.PUBLISH.toString().toLowerCase());
    }
}
