package com.ft.methodearticlemapper.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.message.consumer.config.HealthcheckConfiguration;
import com.ft.messagequeueproducer.QueueProxyConfiguration;

import io.dropwizard.client.JerseyClientConfiguration;

public class ProducerConfiguration {
    private final JerseyClientConfiguration jerseyConfig;
    private final QueueProxyConfiguration producerConfig;
    private final HealthcheckConfiguration healthcheckConfig;

    public ProducerConfiguration(@JsonProperty("jerseyClient") JerseyClientConfiguration jerseyConfig,
                                 @JsonProperty("messageProducer") QueueProxyConfiguration producerConfig,
                                 @JsonProperty("healthCheck") HealthcheckConfiguration healthcheckConfig) {

        this.jerseyConfig = jerseyConfig;
        this.producerConfig = producerConfig;
        this.healthcheckConfig = healthcheckConfig;
    }

    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return jerseyConfig;
    }

    public QueueProxyConfiguration getMessageQueueProducerConfiguration() {
        return producerConfig;
    }

    public HealthcheckConfiguration getHealthcheckConfiguration() {
        return healthcheckConfig;
    }
}
