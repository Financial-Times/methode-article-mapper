package com.ft.methodetransformer.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.methodeapi.client.MethodeApiEndpointConfiguration;

import io.dropwizard.Configuration;

import javax.validation.constraints.NotNull;

public class MethodeTransformerConfiguration extends Configuration {
	
	private final EndpointConfiguration semanticContentStoreReaderConfiguration;
	private final MethodeApiEndpointConfiguration methodeApiConfiguration;

    public MethodeTransformerConfiguration(@JsonProperty("methodeApi") MethodeApiEndpointConfiguration methodeApiConfiguration,
			  @JsonProperty("semanticContentStoreReader") EndpointConfiguration semanticContentStoreReaderConfiguration) {
		this.semanticContentStoreReaderConfiguration = semanticContentStoreReaderConfiguration;
		this.methodeApiConfiguration = methodeApiConfiguration;
	}

	@NotNull
    @JsonProperty
    private long slowRequestTimeout;
    @NotNull
    @JsonProperty
    private String slowRequestPattern;

    public long getSlowRequestTimeout() {
        return slowRequestTimeout;
    }

    public String getSlowRequestPattern() {
        return slowRequestPattern;
    }

	public MethodeApiEndpointConfiguration getMethodeApiConfiguration() {
		return methodeApiConfiguration;
	}

	@NotNull
	public EndpointConfiguration getSemanticContentStoreReaderConfiguration() {
		return semanticContentStoreReaderConfiguration;
	}
}
