package com.ft.methodearticlemapper.resources;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import com.ft.methodearticlemapper.methode.EmbargoDateInTheFutureException;
import com.ft.methodearticlemapper.methode.MethodeArticleTransformerErrorEntityFactory;
import com.ft.methodearticlemapper.methode.MethodeMarkedDeletedException;
import com.ft.methodearticlemapper.methode.MethodeMissingBodyException;
import com.ft.methodearticlemapper.methode.MethodeMissingFieldException;
import com.ft.methodearticlemapper.methode.NotWebChannelException;
import com.ft.methodearticlemapper.methode.ResourceNotFoundException;
import com.ft.methodearticlemapper.methode.SourceNotEligibleForPublishException;
import com.ft.methodearticlemapper.methode.UnsupportedTypeException;
import com.ft.methodearticlemapper.methode.WorkflowStatusNotEligibleForPublishException;
import com.ft.methodearticlemapper.methode.ContentSourceService;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class GetTransformedContentResourceTest {

	private static final String TRANSACTION_ID = "tid_test";
	private static final Date LAST_MODIFIED = new Date();

	private GetTransformedContentResource getTransformedContentResource;
	private ContentSourceService contentSourceService;
	private HttpHeaders httpHeaders;
	private EomFileProcessor eomFileProcessor;

	@Before
	public void setup() {
		contentSourceService = mock(ContentSourceService.class);
		eomFileProcessor = mock(EomFileProcessor.class);

		getTransformedContentResource = new GetTransformedContentResource(contentSourceService, eomFileProcessor);

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
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())). thenReturn(content);

		assertThat(getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders), equalTo(content));
	}

	@Test
	public void shouldThrow400ExceptionWhenNoUuidPassed() {
		try {
			getTransformedContentResource.getByUuid(null, httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(ErrorMessage.UUID_REQUIRED.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
		}
	}

	@Test
	public void shouldThrow400ExceptionWhenInvalidUuidPassed() {
		try {
			getTransformedContentResource.getByUuid("tid_yvkt5qfeyw", httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(ErrorMessage.INVALID_UUID.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenContentNotFoundInNativeStore() {
		UUID randomUuid = UUID.randomUUID();
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenThrow(new ResourceNotFoundException(randomUuid));
		try {
			getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(ErrorMessage.METHODE_FILE_NOT_FOUND.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
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

		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
				thenThrow(new MethodeMarkedDeletedException(randomUuid));
        try {
            getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException wace) {
            assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
                    equalTo(ErrorMessage.METHODE_FILE_NOT_FOUND.toString()));
            assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
        }
    }

	@Test
	public void shouldThrow404ExceptionWhenContentNotEligibleForPublishing() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
				thenThrow(new UnsupportedTypeException(randomUuid, "EOM::DistortedStory"));
		try {
			getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo("[EOM::DistortedStory] not an EOM::CompoundStory."));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenEmbargoDateInTheFuture() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		Date embargoDate = new Date();
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
				thenThrow(new EmbargoDateInTheFutureException(randomUuid, embargoDate));
		try {
			getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format("Embargo date [%s] is in the future", embargoDate)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenNotWebChannel() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
				thenThrow(new NotWebChannelException(randomUuid));
		try {
			getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(ErrorMessage.NOT_WEB_CHANNEL.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenSourceNotFt() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		final String sourceOtherThanFt = "Pepsi";
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
				thenThrow(new SourceNotEligibleForPublishException(randomUuid, sourceOtherThanFt));
		try {
			getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format("Source [%s] not eligible for publishing", sourceOtherThanFt)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenWorkflowStatusNotEligibleForPublishing() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		final String workflowStatusNotEligibleForPublishing = "Story/Edit";
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
				thenThrow(new WorkflowStatusNotEligibleForPublishException(randomUuid, workflowStatusNotEligibleForPublishing));
		try {
			getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format("Workflow status [%s] not eligible for publishing",
							workflowStatusNotEligibleForPublishing)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenMethodeFieldMissing() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(contentSourceService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		final String missingField = "publishedDate";
		when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
				thenThrow(new MethodeMissingFieldException(randomUuid, missingField));
		try {
			getTransformedContentResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(String.format(ErrorMessage.METHODE_FIELD_MISSING.toString(),
							missingField)));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		}
	}

    @Test
    public void shouldThrow418ExceptionWhenMethodeBodyMissing() {
        UUID uuid = UUID.randomUUID();
        EomFile eomFile = mock(EomFile.class);
        when(contentSourceService.fileByUuid(uuid, TRANSACTION_ID)).thenReturn(eomFile);
        when(eomFileProcessor.processPublication(eq(eomFile), eq(TRANSACTION_ID), any())).
                thenThrow(new MethodeMissingBodyException(uuid));
        try {
            getTransformedContentResource.getByUuid(uuid.toString(), httpHeaders);
            fail("No exception was thrown, but expected one.");
        } catch (WebApplicationClientException e) {
            assertThat(((ErrorEntity)e.getResponse().getEntity()).getMessage(),
                    containsString(uuid.toString()));
            assertThat(e.getResponse().getStatus(), equalTo(418));
        }
    }
}
