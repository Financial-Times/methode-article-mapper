package com.ft.methodearticletransformer.resources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.methode.EmbargoDateInTheFutureException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.methode.MethodeMarkedDeletedException;
import com.ft.methodearticletransformer.methode.MethodeMissingFieldException;
import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.SourceNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.UnsupportedTypeException;
import com.ft.methodearticletransformer.methode.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class MethodeArticleTransformerResourceTest {

	private static final String TRANSACTION_ID = "tid_test";

	private MethodeArticleTransformerResource methodeArticleTransformerResource;
	private MethodeFileService methodeFileService;
	private HttpHeaders httpHeaders;
	private EomFileProcessorForContentStore eomFileProcessorForContentStore;

	@Before
	public void setup() {
		methodeFileService = mock(MethodeFileService.class);
		eomFileProcessorForContentStore = mock(EomFileProcessorForContentStore.class);

		methodeArticleTransformerResource = new MethodeArticleTransformerResource(methodeFileService, eomFileProcessorForContentStore);

		httpHeaders = mock(HttpHeaders.class);
		when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
	}

	@Test
	public void shouldReturn200WhenProcessedOk() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		Content content = Content.builder().build();
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)). thenReturn(content);

		assertThat(methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders), equalTo(content));
	}

	@Test
	public void shouldThrow400ExceptionWhenNoUuidPassed() {
		try {
			methodeArticleTransformerResource.getByUuid(null, httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(MethodeArticleTransformerResource.ErrorMessage.UUID_REQUIRED.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow400ExceptionWhenInvalidUuidPassed() {
		try {
			methodeArticleTransformerResource.getByUuid("tid_yvkt5qfeyw", httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(MethodeArticleTransformerResource.ErrorMessage.INVALID_UUID.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenContentNotFoundInMethode() {
		UUID randomUuid = UUID.randomUUID();
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenThrow(new MethodeFileNotFoundException(randomUuid));
		try {
			methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(MethodeArticleTransformerResource.ErrorMessage.METHODE_FILE_NOT_FOUND.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

    @Test
    public void shouldThrow404ExceptionWhenContentIsMarkedAsDeletedInMethode() {
        UUID randomUuid = UUID.randomUUID();
        when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenThrow(new MethodeMarkedDeletedException(randomUuid));
        try {
            methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(MethodeArticleTransformerResource.ErrorMessage.METHODE_FILE_NOT_FOUND.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
        } catch (Throwable throwable) {
            fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
                    throwable.getClass().getCanonicalName()));
        }
    }

	@Test
	public void shouldThrow404ExceptionWhenContentNotEligibleForPublishing() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new UnsupportedTypeException(randomUuid, "EOM::DistortedStory"));
		try {
			methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo("[EOM::DistortedStory] not an EOM::CompoundStory."));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenEmbargoDateInTheFuture() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		Date embargoDate = new Date();
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new EmbargoDateInTheFutureException(randomUuid, embargoDate));
		try {
			methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format("Embargo date [%s] is in the future", embargoDate)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenNotWebChannel() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new NotWebChannelException(randomUuid));
		try {
			methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(MethodeArticleTransformerResource.ErrorMessage.NOT_WEB_CHANNEL.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenSourceNotFt() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		final String sourceOtherThanFt = "Pepsi";
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new SourceNotEligibleForPublishException(randomUuid, sourceOtherThanFt));
		try {
			methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format("Source [%s] not eligible for publishing", sourceOtherThanFt)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenWorkflowStatusNotEligibleForPublishing() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		final String workflowStatusNotEligibleForPublishing = "Story/Edit";
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new WorkflowStatusNotEligibleForPublishException(randomUuid, workflowStatusNotEligibleForPublishing));
		try {
			methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format("Workflow status [%s] not eligible for publishing",
							workflowStatusNotEligibleForPublishing)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenMethodeFieldMissing() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		final String missingField = "publishedDate";
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new MethodeMissingFieldException(randomUuid, missingField));
		try {
			methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format(MethodeArticleTransformerResource.ErrorMessage.METHODE_FIELD_MISSING.toString(),
							missingField)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

}
