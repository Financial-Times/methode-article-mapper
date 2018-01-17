package com.ft.methodearticlemapper.resources;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.methodearticlemapper.exception.EmbargoDateInTheFutureException;
import com.ft.methodearticlemapper.exception.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.exception.MethodeMissingBodyException;
import com.ft.methodearticlemapper.exception.MethodeMissingFieldException;
import com.ft.methodearticlemapper.exception.NotWebChannelException;
import com.ft.methodearticlemapper.exception.SourceNotEligibleForPublishException;
import com.ft.methodearticlemapper.exception.UnsupportedEomTypeException;
import com.ft.methodearticlemapper.exception.UnsupportedTransformationModeException;
import com.ft.methodearticlemapper.exception.UntransformableMethodeContentException;
import com.ft.methodearticlemapper.exception.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.TransformationMode;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that application exceptions that occur during transformation of a published article translate into correct HTTP codes by the POST endpoint.
 *
 * Created by julia.fernee on 29/01/2016.
 */
public class PostContentToTransformResourceForPublicationUnhappyPathsTest {

    private static final String INVALID_TYPE = "EOM::DistortedStory";
    private static final String TRANSACTION_ID = "tid_test";
    private static final boolean IS_PREVIEW_FALSE = false;

    private EomFileProcessor eomFileProcessor = mock(EomFileProcessor.class);
    private HttpHeaders httpHeaders = mock(HttpHeaders.class);
    private EomFile eomFile = mock(EomFile.class);
    private UUID uuid = UUID.randomUUID();

    /*Class under test*/
    private PostContentToTransformResource postContentToTransformResource = new PostContentToTransformResource(eomFileProcessor);

    @Before
    public void preconditions() {
        when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
        when(eomFile.getUuid()).thenReturn(uuid.toString());
    }

    /**
     * Test that the response contains http error code 400 and the correct message
     * when uuid path parameter is absent.
     */
    @Test
    public void shouldThrow400ExceptionWhenNoUuidPassed() {
        try {
            EomFile eomFile = Mockito.mock(EomFile.class);
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
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
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(PostContentToTransformResource.ErrorMessage.INVALID_UUID.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
        }
    }

    /**
     * Tests that response contains 404 error code and the correct message
     * when content marked as deleted in Methode is attempted to be published.
     */
    @Test
    public void shouldThrow404ExceptionWhenContentIsMarkedAsDeletedInMethode() {
        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).thenThrow(new MethodeMarkedDeletedException(uuid));
        try {
            postContentToTransformResource.map(eomFile, false, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(PostContentToTransformResource.ErrorMessage.METHODE_FILE_NOT_FOUND.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
        }
    }

    /**
     * Tests that the response contains http code 422 and the correct message
     * when the type property in the json payload is not EOM::CompoundStory.
     */
    @Test
    public void shouldThrow422ExceptionWhenPublicationNotEligibleForPublishing() {

        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
                thenThrow(new UnsupportedEomTypeException(uuid, "EOM::DistortedStory"));
        try {
            postContentToTransformResource.map(eomFile, false, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format("[%s] not an EOM::CompoundStory.", INVALID_TYPE)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }

    /**
     * Tests that response contains 422 error code and the correct message
     * when content that marked with an active publication embargo date is attempted to be published.
     */
    @Test
    public void shouldThrow422ExceptionWhenEmbargoDateInTheFuture() {
        Date embargoDate = new Date();

        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
                thenThrow(new EmbargoDateInTheFutureException(uuid, embargoDate));
        try {
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format("Embargo date [%s] is in the future", embargoDate)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }

    /**
     * Tests that response contains 422 error code and the correct message
     * when web channel element in eom file system attributes property indicates that the content is not eligible for publication.
     */
    @Test
    public void shouldThrow422ExceptionWhenNotWebChannel() {

        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
                thenThrow(new NotWebChannelException(uuid));
        try {
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(PostContentToTransformResource.ErrorMessage.NOT_WEB_CHANNEL.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }

    /**
     * Tests that response contains 422 error code and the correct message
     * when web source element in eom file attributes property indicates that the content is not eligible for publication.
     */
    @Test
    public void shouldThrow422ExceptionWhenSourceNotFt() {
        final String sourceOtherThanFt = "Pepsi";
        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
                thenThrow(new SourceNotEligibleForPublishException(uuid, sourceOtherThanFt));
        try {
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format("Source [%s] not eligible for publishing", sourceOtherThanFt)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }

    /**
     * Tests that response contains 422 error code and the correct message
     * when content with workFlow status ineligible for publication is attempted to be published.
     */
    @Test
    public void shouldThrow422ExceptionWhenWorkflowStatusNotEligibleForPublishing() {

        final String workflowStatusNotEligibleForPublishing = "Story/Edit";
        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
                thenThrow(new WorkflowStatusNotEligibleForPublishException(uuid, workflowStatusNotEligibleForPublishing));
        try {
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format("Workflow status [%s] not eligible for publishing",
                            workflowStatusNotEligibleForPublishing)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }

    /**
     * Tests that response contains 422 error code and the correct message
     * when content with missing publish date is attempted to be published.
     */
    @Test
    public void shouldThrow422ExceptionWhenMethodeFieldMissing() {
        final String missingField = "publishedDate";
        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
                thenThrow(new MethodeMissingFieldException(uuid, missingField));
        try {
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(String.format(PostContentToTransformResource.ErrorMessage.METHODE_FIELD_MISSING.toString(),
                            missingField)));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }

    /**
     * Tests that response contains 422 error code and the correct message
     * when eom-file with missing or empty value property or its value property translates into
     * an empty content body is attempted to be published.
     */
    @Test
    public void shouldThrow422ExceptionWhenMethodeBodyMissing() {
        when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
                thenThrow(new MethodeMissingBodyException(uuid));
        try {
            postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException e) {
            assertThat(((ErrorEntity)e.getResponse().getEntity()).getMessage(),
                    containsString(uuid.toString()));
            assertThat(e.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        }
    }
    
    
   /* Tests that response contains 422 error code and the correct message
    * when eom-file with missing or empty value property or its value property translates into
    * an blank content body post transformation is attempted to be published.
    */
   @Test
   public void shouldThrow422ExceptionWhenMethodeBodyBlankAfterTransformation() {
       when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
               thenThrow(new UntransformableMethodeContentException(uuid.toString(), "it's blank"));
       try {
           postContentToTransformResource.map(eomFile, IS_PREVIEW_FALSE, null, httpHeaders);
           fail("No exception was thrown, but expected one.");
       } catch (WebApplicationClientException e) {
           assertThat(((ErrorEntity)e.getResponse().getEntity()).getMessage(),
                   containsString("it's blank"));
           assertThat(e.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
       }
   }

   @Test
   public void thatUnsupportedTransformationModeIsRejected() {
       when(eomFileProcessor.process(eq(eomFile), eq(TransformationMode.PUBLISH), eq(TRANSACTION_ID), any())).
           thenThrow(new UnsupportedTransformationModeException(uuid.toString(), TransformationMode.PUBLISH));
       
       try {
           postContentToTransformResource.map(eomFile, false, TransformationMode.PUBLISH.toString(), httpHeaders);
           fail("No exception was thrown, but expected one.");
       } catch (WebApplicationClientException e) {
           assertThat(((ErrorEntity)e.getResponse().getEntity()).getMessage(),
               containsString("Transformation mode PUBLISH is not available"));
           assertThat(e.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
       }
   }
}
