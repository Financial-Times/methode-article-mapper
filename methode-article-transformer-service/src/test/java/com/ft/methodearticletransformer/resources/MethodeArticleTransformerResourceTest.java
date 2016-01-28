package com.ft.methodearticletransformer.resources;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.Errors;
import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodearticletransformer.methode.EmbargoDateInTheFutureException;
import com.ft.methodearticletransformer.methode.IdentifiableErrorEntity;
import com.ft.methodearticletransformer.methode.MethodeArticleTransformerErrorEntityFactory;
import com.ft.methodearticletransformer.methode.MethodeMarkedDeletedException;
import com.ft.methodearticletransformer.methode.MethodeMissingBodyException;
import com.ft.methodearticletransformer.methode.MethodeMissingFieldException;
import com.ft.methodearticletransformer.methode.NotWebChannelException;
import com.ft.methodearticletransformer.methode.ResourceNotFoundException;
import com.ft.methodearticletransformer.methode.SourceNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.UnsupportedTypeException;
import com.ft.methodearticletransformer.methode.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.ContentSourceService;
import com.ft.methodearticletransformer.model.EomFile;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class MethodeArticleTransformerResourceTest {

	private static final String TRANSACTION_ID = "tid_test";

	private MethodeArticleTransformerResource methodeArticleTransformerResource;
	private ContentSourceService contentSourceService;
	private HttpHeaders httpHeaders;
	private EomFileProcessorForContentStore eomFileProcessorForContentStore;

	@Before
	public void setup() {
		contentSourceService = mock(ContentSourceService.class);
		eomFileProcessorForContentStore = mock(EomFileProcessorForContentStore.class);

		methodeArticleTransformerResource = new MethodeArticleTransformerResource(contentSourceService, eomFileProcessorForContentStore);

		httpHeaders = mock(HttpHeaders.class);
		when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));

		Errors.customise(new MethodeArticleTransformerErrorEntityFactory());
	}

	@Test
	public void shouldReturn200WhenProcessedOk() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		Content content = Content.builder().build();
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
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
	public void shouldThrow404ExceptionWhenContentNotFoundInNativeStore() {
		UUID randomUuid = UUID.randomUUID();
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenThrow(new ResourceNotFoundException(randomUuid));
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
		Date date = new Date();
		OffsetDateTime offsetDateTime = OffsetDateTime.of(
				LocalDateTime.ofInstant(
						date.toInstant(),
						ZoneId.of(ZoneOffset.UTC.getId())
				),
				ZoneOffset.UTC
		);

		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(eomFile.getLastModified()).thenReturn(date);

		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new MethodeMarkedDeletedException(randomUuid));
        try {
            methodeArticleTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
			IdentifiableErrorEntity identifiableErrorEntity = (IdentifiableErrorEntity) wace.getResponse().getEntity();
			assertThat(identifiableErrorEntity.getLastModified(), equalTo(offsetDateTime));
			assertThat(
					identifiableErrorEntity.getMessage(),
                    equalTo(
							MethodeArticleTransformerResource.ErrorMessage.METHODE_FILE_NOT_FOUND.toString()
					)
			);
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
		when(eomFile.getLastModified()).thenReturn(new Date());
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
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
		when(eomFile.getLastModified()).thenReturn(new Date());
		Date embargoDate = new Date();
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
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
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
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
		when(eomFile.getLastModified()).thenReturn(new Date());
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
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
		when(eomFile.getLastModified()).thenReturn(new Date());
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
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
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
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

    @Test
    public void shouldThrow418ExceptionWhenMethodeBodyMissing() {
        UUID uuid = UUID.randomUUID();
        EomFile eomFile = mock(EomFile.class);
        when(contentSourceService.fileByUuid(uuid, TRANSACTION_ID)).thenReturn(eomFile);
        when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
                thenThrow(new MethodeMissingBodyException(uuid));
        try {
            methodeArticleTransformerResource.getByUuid(uuid.toString(), httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException e) {
            assertThat(((ErrorEntity)e.getResponse().getEntity()).getMessage(),
                    containsString(uuid.toString()));
            assertThat(e.getResponse().getStatus(), equalTo(418));
        }
    }
}
