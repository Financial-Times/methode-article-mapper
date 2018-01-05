package com.ft.methodearticlemapper.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import io.dropwizard.Configuration;

import java.util.List;

public class MethodeArticleMapperConfiguration extends Configuration {
	private boolean documentStoreApiEnabled;
	private DocumentStoreApiConfiguration documentStoreApiConfiguration;
    private boolean concordanceApiEnabled;
	private ConcordanceApiConfiguration concordanceApiConfiguration;
    private boolean messagingEndpointEnabled;
    private ConsumerConfiguration consumerConfiguration;
    private ProducerConfiguration producerConfiguration;
    private final List<BrandConfiguration> brands;
    private final List<VideoSiteConfiguration> videoSiteConfig;
    private final List<String> interactiveGraphicsWhiteList;
    private final String contentUriPrefix;
    private final String apiHost;

    public MethodeArticleMapperConfiguration(@JsonProperty("messagingEndpointEnabled") Boolean messagingEndpointEnabled,
                                             @JsonProperty("consumer") ConsumerConfiguration consumerConfiguration,
                                             @JsonProperty("producer") ProducerConfiguration producerConfiguration,
                                             @JsonProperty("concordanceApiEnabled") Boolean concordanceApiEnabled,
                                             @JsonProperty("concordanceApi") ConcordanceApiConfiguration concordanceApiConfiguration,
                                             @JsonProperty("documentStoreApiEnabled") Boolean documentStoreApiEnabled,
                                             @JsonProperty("documentStoreApi") DocumentStoreApiConfiguration documentStoreApiConfiguration,
                                             @JsonProperty("brands") List<BrandConfiguration> brands,
                                             @JsonProperty("videoSiteConfig") List<VideoSiteConfiguration> videoSiteConfig,
                                             @JsonProperty("interactiveGraphicsWhiteList") List<String> interactiveGraphicsWhiteList,
                                             @JsonProperty("contentUriPrefix") String contentUriPrefix,
                                             @JsonProperty("apiHost") String apiHost) {

        if ((documentStoreApiEnabled == null) || documentStoreApiEnabled.booleanValue()) {
            this.documentStoreApiEnabled = true;
            this.documentStoreApiConfiguration = documentStoreApiConfiguration;
        }
        
        if ((concordanceApiEnabled == null) || concordanceApiEnabled.booleanValue()) {
            this.concordanceApiEnabled = true;
            this.concordanceApiConfiguration=concordanceApiConfiguration;
        }
        
        this.brands = brands;
        
        if ((messagingEndpointEnabled == null) || messagingEndpointEnabled.booleanValue()) {
            this.messagingEndpointEnabled = true;
            this.consumerConfiguration = consumerConfiguration;
            this.producerConfiguration = producerConfiguration;
        }
        
        this.videoSiteConfig = videoSiteConfig;
        this.interactiveGraphicsWhiteList = interactiveGraphicsWhiteList;
        this.contentUriPrefix = contentUriPrefix;
        this.apiHost = apiHost;
    }
    
    public boolean isConcordanceApiEnabled() {
        return concordanceApiEnabled;
    }

	public ConcordanceApiConfiguration getConcordanceApiConfiguration() {
		return concordanceApiConfiguration;
	}
	
	public boolean isDocumentStoreApiEnabled() {
	    return documentStoreApiEnabled;
	}
	
	public DocumentStoreApiConfiguration getDocumentStoreApiConfiguration() {
	    return documentStoreApiConfiguration;
	}

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
    
    public boolean isMessagingEndpointEnabled() {
        return messagingEndpointEnabled;
    }

    public ConsumerConfiguration getConsumerConfiguration() {
        return consumerConfiguration;
    }

    public ProducerConfiguration getProducerConfiguration() {
        return producerConfiguration;
    }

    @NotNull
    public String getContentUriPrefix() {
        return contentUriPrefix;
    }

    @NotNull
    public String getApiHost() {
        return apiHost;
    }
}
