package com.ft.methodearticlemapper.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import io.dropwizard.Configuration;

import java.util.List;

public class MethodeArticleMapperConfiguration extends Configuration {
	
	private final DocumentStoreApiConfiguration documentStoreApiConfiguration;
	private final ConcordanceApiConfiguration concordanceApiConfiguration;
    private final ConsumerConfiguration consumerConfiguration;
    private final ProducerConfiguration producerConfiguration;
    private final List<BrandConfiguration> brands;
    private final List<VideoSiteConfiguration> videoSiteConfig;
    private final List<String> interactiveGraphicsWhiteList;
    private final String contentUriPrefix;

    public MethodeArticleMapperConfiguration(@JsonProperty("consumer") ConsumerConfiguration consumerConfiguration,
                                             @JsonProperty("producer") ProducerConfiguration producerConfiguration,
                                             @JsonProperty("concordanceApi") ConcordanceApiConfiguration concordanceApiConfiguration,
                                             @JsonProperty("documentStoreApi") DocumentStoreApiConfiguration documentStoreApiConfiguration,
                                             @JsonProperty("brands") List<BrandConfiguration> brands,
                                             @JsonProperty("videoSiteConfig") List<VideoSiteConfiguration> videoSiteConfig,
                                             @JsonProperty("interactiveGraphicsWhiteList") List<String> interactiveGraphicsWhiteList,
                                             @JsonProperty("contentUriPrefix") String contentUriPrefix) {

        this.documentStoreApiConfiguration = documentStoreApiConfiguration;
		this.concordanceApiConfiguration=concordanceApiConfiguration;
        this.brands = brands;
        this.consumerConfiguration = consumerConfiguration;
        this.producerConfiguration = producerConfiguration;
        this.videoSiteConfig = videoSiteConfig;
        this.interactiveGraphicsWhiteList = interactiveGraphicsWhiteList;
        this.contentUriPrefix = contentUriPrefix;
    }

	@NotNull
	public ConcordanceApiConfiguration getConcordanceApiConfiguration() {
		return concordanceApiConfiguration;
	}

	@NotNull
	public DocumentStoreApiConfiguration getDocumentStoreApiConfiguration() { return documentStoreApiConfiguration; }

   @NotNull
    public List<BrandConfiguration> getBrandsConfiguration() {
        return brands;
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
