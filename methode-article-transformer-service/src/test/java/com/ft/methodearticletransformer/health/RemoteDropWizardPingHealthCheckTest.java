package com.ft.methodearticletransformer.health;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;

public class RemoteDropWizardPingHealthCheckTest {
	public static final String TEST_HOST = "localhost";
	public static final String ROOT = "/";
	private static final Integer TIMEOUT = 5000;
	private static final int API_PORT = 33666; // this port needs to be in config but isn't used

	EndpointConfiguration endpointConfiguration;
	Environment environment;


	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(0); //will allocate a free port

	@Rule
	public WireMockClassRule instanceRule = wireMockRule;
	private Client client;

	@Before
	public void setUpEndpoints() {

		JerseyClientConfiguration fastTimeOuts = new JerseyClientConfiguration();
		fastTimeOuts.setConnectionTimeout(Duration.milliseconds(100));
		fastTimeOuts.setTimeout(Duration.milliseconds(100));

		int apiAdminPort = instanceRule.port();

		endpointConfiguration = new EndpointConfiguration(
				Optional.of("heathchecktest"),
				Optional.of(fastTimeOuts),
				Optional.of(ROOT),
				Arrays.asList(String.format("%s:%d:%d", TEST_HOST, API_PORT, apiAdminPort)),
				Collections.<String>emptyList()
		);

		environment = new Environment("test-env", new ObjectMapper(), null, new MetricRegistry(), Thread.currentThread().getContextClassLoader());

		client = ResilientClientBuilder.in(environment).using(endpointConfiguration).usingAdminPorts().build();
	}


	@Test
	public void givenPingApiIsUpHealthCheckShouldPass() {

		stubFor(get(toHealthcheckUrl()).willReturn(aPongResponse()));

		RemoteDropWizardPingHealthCheck checkUnderTest = new RemoteDropWizardPingHealthCheck("test", client, endpointConfiguration);

		HealthCheck.Result result = checkUnderTest.execute();

		assertThat(result, hasProperty("healthy", is(true)));

	}

	private ResponseDefinitionBuilder aPongResponse() {
		return aResponse().withStatus(200).withBody("pong\n");
	}

	@Test
	public void givenPingApiIsDownHealthCheckShouldFail() {

		stubFor(get(toHealthcheckUrl()).willReturn(aPongResponse().withFixedDelay(thatWill(TIMEOUT))));

		RemoteDropWizardPingHealthCheck checkUnderTest = new RemoteDropWizardPingHealthCheck("test", client, endpointConfiguration);

		HealthCheck.Result result = checkUnderTest.execute();

		assertThat(result, hasProperty("healthy", is(false)));
	}

	@Test
	public void givenPingApiReturnsUnexpectedTextHealthCheckShouldFail() {

		stubFor(get(toHealthcheckUrl()).willReturn(aResponse().withStatus(200).withBody("spong\n")));

		RemoteDropWizardPingHealthCheck checkUnderTest = new RemoteDropWizardPingHealthCheck("test", client, endpointConfiguration);

		HealthCheck.Result result = checkUnderTest.execute();

		assertThat(result, hasProperty("healthy", is(false)));
	}


	@Test
	public void givenPingApiReturnsUnexpectedStatusHealthCheckShouldFail() {

		stubFor(get(toHealthcheckUrl()).willReturn(aResponse().withStatus(404).withBody("pong\n")));

		RemoteDropWizardPingHealthCheck checkUnderTest = new RemoteDropWizardPingHealthCheck("test", client, endpointConfiguration);

		HealthCheck.Result result = checkUnderTest.execute();

		assertThat(result, hasProperty("healthy", is(false)));
	}

	private UrlMatchingStrategy toHealthcheckUrl() {
		return urlMatching("/__health");
	}

	private Integer thatWill(Integer n) {
		return n;
	}
}
