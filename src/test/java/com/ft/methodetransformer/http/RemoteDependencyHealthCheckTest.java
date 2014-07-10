package com.ft.methodetransformer.http;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedResult;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.dropwizard.client.JerseyClientConfiguration;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RemoteDependencyHealthCheckTest {

	private RemoteDependencyHealthCheck healthCheck;

	@Mock
	private EndpointConfiguration mockEndpointConfiguration;
	@Mock
	private BuildInfoResource mockBuildInfoResource;
	@Mock
	private Client mockClient;
	@Mock
	private WebResource mockResource;
	@Mock
	private ClientResponse mockClientResponse;

	@Before
	public void setup() {
		when(mockEndpointConfiguration.getHost()).thenReturn("localhost");
		when(mockEndpointConfiguration.getPort()).thenReturn(9080);
		when(mockEndpointConfiguration.getShortName()).thenReturn(Optional.<String>absent());
		when(mockEndpointConfiguration.getJerseyClientConfiguration()).thenReturn(new JerseyClientConfiguration());

		when(mockClient.resource(any(URI.class))).thenReturn(mockResource);
		when(mockResource.get(ClientResponse.class)).thenReturn(mockClientResponse);

		healthCheck = new RemoteDependencyHealthCheck("test methode api version",
				mockClient,
				mockEndpointConfiguration,
				mockBuildInfoResource,
				"minimum.methode.api.version");
	}

	@Test
	public void shouldReturnUnhealthyUnexpectedStatusWhen404ReturnedByDependency() throws Exception {
		when(mockClientResponse.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
		
		AdvancedResult expectedHealthCheckResult = AdvancedResult.error(healthCheck, "Unexpected status : 404");
		AdvancedResult actualHealthCheckResult = healthCheck.checkAdvanced();

		assertThat(actualHealthCheckResult.status(), is(expectedHealthCheckResult.status()));
		assertThat(actualHealthCheckResult.checkOutput(), is(expectedHealthCheckResult.checkOutput()));
	}

}
