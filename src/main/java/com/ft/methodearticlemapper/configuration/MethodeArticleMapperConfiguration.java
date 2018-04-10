package com.ft.methodearticlemapper.configuration;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.bodyprocessing.richcontent.VideoSiteConfiguration;
import com.ft.platform.dropwizard.AppInfo;
import com.ft.platform.dropwizard.ConfigWithAppInfo;
import com.ft.platform.dropwizard.ConfigWithGTG;
import com.ft.platform.dropwizard.GTGConfig;

import io.dropwizard.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MethodeArticleMapperConfiguration extends Configuration implements ConfigWithAppInfo, ConfigWithGTG {
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
    private final Map<String,String> additionalNativeContentProperties;
    private final PropertySource lastModifiedSource;
    private final PropertySource txIdSource;
    private final String txIdPropertyName;
    private final String apiHost;
    private final String webUrlTemplate;
    private final String canonicalWebUrlTemplate;
    private final AppInfo appInfo;
    @JsonProperty
    private final GTGConfig gtgConfig= new GTGConfig();

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
                                             @JsonProperty("additionalNativeContentProperties") Map<String,String> additionalNativeContentProperties,
                                             @JsonProperty("lastModifiedSource") PropertySource lastModifiedSource,
                                             @JsonProperty("transactionIdSource") PropertySource txIdSource,
                                             @JsonProperty("transactionIdProperty") String txIdPropertyName,
                                             @JsonProperty("apiHost") String apiHost,
                                             @JsonProperty("webUrlTemplate") String webUrlTemplate,
                                             @JsonProperty("canonicalWebUrlTemplate") String canonicalWebUrlTemplate,
                                             @JsonProperty("appInfo") AppInfo appInfo) {

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
        this.additionalNativeContentProperties = Collections.unmodifiableMap(additionalNativeContentProperties);
        this.lastModifiedSource = lastModifiedSource;
        this.txIdSource = txIdSource;
        this.txIdPropertyName = txIdPropertyName;
        this.apiHost = apiHost;
        this.webUrlTemplate = webUrlTemplate;
        this.canonicalWebUrlTemplate = canonicalWebUrlTemplate;
        this.appInfo = appInfo;
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

    public Map<String,String> getAdditionalNativeContentProperties() {
        return additionalNativeContentProperties;
    }
    
    public PropertySource getLastModifiedSource() {
        return lastModifiedSource;
    }
    
    public PropertySource getTxIdSource() {
        return txIdSource;
    }
    
    public String getTxIdPropertyName() {
        return txIdPropertyName;
    }
    
    @NotNull
    public String getApiHost() {
        return apiHost;
    }

    @NotNull
    public String getCanonicalWebUrlTemplate() {
        return canonicalWebUrlTemplate;
    }

    @NotNull
    public String getWebUrlTemplate() {
        return webUrlTemplate;
    }

    @Override
    public AppInfo getAppInfo() {
        return appInfo;
    }

	@Override
	public GTGConfig getGtg() {
		return gtgConfig;
	}
}
