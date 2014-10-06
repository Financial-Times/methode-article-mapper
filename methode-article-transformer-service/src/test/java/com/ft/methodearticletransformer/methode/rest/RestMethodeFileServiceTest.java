package com.ft.methodearticletransformer.methode.rest;

import com.ft.api.jaxrs.client.exceptions.RemoteApiException;
import com.ft.methodeapi.client.MethodeApiClient;
import com.ft.methodearticletransformer.methode.MethodeApiUnavailableException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestMethodeFileServiceTest {

	private final UUID SAMPLE_UUID = UUID.randomUUID();
	private final String sampleTransactionId = "tid_test_allieshaveleftus";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private RestMethodeFileService restMethodeFileService;
	private MethodeApiClient methodeApiClient;

	@Before
	public void setup() {
		methodeApiClient = mock(MethodeApiClient.class);
		restMethodeFileService = new RestMethodeFileService(methodeApiClient);
	}

	@Test
	public void shouldThrowMethodeFileNotFoundExceptionWhen404FromMethodeApi() {
		expectedException.expect(MethodeFileNotFoundException.class);
		expectedException.expect(hasProperty("uuid", equalTo(SAMPLE_UUID)));

		when(methodeApiClient.findFileByUuid(SAMPLE_UUID.toString(), sampleTransactionId)).thenThrow(newRemoteApiException(404));
		restMethodeFileService.fileByUuid(SAMPLE_UUID, sampleTransactionId);
	}

	@Test
	public void shouldThrowMethodeApiUnavailableExceptionWhen503FromMethodeApi() {
		RemoteApiException remoteApiException503 = newRemoteApiException(503);

		expectedException.expect(MethodeApiUnavailableException.class);
		expectedException.expect(hasProperty("cause", equalTo(remoteApiException503)));

		when(methodeApiClient.findFileByUuid(SAMPLE_UUID.toString(), sampleTransactionId)).thenThrow(remoteApiException503);
		restMethodeFileService.fileByUuid(SAMPLE_UUID, sampleTransactionId);
	}

	private RemoteApiException newRemoteApiException(int statusCode) {
		return new RemoteApiException(null, null, statusCode);
	}

}
