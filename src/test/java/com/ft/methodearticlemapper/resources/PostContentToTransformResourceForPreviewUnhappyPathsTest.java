package com.ft.methodearticlemapper.resources;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.methodearticlemapper.exception.UnsupportedEomTypeException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
            EomFile eomFile = Mockito.mock(EomFile.class);
            postContentToTransformResource.map(eomFile, IS_PREVIEW_TRUE, null, httpHeaders);
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
            EomFile eomFile = ArticlePreviewTransformationTest.articlePreviewMinimalEomFile("invalid_uuid");
            postContentToTransformResource.map(eomFile, IS_PREVIEW_TRUE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(PostContentToTransformResource.ErrorMessage.INVALID_UUID.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
        }
    }

    /**
     * Tests that the response contains http code 422 and the correct message
     * when the type property in the json payload is not EOM::CompoundStory.
     */
    @Test
    public void shouldThrow422ExceptionWhenPreviewNotEligibleForPublishing() {
        UUID randomUuid = UUID.randomUUID();
        when(eomFile.getUuid()).thenReturn(randomUuid.toString());
        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PREVIEW), eq(TRANSACTION_ID), any())).
                thenThrow(new UnsupportedEomTypeException(randomUuid, INVALID_TYPE));
        try {
            postContentToTransformResource.map(eomFile, IS_PREVIEW_TRUE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format("[%s] not an EOM::CompoundStory.", INVALID_TYPE)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }
}
