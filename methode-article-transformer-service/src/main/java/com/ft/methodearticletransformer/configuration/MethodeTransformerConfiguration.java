package com.ft.methodearticletransformer.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;

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

	public MethodeApiEndpointConfiguration getMethodeApiConfiguration() {
		return methodeApiConfiguration;
	}

	@NotNull
	public EndpointConfiguration getSemanticContentStoreReaderConfiguration() {
		return semanticContentStoreReaderConfiguration;
	}
}
