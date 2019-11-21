package com.ft.methodearticlemapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.UriBuilder;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.ft.api.jaxrs.errors.Errors;
import com.ft.api.jaxrs.errors.RuntimeExceptionMapper;
import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.bodyprocessing.html.Html5SelfClosingTagBodyProcessor;
import com.ft.bodyprocessing.richcontent.VideoMatcher;
import com.ft.content.model.Brand;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.jerseyhttpwrapper.continuation.ExponentialBackoffContinuationPolicy;
import com.ft.message.consumer.MessageListener;
import com.ft.message.consumer.MessageQueueConsumerInitializer;
import com.ft.messagequeueproducer.MessageProducer;
import com.ft.messagequeueproducer.QueueProxyProducer;
import com.ft.methodearticlemapper.configuration.BrandConfiguration;
import com.ft.methodearticlemapper.configuration.ConcordanceApiConfiguration;
import com.ft.methodearticlemapper.configuration.ConnectionConfiguration;
import com.ft.methodearticlemapper.configuration.ConsumerConfiguration;
import com.ft.methodearticlemapper.configuration.DocumentStoreApiConfiguration;
import com.ft.methodearticlemapper.configuration.MethodeArticleMapperConfiguration;
import com.ft.methodearticlemapper.configuration.ProducerConfiguration;
import com.ft.methodearticlemapper.exception.ConfigurationException;
import com.ft.methodearticlemapper.health.CanConnectToMessageQueueProducerProxyHealthcheck;
import com.ft.methodearticlemapper.health.RemoteServiceHealthCheck;
import com.ft.methodearticlemapper.health.StandaloneHealthCheck;
import com.ft.methodearticlemapper.messaging.MessageBuilder;
import com.ft.methodearticlemapper.messaging.MessageProducingArticleMapper;
import com.ft.methodearticlemapper.messaging.NativeCmsPublicationEventsListener;
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.methodearticlemapper.methode.MethodeArticleTransformerErrorEntityFactory;
import com.ft.methodearticlemapper.model.EomFile;
import com.ft.methodearticlemapper.resources.PostContentToTransformResource;
import com.ft.methodearticlemapper.transformation.BodyProcessingFieldTransformerFactory;
import com.ft.methodearticlemapper.transformation.BylineProcessingFieldTransformerFactory;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.InteractiveGraphicsMatcher;
import com.ft.methodearticlemapper.transformation.TransformationMode;
import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.platform.dropwizard.DefaultGoodToGoChecker;
import com.ft.platform.dropwizard.GoodToGoConfiguredBundle;
import com.sun.jersey.api.client.Client;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class MethodeArticleMapperApplication extends Application<MethodeArticleMapperConfiguration> {
    private static final String DEWEY_URL = "https://dewey.ft.com/up-mam.html";
    
    public static void main(final String[] args) throws Exception {
        new MethodeArticleMapperApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<MethodeArticleMapperConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
        bootstrap.addBundle(new GoodToGoConfiguredBundle(new DefaultGoodToGoChecker()));
    }

    @Override
    public void run(final MethodeArticleMapperConfiguration configuration, final Environment environment) throws Exception {
        org.slf4j.LoggerFactory.getLogger(MethodeArticleMapperApplication.class)
                .info("JVM file.encoding = {}", System.getProperty("file.encoding"));

        environment.servlets().addFilter("transactionIdFilter", new TransactionIdFilter())
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/content-transform/*", "/map");

        BuildInfoResource buildInfoResource = new BuildInfoResource();
        environment.jersey().register(buildInfoResource);

        List<AdvancedHealthCheck> healthchecks = new ArrayList<>();
        
        Client documentStoreApiClient = null;
        URI documentStoreUri = null;
        if (configuration.isDocumentStoreApiEnabled()) {
            DocumentStoreApiConfiguration documentStoreApiConfiguration = configuration.getDocumentStoreApiConfiguration();
            EndpointConfiguration documentStoreApiEndpointConfiguration = documentStoreApiConfiguration.getEndpointConfiguration();
            documentStoreApiClient = configureResilientClient(environment, documentStoreApiEndpointConfiguration, documentStoreApiConfiguration.getConnectionConfig());
            
            UriBuilder documentStoreApiBuilder = UriBuilder.fromPath(documentStoreApiEndpointConfiguration.getPath()).scheme("http").host(documentStoreApiEndpointConfiguration.getHost()).port(documentStoreApiEndpointConfiguration.getPort());
            documentStoreUri = documentStoreApiBuilder.build();

            healthchecks.add(buildDocumentStoreAPIHealthCheck(documentStoreApiClient, documentStoreApiEndpointConfiguration));
        }
        
        Client concordanceApiClient = null;
        URI concordanceUri = null;
        if (configuration.isConcordanceApiEnabled()) {
            ConcordanceApiConfiguration concordanceApiConfiguration = configuration.getConcordanceApiConfiguration();
            EndpointConfiguration concordanceApiEndpointConfiguration = concordanceApiConfiguration.getEndpointConfiguration();
            concordanceApiClient = configureResilientClient(environment, concordanceApiEndpointConfiguration, concordanceApiConfiguration.getConnectionConfiguration());
            UriBuilder concordanceApiBuilder = UriBuilder.fromPath(concordanceApiEndpointConfiguration.getPath()).scheme("http").host(concordanceApiEndpointConfiguration.getHost()).port(concordanceApiEndpointConfiguration.getPort());
            concordanceUri = concordanceApiBuilder.build();
            
            healthchecks.add(buildConcordanceAPIHealthCheck(concordanceApiClient, concordanceApiEndpointConfiguration));
        }
        
        EomFile.setAdditionalMappings(configuration.getAdditionalNativeContentProperties());
        
        EomFileProcessor eomFileProcessor = configureEomFileProcessorForContentStore(
                documentStoreApiClient,
                documentStoreUri,
                configuration,
                concordanceApiClient,
                concordanceUri
        );

        if (configuration.isMessagingEndpointEnabled()) {
            ProducerConfiguration producerConfig = configuration.getProducerConfiguration();
            Client producerClient = getMessagingClient(environment, producerConfig.getJerseyClientConfiguration(), "producer-client");
            QueueProxyProducer.BuildNeeded queueProxyBuilder = QueueProxyProducer.builder()
                    .withJerseyClient(producerClient)
                    .withQueueProxyConfiguration(producerConfig.getMessageQueueProducerConfiguration());

            MessageProducer producer = queueProxyBuilder.build();
//            healthchecks.add(buildProducerHealthCheck(environment, producerConfig, queueProxyBuilder));
            
            MessageProducingArticleMapper msgProducingListMapper = new MessageProducingArticleMapper(
                    getMessageBuilder(configuration, environment),
                    producer, eomFileProcessor);
            
            ConsumerConfiguration consumerConfig = configuration.getConsumerConfiguration();
            MessageListener listener = new NativeCmsPublicationEventsListener(
                    environment.getObjectMapper(),
                    msgProducingListMapper,
                    consumerConfig.getSystemCode()
                    );
            
//            healthchecks.add(
                    registerListener(environment, listener, consumerConfig,
                            getMessagingClient(environment, consumerConfig.getJerseyClientConfiguration(), "consumer-client")
                            );
//                    );
        }

        environment.jersey().register(
                new PostContentToTransformResource(
                        eomFileProcessor, configuration.getLastModifiedSource(), configuration.getTxIdSource(), configuration.getTxIdPropertyName()
                )
        );
        
        if (healthchecks.isEmpty()) {
            // nothing to check, but prevent alarming startup messages
            healthchecks.add(new StandaloneHealthCheck(DEWEY_URL));
        }
        
        registerHealthChecks(environment, healthchecks);

        environment.jersey().register(RuntimeExceptionMapper.class);
        Errors.customise(new MethodeArticleTransformerErrorEntityFactory());
    }

    private Client configureResilientClient(Environment environment, EndpointConfiguration endpointConfiguration, ConnectionConfiguration connectionConfig) {
        return ResilientClientBuilder.in(environment)
                .using(endpointConfiguration)
                .withContinuationPolicy(
                        new ExponentialBackoffContinuationPolicy(
                                connectionConfig.getNumberOfConnectionAttempts(),
                                connectionConfig.getTimeoutMultiplier()
                        )
                ).withTransactionPropagation()
                .build();
    }

    private EomFileProcessor configureEomFileProcessorForContentStore(
            final Client documentStoreApiClient,
            final URI documentStoreUri,
            final MethodeArticleMapperConfiguration configuration,
            final Client concordanceApiClient,
            final URI concordanceUri) {
        
        EnumSet<TransformationMode> supportedModes;
        if ((documentStoreApiClient != null) && (concordanceApiClient != null)) {
            supportedModes = EnumSet.allOf(TransformationMode.class);
        } else {
            supportedModes = EnumSet.of(TransformationMode.SUGGEST);
        }
        
        return new EomFileProcessor(
                supportedModes,
                new BodyProcessingFieldTransformerFactory(documentStoreApiClient,
                        documentStoreUri,
                        new VideoMatcher(configuration.getVideoSiteConfig()),
                        new InteractiveGraphicsMatcher(configuration.getInteractiveGraphicsWhitelist()),
                        concordanceApiClient,
                        concordanceUri,
                        configuration.getCanonicalWebUrlTemplate()
                ).newInstance(),
                new BylineProcessingFieldTransformerFactory().newInstance(),
                new Html5SelfClosingTagBodyProcessor(),
                processConfigurationBrands(configuration.getBrandsConfiguration()),
                configuration.getTxIdPropertyName(),
                configuration.getApiHost(),
                configuration.getWebUrlTemplate(),
                configuration.getCanonicalWebUrlTemplate());
    }

    private void registerHealthChecks(Environment environment, List<AdvancedHealthCheck> advancedHealthChecks) {
        HealthCheckRegistry healthChecks = environment.healthChecks();
        for (AdvancedHealthCheck hc : advancedHealthChecks) {
            healthChecks.register(hc.getName(), hc);
        }
    }

    private AdvancedHealthCheck buildConcordanceAPIHealthCheck(Client concordanceApiClient, EndpointConfiguration concordanceApiConfiguration) {
        return new RemoteServiceHealthCheck(
                "Public Concordance API",
                concordanceApiClient,
                concordanceApiConfiguration.getHost(),
                concordanceApiConfiguration.getPort(),
                "/__gtg",
                "public-concordances-api",
                2,
                "Articles will not be annotated with company tearsheet information.",
                DEWEY_URL);
    }
    
    private AdvancedHealthCheck buildDocumentStoreAPIHealthCheck(Client documentStoreApiClient, EndpointConfiguration documentStoreApiEndpointConfiguration) {
        return new RemoteServiceHealthCheck(
                "Document Store API",
                documentStoreApiClient,
                documentStoreApiEndpointConfiguration.getHost(),
                documentStoreApiEndpointConfiguration.getPort(),
                "/__health",
                "document-store-api",
                2,
                "Clients will be unable to query the content service using alternative identifiers.",
                DEWEY_URL);
    }

    private MessageBuilder getMessageBuilder(MethodeArticleMapperConfiguration configuration, Environment environment) {
        return new MessageBuilder(
                UriBuilder.fromUri(configuration.getContentUriPrefix()).path("{uuid}"),
                configuration.getConsumerConfiguration().getSystemCode(),
                environment.getObjectMapper(),
                configuration.getContentTypeMappings()
        );
    }

    private Client getMessagingClient(Environment environment, JerseyClientConfiguration jerseyConfig, String name) {
        jerseyConfig.setGzipEnabled(false);
        jerseyConfig.setGzipEnabledForRequests(false);

        return ResilientClientBuilder.in(environment)
                .using(jerseyConfig)
                .usingDNS()
                .named(name)
                .build();
    }

    protected AdvancedHealthCheck registerListener(Environment environment, MessageListener listener, ConsumerConfiguration config, Client consumerClient) {
        final MessageQueueConsumerInitializer messageQueueConsumerInitializer =
                new MessageQueueConsumerInitializer(
                        config.getMessageQueueConsumerConfiguration(),
                        listener,
                        consumerClient
                );
        environment.lifecycle().manage(messageQueueConsumerInitializer);

        return buildConsumerHealthCheck(environment, config, messageQueueConsumerInitializer);
    }

    private AdvancedHealthCheck buildProducerHealthCheck(Environment environment, ProducerConfiguration config, QueueProxyProducer.BuildNeeded queueProxyBuilder) {
        return new CanConnectToMessageQueueProducerProxyHealthcheck(
                queueProxyBuilder.buildHealthcheck(),
                config.getHealthcheckConfiguration(),
                environment.metrics()
        );
    }
    
    private AdvancedHealthCheck buildConsumerHealthCheck(Environment environment, ConsumerConfiguration config, MessageQueueConsumerInitializer messageQueueConsumerInitializer) {
        return messageQueueConsumerInitializer.buildPassiveConsumerHealthcheck(
                config.getHealthcheckConfiguration(), environment.metrics()
        );
    }

    private Map<ContentSource, Brand> processConfigurationBrands(List<BrandConfiguration> brands) {
        Map<ContentSource, Brand> contentSourceBrandMap = new HashMap<>();
        for (BrandConfiguration brandConfiguration : brands) {
            ContentSource contentSource = ContentSource.valueOf(brandConfiguration.getName());
            contentSourceBrandMap.put(contentSource, new Brand(brandConfiguration.getId()));
        }

        validateBrandsConfiguration(contentSourceBrandMap);

        return contentSourceBrandMap;
    }

    private void validateBrandsConfiguration(Map<ContentSource, Brand> contentSourceBrandMap) {
        for (ContentSource contentSource : ContentSource.values()) {
            if (!contentSourceBrandMap.containsKey(contentSource)) {
                throw new ConfigurationException(
                        "No brand information configured for source with name: " + contentSource.name());
            }
        }
    }
}
