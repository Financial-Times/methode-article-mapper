package com.ft.methodearticletransformer.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import com.ft.content.model.Brand;
import io.dropwizard.Configuration;

import java.util.List;

public class MethodeArticleTransformerConfiguration extends Configuration {
	
	private final SemanticReaderEndpointConfiguration semanticReaderEndpointConfiguration;
	private final SourceApiEndpointConfiguration sourceApiConfiguration;
    private final Brand financialTimesBrand;
    private final List<VideoSiteConfiguration> videoSiteConfig;

    public MethodeArticleTransformerConfiguration(@JsonProperty("sourceApi") SourceApiEndpointConfiguration sourceApiConfiguration,
                                                  @JsonProperty("semanticContentStoreReader") SemanticReaderEndpointConfiguration semanticReaderEndpointConfiguration,
                                                  @JsonProperty("financialTimesBrandId") String financialTimesBrandId,
                                                  @JsonProperty("videoSiteConfig") List<VideoSiteConfiguration> videoSiteConfig) {

        this.semanticReaderEndpointConfiguration = semanticReaderEndpointConfiguration;
		this.sourceApiConfiguration = sourceApiConfiguration;
        this.financialTimesBrand = new Brand(financialTimesBrandId);
        this.videoSiteConfig = videoSiteConfig;
    }

	public SourceApiEndpointConfiguration getSourceApiConfiguration() {
		return sourceApiConfiguration;
	}

	@NotNull
	public SemanticReaderEndpointConfiguration getSemanticReaderEndpointConfiguration() { return semanticReaderEndpointConfiguration; }

    @NotNull
    public Brand getFinancialTimesBrand() {
        return financialTimesBrand;
    }

    @NotNull
    public List<VideoSiteConfiguration> getVideoSiteConfig() { return videoSiteConfig; }
}
