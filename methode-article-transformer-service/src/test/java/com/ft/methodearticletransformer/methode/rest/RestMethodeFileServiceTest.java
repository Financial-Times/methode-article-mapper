package com.ft.methodearticletransformer.methode.rest;

import com.codahale.metrics.MetricRegistry;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.methodearticletransformer.configuration.AssetTypeRequestConfiguration;
import com.ft.methodearticletransformer.configuration.MethodeApiEndpointConfiguration;
import com.ft.methodearticletransformer.methode.MethodeApiUnavailableException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import cucumber.api.java.After;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

public class RestMethodeFileServiceTest {

	private static final String TEST_HOST = "localhost";
	private static final String ROOT = "/";
	private static final UUID SAMPLE_UUID = UUID.randomUUID();
	private static final String SAMPLE_TRANSACTION_ID = "tid_test_allieshaveleftus";

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(0); //will allocate a free port

	@Rule
	public WireMockClassRule instanceRule = wireMockRule;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private RestMethodeFileService restMethodeFileService;

	@Before
	public void setup() {

		JerseyClientConfiguration fastTimeOuts = new JerseyClientConfiguration();
		fastTimeOuts.setConnectionTimeout(Duration.milliseconds(100));
		fastTimeOuts.setTimeout(Duration.milliseconds(100));

		int port = instanceRule.port();

		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
				Optional.of("methode-file-service-test"),
				Optional.of(fastTimeOuts),
				Optional.of(ROOT),
				Arrays.asList(String.format("%s:%d:%d", TEST_HOST, port, port + 1)),
				Collections.<String>emptyList());

		AssetTypeRequestConfiguration assetTypeRequestConfiguration = new AssetTypeRequestConfiguration(4, 4);

		MethodeApiEndpointConfiguration methodeApiEndpointConfiguration =
				new MethodeApiEndpointConfiguration(endpointConfiguration, assetTypeRequestConfiguration);

		Environment environment = new Environment("test-env", null, null, new MetricRegistry(), Thread.currentThread().getContextClassLoader());

		Client client = ResilientClientBuilder.in(environment).using(endpointConfiguration).build();

		restMethodeFileService = new RestMethodeFileService(environment, client, methodeApiEndpointConfiguration);
	}

	@Test
	public void shouldThrowMethodeFileNotFoundExceptionWhen404FromMethodeApi() {
		stubFor(get(toFindEomFileUrl()).willReturn(aResponseWithCode(404)));

		expectedException.expect(MethodeFileNotFoundException.class);
		expectedException.expect(hasProperty("uuid", equalTo(SAMPLE_UUID)));

		restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
	}

	@Test
	public void shouldThrowMethodeApiUnavailableExceptionWhen503FromMethodeApi() {
		stubFor(get(toFindEomFileUrl()).willReturn(aResponseWithCode(503)));

		expectedException.expect(MethodeApiUnavailableException.class);

		restMethodeFileService.fileByUuid(SAMPLE_UUID, SAMPLE_TRANSACTION_ID);
	}

	private UrlMatchingStrategy toFindEomFileUrl() {
		return urlMatching("/eom-file/.*");
	}

	private ResponseDefinitionBuilder aResponseWithCode(int code) {
		return aResponse().withStatus(code).withHeader("Content-type", "application/json");
	}

	@After
	public void cleanUp() {
		reset();
	}

}
