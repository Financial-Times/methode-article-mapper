package com.ft.methodearticletransformer;

import java.net.URI;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.ws.rs.core.UriBuilder;

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
import com.ft.methodearticletransformer.configuration.ConcordanceApiConfiguration;
import com.ft.methodearticletransformer.configuration.ConnectionConfiguration;
import com.ft.methodearticletransformer.configuration.DocumentStoreApiConfiguration;
import com.ft.methodearticletransformer.configuration.SourceApiEndpointConfiguration;
import com.ft.methodearticletransformer.configuration.MethodeArticleTransformerConfiguration;
import com.ft.methodearticletransformer.health.RemoteDropWizardPingHealthCheck;
import com.ft.methodearticletransformer.methode.MethodeArticleTransformerErrorEntityFactory;
import com.ft.methodearticletransformer.methode.ContentSourceService;
import com.ft.methodearticletransformer.methode.rest.RestContentSourceService;
import com.ft.methodearticletransformer.resources.PostContentToTransformResource;
import com.ft.methodearticletransformer.resources.GetTransformedContentResource;
import com.ft.methodearticletransformer.transformation.BodyProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.BylineProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.EomFileProcessor;
import com.ft.methodearticletransformer.transformation.InteractiveGraphicsMatcher;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.sun.jersey.api.client.Client;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class MethodeArticleTransformerApplication extends Application<MethodeArticleTransformerConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MethodeArticleTransformerApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<MethodeArticleTransformerConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
    }

    @Override
    public void run(final MethodeArticleTransformerConfiguration configuration, final Environment environment) throws Exception {
        org.slf4j.LoggerFactory.getLogger(MethodeArticleTransformerApplication.class)
            .info("JVM file.encoding = {}", System.getProperty("file.encoding"));
        
    	environment.servlets().addFilter("transactionIdFilter", new TransactionIdFilter())
    		.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/content/*", "/content-transform/*");

    	BuildInfoResource buildInfoResource = new BuildInfoResource();
    	environment.jersey().register(buildInfoResource);

        DocumentStoreApiConfiguration documentStoreApiConfiguration = configuration.getDocumentStoreApiConfiguration();
        ResilientClient documentStoreApiClient = (ResilientClient) configureResilientClient(environment, documentStoreApiConfiguration.getEndpointConfiguration(), documentStoreApiConfiguration.getConnectionConfig());
    	SourceApiEndpointConfiguration sourceApiEndpointConfiguration = configuration.getSourceApiConfiguration();
        Client sourceApiClient = configureResilientClient(environment, sourceApiEndpointConfiguration.getEndpointConfiguration(), sourceApiEndpointConfiguration.getConnectionConfiguration());
        
        EndpointConfiguration documentStoreApiEndpointConfiguration = documentStoreApiConfiguration.getEndpointConfiguration();
        UriBuilder documentStoreApiBuilder = UriBuilder.fromPath(documentStoreApiEndpointConfiguration.getPath()).scheme("http").host(documentStoreApiEndpointConfiguration.getHost()).port(documentStoreApiEndpointConfiguration.getPort());
        URI documentStoreUri = documentStoreApiBuilder.build();
        ContentSourceService contentSourceService = new RestContentSourceService(environment, sourceApiClient, sourceApiEndpointConfiguration);
        
        ConcordanceApiConfiguration concordanceApiConfiguration=configuration.getConcordanceApiConfiguration();
        Client concordanceApiClient = configureResilientClient(environment, concordanceApiConfiguration.getEndpointConfiguration(), concordanceApiConfiguration.getConnectionConfiguration());
        EndpointConfiguration concordanceApiEndpointConfiguration = concordanceApiConfiguration.getEndpointConfiguration();
        UriBuilder concordanceApiBuilder = UriBuilder.fromPath(concordanceApiEndpointConfiguration.getPath()).scheme("http").host(concordanceApiEndpointConfiguration.getHost()).port(concordanceApiEndpointConfiguration.getPort());
        URI concordanceUri = concordanceApiBuilder.build();

        EomFileProcessor eomFileProcessor = configureEomFileProcessorForContentStore(
                documentStoreApiClient,
                documentStoreUri,
                configuration.getFinancialTimesBrand(), 
                configuration,
                concordanceApiClient,
                concordanceUri
        );

        environment.jersey().register(
        		
                new GetTransformedContentResource(
                        contentSourceService,
                        eomFileProcessor
                )
        );
        environment.jersey().register(
                new PostContentToTransformResource(
                        eomFileProcessor
                )
        );
        
        environment.healthChecks().register("ContentSourceService API ping", new RemoteDropWizardPingHealthCheck(
                "contentSourceService api ping",
                sourceApiClient,
        		sourceApiEndpointConfiguration.getEndpointConfiguration())
        );
        environment.jersey().register(RuntimeExceptionMapper.class);
        Errors.customise(new MethodeArticleTransformerErrorEntityFactory());
    }

    private Client configureResilientClient(Environment environment, EndpointConfiguration endpointConfiguration, ConnectionConfiguration connectionConfig) {
        return  ResilientClientBuilder.in(environment)
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
            final Brand financialTimesBrand,
            final MethodeArticleTransformerConfiguration configuration,
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
                financialTimesBrand);
	}
}
