package com.ft.methodearticlemapper.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import com.ft.content.model.Brand;
import io.dropwizard.Configuration;

import java.util.List;

public class MethodeArticleMapperConfiguration extends Configuration {
	
	private final DocumentStoreApiConfiguration documentStoreApiConfiguration;
	private final SourceApiConfiguration sourceApiConfiguration;
	private final ConcordanceApiConfiguration concordanceApiConfiguration;
    private final ConsumerConfiguration consumerConfiguration;
    private final ProducerConfiguration producerConfiguration;
    private final Brand financialTimesBrand;
    private final List<VideoSiteConfiguration> videoSiteConfig;
    private final List<String> interactiveGraphicsWhiteList;
    private final String contentUriPrefix;

    public MethodeArticleMapperConfiguration(@JsonProperty("consumer") ConsumerConfiguration consumerConfiguration,
                                             @JsonProperty("producer") ProducerConfiguration producerConfiguration,
                                             @JsonProperty("concordanceApi") ConcordanceApiConfiguration concordanceApiConfiguration,
                                             @JsonProperty("sourceApi") SourceApiConfiguration sourceApiConfiguration,
                                             @JsonProperty("documentStoreApi") DocumentStoreApiConfiguration documentStoreApiConfiguration,
                                             @JsonProperty("financialTimesBrandId") String financialTimesBrandId,
                                             @JsonProperty("videoSiteConfig") List<VideoSiteConfiguration> videoSiteConfig,
                                             @JsonProperty("interactiveGraphicsWhiteList") List<String> interactiveGraphicsWhiteList,
                                             @JsonProperty("contentUriPrefix") String contentUriPrefix) {

        this.documentStoreApiConfiguration = documentStoreApiConfiguration;
		this.sourceApiConfiguration = sourceApiConfiguration;
		this.concordanceApiConfiguration=concordanceApiConfiguration;
        this.financialTimesBrand = new Brand(financialTimesBrandId);
        this.consumerConfiguration = consumerConfiguration;
        this.producerConfiguration = producerConfiguration;
        this.videoSiteConfig = videoSiteConfig;
        this.interactiveGraphicsWhiteList = interactiveGraphicsWhiteList;
        this.contentUriPrefix = contentUriPrefix;
    }

	public SourceApiConfiguration getSourceApiConfiguration() {
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

    @NotNull
    public ConsumerConfiguration getConsumerConfiguration() {
        return consumerConfiguration;
    }

    @NotNull
    public ProducerConfiguration getProducerConfiguration() {
        return producerConfiguration;
    }

    @NotNull
    public String getContentUriPrefix() {
        return contentUriPrefix;
    }
}
