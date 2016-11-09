package com.ft.methodearticletransformer.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import com.ft.content.model.Brand;
import io.dropwizard.Configuration;

import java.util.List;

public class MethodeArticleTransformerConfiguration extends Configuration {
	
	private final DocumentStoreApiConfiguration documentStoreApiConfiguration;
	private final SourceApiEndpointConfiguration sourceApiConfiguration;
	private final ConcordanceApiConfiguration concordanceApiConfiguration;
    private final Brand financialTimesBrand;
    private final List<VideoSiteConfiguration> videoSiteConfig;
    private final List<String> interactiveGraphicsWhiteList;

    public MethodeArticleTransformerConfiguration(@JsonProperty("concordanceApi") ConcordanceApiConfiguration concordanceApiConfiguration,
    											  @JsonProperty("sourceApi") SourceApiEndpointConfiguration sourceApiConfiguration,
                                                  @JsonProperty("documentStoreApi") DocumentStoreApiConfiguration documentStoreApiConfiguration,
                                                  @JsonProperty("financialTimesBrandId") String financialTimesBrandId,
                                                  @JsonProperty("videoSiteConfig") List<VideoSiteConfiguration> videoSiteConfig,
                                                  @JsonProperty("interactiveGraphicsWhiteList") List<String> interactiveGraphicsWhiteList) {

        this.documentStoreApiConfiguration = documentStoreApiConfiguration;
		this.sourceApiConfiguration = sourceApiConfiguration;
		this.concordanceApiConfiguration=concordanceApiConfiguration;
        this.financialTimesBrand = new Brand(financialTimesBrandId);
        this.videoSiteConfig = videoSiteConfig;
        this.interactiveGraphicsWhiteList = interactiveGraphicsWhiteList;
    }

	public SourceApiEndpointConfiguration getSourceApiConfiguration() {
		return sourceApiConfiguration;
	}
	
	@NotNull
	public ConcordanceApiConfiguration getConcordanceApiConfiguration() {
		return concordanceApiConfiguration;
	}

	@NotNull
	public DocumentStoreApiConfiguration getDocumentStoreApiConfiguration() { return documentStoreApiConfiguration; }

    @NotNull
    public Brand getFinancialTimesBrand() {
        return financialTimesBrand;
    }

    @NotNull
    public List<VideoSiteConfiguration> getVideoSiteConfig() { return videoSiteConfig; }

    @NotNull
    public List<String> getInteractiveGraphicsWhitelist() {
        return interactiveGraphicsWhiteList;
    }
}
