package com.ft.methodearticletransformer.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import com.ft.content.model.Brand;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import io.dropwizard.Configuration;

import java.util.List;

public class MethodeArticleTransformerConfiguration extends Configuration {
	
	private final EndpointConfiguration semanticContentStoreReaderConfiguration;
	private final MethodeApiEndpointConfiguration methodeApiConfiguration;
    private final Brand financialTimesBrand;
    private final List<VideoSiteConfiguration> videoSiteConfig;

    public MethodeArticleTransformerConfiguration(@JsonProperty("methodeApi") MethodeApiEndpointConfiguration methodeApiConfiguration,
                                                  @JsonProperty("semanticContentStoreReader") EndpointConfiguration semanticContentStoreReaderConfiguration,
                                                  @JsonProperty("financialTimesBrandId") String financialTimesBrandId,
                                                  @JsonProperty("videoSiteConfig") List<VideoSiteConfiguration> videoSiteConfig) {

        this.semanticContentStoreReaderConfiguration = semanticContentStoreReaderConfiguration;
		this.methodeApiConfiguration = methodeApiConfiguration;
        this.financialTimesBrand = new Brand(financialTimesBrandId);
        this.videoSiteConfig = videoSiteConfig;
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

    @NotNull
    public List<VideoSiteConfiguration> getVideoSiteConfig() { return videoSiteConfig; }
}
