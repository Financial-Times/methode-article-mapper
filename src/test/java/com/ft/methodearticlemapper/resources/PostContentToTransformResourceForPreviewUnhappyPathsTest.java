package com.ft.methodearticlemapper.resources;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.methodearticlemapper.exception.UnsupportedTypeException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that application exceptions that occur during transformation of article preview translate into correct HTTP codes by the POST endpoint.
 *
 * Created by julia.fernee on 28/01/2016.
 */
public class PostContentToTransformResourceForPreviewUnhappyPathsTest {

    private static final boolean IS_PREVIEW_TRUE = true;
    private static final String TRANSACTION_ID = "tid_test";
    private static final String INVALID_TYPE = "NOT_COMPOUND_STORY";

    private EomFileProcessor eomFileProcessor = mock(EomFileProcessor.class);
    private HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private EomFile eomFile = mock(EomFile.class);

    /*Class under test*/
    private PostContentToTransformResource postContentToTransformResource = new PostContentToTransformResource(eomFileProcessor);;

    @Before
    public void preconditions() {
        when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
    }

    /**
     * Test that the response contains http error code 400 and the correct message
     * when uuid path parameter is absent.
     */
    @Test
    public void shouldThrow400ExceptionWhenNoUuidPassed() {
        try {
            postContentToTransformResource.doTransform(null, IS_PREVIEW_TRUE, eomFile, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(PostContentToTransformResource.ErrorMessage.UUID_REQUIRED.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
        }
    }

    /**
     * Tests that response contains 400 error code and the correct message
     * when uuid path variable does not conform to UUID format.
     */
    @Test
    public void shouldThrow400ExceptionWhenInvalidUuidPassed() {
        try {
            postContentToTransformResource.doTransform("invalid_uuid", IS_PREVIEW_TRUE, eomFile, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(PostContentToTransformResource.ErrorMessage.INVALID_UUID.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
        }
    }

    /**
     * Tests that response contains http code 409 and the correct error message
     * when uuid path variable in the post URI and uuid present in the payload json property are not the same.
     */
    @Test
    public void shouldThrow409ExceptionWhenUuidsMismatch() {
        String pathUuid = "1-1-1-1-1";
        String payloadUuid = "1-1-1-1-2";
        when(eomFile.getUuid()).thenReturn(payloadUuid);
        try {
            postContentToTransformResource.doTransform(pathUuid, IS_PREVIEW_TRUE, eomFile, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format(PostContentToTransformResource.ErrorMessage.CONFLICTING_UUID.toString(), pathUuid, payloadUuid)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_CONFLICT));
        }
    }

    /**
     * Tests that the response contains http code 404 and the correct message
     * when the type property in the json payload is not EOM::CompoundStory.
     */
    @Test
    public void shouldThrow404ExceptionWhenPreviewNotEligibleForPublishing() {
        UUID randomUuid = UUID.randomUUID();
        when(eomFile.getUuid()).thenReturn(randomUuid.toString());
        when(eomFileProcessor.processPreview(eomFile, TRANSACTION_ID)).
                thenThrow(new UnsupportedTypeException(randomUuid, INVALID_TYPE));
        try {
            postContentToTransformResource.doTransform(randomUuid.toString(), IS_PREVIEW_TRUE, eomFile, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format("[%s] not an EOM::CompoundStory.", INVALID_TYPE)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
        }
    }
}