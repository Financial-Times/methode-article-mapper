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
import com.ft.methodearticletransformer.configuration.ConnectionConfiguration;
import com.ft.methodearticletransformer.configuration.MethodeApiEndpointConfiguration;
import com.ft.methodearticletransformer.configuration.MethodeArticleTransformerConfiguration;
import com.ft.methodearticletransformer.configuration.SemanticReaderEndpointConfiguration;
import com.ft.methodearticletransformer.health.RemoteDependencyHealthCheck;
import com.ft.methodearticletransformer.health.RemoteDropWizardPingHealthCheck;
import com.ft.methodearticletransformer.methode.MethodeArticleTransformerErrorEntityFactory;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.methode.rest.RestMethodeFileService;
import com.ft.methodearticletransformer.resources.MethodeArticleTransformerResource;
import com.ft.methodearticletransformer.transformation.BodyProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.BylineProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;
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
    	environment.servlets().addFilter("transactionIdFilter", new TransactionIdFilter())
    		.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/content/*");

    	BuildInfoResource buildInfoResource = new BuildInfoResource();   	
    	environment.jersey().register(buildInfoResource);

        SemanticReaderEndpointConfiguration semanticReaderEndpointConfiguration = configuration.getSemanticReaderEndpointConfiguration();
        ResilientClient semanticReaderClient = (ResilientClient) configureResilientClient(environment, semanticReaderEndpointConfiguration.getEndpointConfiguration(), semanticReaderEndpointConfiguration.getConnectionConfig());
    	MethodeApiEndpointConfiguration methodeApiEndpointConfiguration = configuration.getMethodeApiConfiguration();
        Client clientForMethodeApiClient = configureResilientClient(environment, methodeApiEndpointConfiguration.getEndpointConfiguration(), methodeApiEndpointConfiguration.getConnectionConfiguration());
    	Client clientForMethodeApiClientOnAdminPort = getClientForMethodeApiClientOnAdminPort(environment, methodeApiEndpointConfiguration);

        VideoMatcher videoMatcher = new VideoMatcher(configuration.getVideoSiteConfig());

        EndpointConfiguration endpointConfiguration = semanticReaderEndpointConfiguration.getEndpointConfiguration();
        UriBuilder builder = UriBuilder.fromPath(endpointConfiguration.getPath()).scheme("http").host(endpointConfiguration.getHost()).port(endpointConfiguration.getPort());
        URI uri = builder.build();
        MethodeFileService methodeFileService = configureMethodeFileService(environment, clientForMethodeApiClient, methodeApiEndpointConfiguration);
        environment.jersey().register(new MethodeArticleTransformerResource(methodeFileService,
        		configureEomFileProcessorForContentStore(methodeFileService, semanticReaderClient, uri, configuration.getFinancialTimesBrand(), videoMatcher)));
        
        environment.healthChecks().register("MethodeAPI ping", new RemoteDropWizardPingHealthCheck("methode api ping",
                clientForMethodeApiClientOnAdminPort,
        		methodeApiEndpointConfiguration.getEndpointConfiguration()));
        environment.healthChecks().register("MethodeAPI version", new RemoteDependencyHealthCheck("methode api version", 
        		clientForMethodeApiClient, 
        		methodeApiEndpointConfiguration.getEndpointConfiguration(), buildInfoResource, "build.minimum.methode.api.version"));
        environment.jersey().register(RuntimeExceptionMapper.class);
        Errors.customise(new MethodeArticleTransformerErrorEntityFactory());

    }

	private MethodeFileService configureMethodeFileService(Environment environment, Client clientForMethodeApiClient, MethodeApiEndpointConfiguration methodeApiEndpointConfiguration) {
        return new RestMethodeFileService(environment, clientForMethodeApiClient, methodeApiEndpointConfiguration);
	}

    private Client configureResilientClient(Environment environment, EndpointConfiguration endpointConfiguration, ConnectionConfiguration connectionConfig) {
        return  ResilientClientBuilder.in(environment)
                .using(endpointConfiguration)
                .withContinuationPolicy(new ExponentialBackoffContinuationPolicy(connectionConfig.getNumberOfConnectionAttempts(), connectionConfig.getTimeoutMultiplier()))
                .build();
    }

	private Client getClientForMethodeApiClientOnAdminPort(Environment environment, MethodeApiEndpointConfiguration methodeApiEndpointConfiguration) {
		return ResilientClientBuilder.in(environment)
                .using(methodeApiEndpointConfiguration.getEndpointConfiguration())
                .withContinuationPolicy(new ExponentialBackoffContinuationPolicy(methodeApiEndpointConfiguration.getConnectionConfiguration().getNumberOfConnectionAttempts(), methodeApiEndpointConfiguration.getConnectionConfiguration().getTimeoutMultiplier()))
                .usingAdminPorts()
                .build();
	}

	private EomFileProcessorForContentStore configureEomFileProcessorForContentStore(MethodeFileService methodeFileService, ResilientClient semanticStoreContentReaderClient, URI uri, Brand financialTimesBrand, VideoMatcher videoMatcher) {
		return new EomFileProcessorForContentStore(
				new BodyProcessingFieldTransformerFactory(methodeFileService, semanticStoreContentReaderClient, uri, videoMatcher).newInstance(),
				new BylineProcessingFieldTransformerFactory().newInstance(),
                financialTimesBrand);
	}

}
