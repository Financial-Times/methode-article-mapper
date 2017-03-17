package com.ft.methodearticlemapper;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.ft.api.jaxrs.errors.Errors;
import com.ft.api.jaxrs.errors.RuntimeExceptionMapper;
import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.bodyprocessing.richcontent.VideoMatcher;
import com.ft.content.model.Brand;
import com.ft.jerseyhttpwrapper.ResilientClient;
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
import com.ft.methodearticlemapper.messaging.MessageBuilder;
import com.ft.methodearticlemapper.messaging.MessageProducingArticleMapper;
import com.ft.methodearticlemapper.messaging.NativeCmsPublicationEventsListener;
import com.ft.methodearticlemapper.methode.ContentSource;
import com.ft.methodearticlemapper.methode.MethodeArticleTransformerErrorEntityFactory;
import com.ft.methodearticlemapper.resources.PostContentToTransformResource;
import com.ft.methodearticlemapper.transformation.BodyProcessingFieldTransformerFactory;
import com.ft.methodearticlemapper.transformation.BylineProcessingFieldTransformerFactory;
import com.ft.methodearticlemapper.transformation.EomFileProcessor;
import com.ft.methodearticlemapper.transformation.InteractiveGraphicsMatcher;
import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.platform.dropwizard.DefaultGoodToGoChecker;
import com.ft.platform.dropwizard.GoodToGoBundle;
import com.sun.jersey.api.client.Client;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.UriBuilder;

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class MethodeArticleMapperApplication extends Application<MethodeArticleMapperConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MethodeArticleMapperApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<MethodeArticleMapperConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
        bootstrap.addBundle(new GoodToGoBundle(new DefaultGoodToGoChecker()));
    }

    @Override
    public void run(final MethodeArticleMapperConfiguration configuration, final Environment environment) throws Exception {
        org.slf4j.LoggerFactory.getLogger(MethodeArticleMapperApplication.class)
                .info("JVM file.encoding = {}", System.getProperty("file.encoding"));

        environment.servlets().addFilter("transactionIdFilter", new TransactionIdFilter())
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/content-transform/*", "/map");

        BuildInfoResource buildInfoResource = new BuildInfoResource();
        environment.jersey().register(buildInfoResource);

        DocumentStoreApiConfiguration documentStoreApiConfiguration = configuration.getDocumentStoreApiConfiguration();
        ResilientClient documentStoreApiClient = (ResilientClient) configureResilientClient(environment, documentStoreApiConfiguration.getEndpointConfiguration(), documentStoreApiConfiguration.getConnectionConfig());

        EndpointConfiguration documentStoreApiEndpointConfiguration = documentStoreApiConfiguration.getEndpointConfiguration();
        UriBuilder documentStoreApiBuilder = UriBuilder.fromPath(documentStoreApiEndpointConfiguration.getPath()).scheme("http").host(documentStoreApiEndpointConfiguration.getHost()).port(documentStoreApiEndpointConfiguration.getPort());
        URI documentStoreUri = documentStoreApiBuilder.build();

        ConcordanceApiConfiguration concordanceApiConfiguration = configuration.getConcordanceApiConfiguration();
        Client concordanceApiClient = configureResilientClient(environment, concordanceApiConfiguration.getEndpointConfiguration(), concordanceApiConfiguration.getConnectionConfiguration());
        EndpointConfiguration concordanceApiEndpointConfiguration = concordanceApiConfiguration.getEndpointConfiguration();
        UriBuilder concordanceApiBuilder = UriBuilder.fromPath(concordanceApiEndpointConfiguration.getPath()).scheme("http").host(concordanceApiEndpointConfiguration.getHost()).port(concordanceApiEndpointConfiguration.getPort());
        URI concordanceUri = concordanceApiBuilder.build();

        EomFileProcessor eomFileProcessor = configureEomFileProcessorForContentStore(
                documentStoreApiClient,
                documentStoreUri,
                configuration,
                concordanceApiClient,
                concordanceUri
        );

        ConsumerConfiguration consumerConfig = configuration.getConsumerConfiguration();
        MessageProducingArticleMapper msgProducingListMapper = new MessageProducingArticleMapper(
                getMessageBuilder(configuration, environment),
                configureMessageProducer(configuration.getProducerConfiguration(), environment),
                eomFileProcessor
        );
        MessageListener listener = new NativeCmsPublicationEventsListener(
                environment.getObjectMapper(),
                msgProducingListMapper,
                consumerConfig.getSystemCode()
        );
        registerListener(environment, listener, consumerConfig, getConsumerClient(environment, consumerConfig));

        registerHealthChecks(
                environment,
                buildClientHealthChecks(
                        concordanceApiClient, concordanceApiConfiguration.getEndpointConfiguration(),
                        documentStoreApiClient, documentStoreApiConfiguration.getEndpointConfiguration()
                )
        );

        environment.jersey().register(
                new PostContentToTransformResource(
                        eomFileProcessor
                )
        );

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
            final ResilientClient documentStoreApiClient,
            final URI documentStoreUri,
            final MethodeArticleMapperConfiguration configuration,
            final Client concordanceApiClient,
            final URI concordanceUri) {
        return new EomFileProcessor(
                new BodyProcessingFieldTransformerFactory(documentStoreApiClient,
                        documentStoreUri,
                        new VideoMatcher(configuration.getVideoSiteConfig()),
                        new InteractiveGraphicsMatcher(configuration.getInteractiveGraphicsWhitelist()),
                        concordanceApiClient,
                        concordanceUri
                ).newInstance(),
                new BylineProcessingFieldTransformerFactory().newInstance(),
                processConfigurationBrands(configuration.getBrandsConfiguration()));
    }

    private void registerHealthChecks(Environment environment, List<AdvancedHealthCheck> advancedHealthChecks) {
        HealthCheckRegistry healthChecks = environment.healthChecks();
        for (AdvancedHealthCheck hc : advancedHealthChecks) {
            healthChecks.register(hc.getName(), hc);
        }
    }

    private List<AdvancedHealthCheck> buildClientHealthChecks(
            Client concordanceApiClient, EndpointConfiguration concordanceApiConfiguration,
            ResilientClient documentStoreApiClient, EndpointConfiguration documentStoreApiEndpointConfiguration) {

        List<AdvancedHealthCheck> healthchecks = new ArrayList<>();
        healthchecks.add(new RemoteServiceHealthCheck(
                "Public Concordance API",
                concordanceApiClient,
                concordanceApiConfiguration.getHost(),
                concordanceApiConfiguration.getPort(),
                "/__gtg",
                "public-concordances-api",
                1,
                "Articles will not be annotated with company tearsheet information.",
                "https://dewey.ft.com/up-mam.html")
        );
        healthchecks.add(new RemoteServiceHealthCheck(
                "Document Store API",
                documentStoreApiClient,
                documentStoreApiEndpointConfiguration.getHost(),
                documentStoreApiEndpointConfiguration.getPort(),
                "/__health",
                "document-store-api",
                1,
                "Clients will be unable to query the content service using alternative identifiers.",
                "https://dewey.ft.com/up-mam.html")
        );
        return healthchecks;
    }

    private MessageBuilder getMessageBuilder(MethodeArticleMapperConfiguration configuration, Environment environment) {
        return new MessageBuilder(
                UriBuilder.fromUri(configuration.getContentUriPrefix()).path("{uuid}"),
                configuration.getConsumerConfiguration().getSystemCode(),
                environment.getObjectMapper()
        );
    }

    private Client getConsumerClient(Environment environment, ConsumerConfiguration config) {
        JerseyClientConfiguration jerseyConfig = config.getJerseyClientConfiguration();
        jerseyConfig.setGzipEnabled(false);
        jerseyConfig.setGzipEnabledForRequests(false);

        return ResilientClientBuilder.in(environment)
                .using(jerseyConfig)
                .usingDNS()
                .named("consumer-client")
                .build();
    }

    protected MessageProducer configureMessageProducer(ProducerConfiguration config, Environment environment) {
        JerseyClientConfiguration jerseyConfig = config.getJerseyClientConfiguration();
        jerseyConfig.setGzipEnabled(false);
        jerseyConfig.setGzipEnabledForRequests(false);

        Client producerClient = ResilientClientBuilder.in(environment)
                .using(jerseyConfig)
                .usingDNS()
                .named("producer-client")
                .build();

        final QueueProxyProducer.BuildNeeded queueProxyBuilder = QueueProxyProducer.builder()
                .withJerseyClient(producerClient)
                .withQueueProxyConfiguration(config.getMessageQueueProducerConfiguration());

        final QueueProxyProducer producer = queueProxyBuilder.build();

        registerProducerHealthCheck(environment, config, queueProxyBuilder);

        return producer;
    }

    protected void registerListener(Environment environment, MessageListener listener, ConsumerConfiguration config, Client consumerClient) {
        final MessageQueueConsumerInitializer messageQueueConsumerInitializer =
                new MessageQueueConsumerInitializer(
                        config.getMessageQueueConsumerConfiguration(),
                        listener,
                        consumerClient
                );
        environment.lifecycle().manage(messageQueueConsumerInitializer);

        registerConsumerHealthCheck(environment, config, messageQueueConsumerInitializer);
    }

    private void registerProducerHealthCheck(Environment environment, ProducerConfiguration config, QueueProxyProducer.BuildNeeded queueProxyBuilder) {
        environment.healthChecks().register("KafkaProxyProducer",
                new CanConnectToMessageQueueProducerProxyHealthcheck(
                        queueProxyBuilder.buildHealthcheck(),
                        config.getHealthcheckConfiguration(),
                        environment.metrics()
                )
        );
    }

    private void registerConsumerHealthCheck(Environment environment, ConsumerConfiguration config, MessageQueueConsumerInitializer messageQueueConsumerInitializer) {
        environment.healthChecks().register("KafkaProxyConsumer",
                messageQueueConsumerInitializer.buildPassiveConsumerHealthcheck(
                        config.getHealthcheckConfiguration(), environment.metrics()
                )
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
