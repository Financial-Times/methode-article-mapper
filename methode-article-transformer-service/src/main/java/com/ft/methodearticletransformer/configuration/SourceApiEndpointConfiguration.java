package com.ft.methodearticletransformer.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import io.dropwizard.client.JerseyClientConfiguration;

import java.util.Arrays;
import java.util.Collections;

public class SourceApiEndpointConfiguration {

	private final EndpointConfiguration endpointConfiguration;

	private final AssetTypeRequestConfiguration assetTypeRequestConfiguration;

    private final ConnectionConfiguration connectionConfig;

	/**
	 * Creates a simple configuration for a test host (e.g. WireMock)
	 * with GZip disabled.
	 * @param host the test server
	 * @param port the test port
	 * @param assetTypeRequestConfiguration the configuration for requesting asset types
	 * @return a simple configuration
	 */
	public static SourceApiEndpointConfiguration forTesting(String host, int port, AssetTypeRequestConfiguration assetTypeRequestConfiguration, ConnectionConfiguration connectionConfig) {

		JerseyClientConfiguration clientConfig = new JerseyClientConfiguration();
		clientConfig.setGzipEnabled(false);
		clientConfig.setGzipEnabledForRequests(false);

		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(
				Optional.of(String.format("test-%s-%s", host, port)),
				Optional.of(clientConfig),
				Optional.<String>absent(),
				Arrays.asList(String.format("%s:%d:%d", host, port, port + 1)),
				Collections.<String>emptyList());

		return new SourceApiEndpointConfiguration(
				endpointConfiguration,
				assetTypeRequestConfiguration,
                connectionConfig
		);
	}


	public SourceApiEndpointConfiguration(@JsonProperty("endpointConfiguration") EndpointConfiguration endpointConfiguration,
                                          @JsonProperty("assetTypeRequestConfiguration") AssetTypeRequestConfiguration assetTypeRequestConfiguration,
                                          @JsonProperty("connectionConfig") ConnectionConfiguration connectionConfig) {
		this.endpointConfiguration = endpointConfiguration;
		this.assetTypeRequestConfiguration = assetTypeRequestConfiguration;
        this.connectionConfig = connectionConfig;
	}

	public EndpointConfiguration getEndpointConfiguration() {
		return endpointConfiguration;
	}

	public AssetTypeRequestConfiguration getAssetTypeRequestConfiguration() {
		return assetTypeRequestConfiguration;
	}

    public ConnectionConfiguration getConnectionConfiguration() { return connectionConfig; }

	protected Objects.ToStringHelper toStringHelper() {
		return Objects
				.toStringHelper(this)
				.add("endpointConfiguration", endpointConfiguration)
				.add("assetTypeRequestConfiguration", assetTypeRequestConfiguration)
                .add("connectionConfig", connectionConfig);
	}

	@Override
	public String toString() {
		return toStringHelper().toString();
	}

}

