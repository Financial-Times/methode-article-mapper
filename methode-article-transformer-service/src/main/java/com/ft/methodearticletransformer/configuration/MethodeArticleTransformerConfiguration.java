package com.ft.methodearticletransformer.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.content.model.Brand;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import io.dropwizard.Configuration;

public class MethodeArticleTransformerConfiguration extends Configuration {
	
	private final EndpointConfiguration semanticContentStoreReaderConfiguration;
	private final MethodeApiEndpointConfiguration methodeApiConfiguration;
    private final Brand financialTimesBrand;

    public MethodeArticleTransformerConfiguration(@JsonProperty("methodeApi") MethodeApiEndpointConfiguration methodeApiConfiguration,
                                                  @JsonProperty("semanticContentStoreReader") EndpointConfiguration semanticContentStoreReaderConfiguration,
                                                  @JsonProperty("financialTimesBrandId") String financialTimesBrandId) {

        this.semanticContentStoreReaderConfiguration = semanticContentStoreReaderConfiguration;
		this.methodeApiConfiguration = methodeApiConfiguration;
        this.financialTimesBrand = new Brand(financialTimesBrandId);
    }

	public MethodeApiEndpointConfiguration getMethodeApiConfiguration() {
		return methodeApiConfiguration;
	}

	@NotNull
	public EndpointConfiguration getSemanticContentStoreReaderConfiguration() {
		return semanticContentStoreReaderConfiguration;
	}

    @NotNull
    public Brand getFinancialTimesBrand() {
        return financialTimesBrand;
    }
}
