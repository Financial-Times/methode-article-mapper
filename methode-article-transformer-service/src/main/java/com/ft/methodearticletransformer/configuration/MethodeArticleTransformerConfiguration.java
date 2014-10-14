package com.ft.methodearticletransformer.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import io.dropwizard.Configuration;

public class MethodeArticleTransformerConfiguration extends Configuration {
	
	private final EndpointConfiguration semanticContentStoreReaderConfiguration;
	private final MethodeApiEndpointConfiguration methodeApiConfiguration;

    public MethodeArticleTransformerConfiguration(@JsonProperty("methodeApi") MethodeApiEndpointConfiguration methodeApiConfiguration,
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
