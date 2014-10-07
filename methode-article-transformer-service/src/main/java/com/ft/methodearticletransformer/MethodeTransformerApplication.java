package com.ft.methodearticletransformer;

import com.ft.methodearticletransformer.configuration.MethodeApiEndpointConfiguration;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.servlets.SlowRequestFilter;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.methodearticletransformer.configuration.MethodeTransformerConfiguration;
import com.ft.methodearticletransformer.health.RemoteDependencyHealthCheck;
import com.ft.methodearticletransformer.health.RemoteDropWizardPingHealthCheck;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.methode.rest.RestMethodeFileService;
import com.ft.methodearticletransformer.resources.MethodeTransformerResource;
import com.ft.methodearticletransformer.transformation.BodyProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.BylineProcessingFieldTransformerFactory;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;
import com.ft.mustachemods.SwitchableMustacheViewBundle;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.sun.jersey.api.client.Client;

public class MethodeTransformerApplication extends Application<MethodeTransformerConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MethodeTransformerApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<MethodeTransformerConfiguration> bootstrap) {
        bootstrap.addBundle(new SwitchableMustacheViewBundle());
        bootstrap.addBundle(new AssetsBundle("/views"));
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
    }

    @Override
    public void run(final MethodeTransformerConfiguration configuration, final Environment environment) throws Exception {
    	environment.servlets().addFilter("transactionIdFilter", new TransactionIdFilter())
    		.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/content/*");
    	
    	BuildInfoResource buildInfoResource = new BuildInfoResource();   	
    	environment.jersey().register(buildInfoResource);
    	
    	ResilientClient semanticReaderClient = configureContentReaderClient(environment, configuration.getSemanticContentStoreReaderConfiguration());
    	MethodeApiEndpointConfiguration methodeApiEndpointConfiguration = configuration.getMethodeApiConfiguration();
    	Client clientForMethodeApiClient = getClientForMethodeApiClient(environment, methodeApiEndpointConfiguration);
    	
        MethodeFileService methodeFileService = configureMethodeFileService(environment, clientForMethodeApiClient, methodeApiEndpointConfiguration);
        environment.jersey().register(new MethodeTransformerResource(methodeFileService, 
        		configureEomFileProcessorForContentStore(methodeFileService, semanticReaderClient)));

        environment.servlets().addFilter(
                "Slow Servlet Filter",
                new SlowRequestFilter(Duration.milliseconds(configuration.getSlowRequestTimeout()))).addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                false,
                configuration.getSlowRequestPattern());
        
        
        
        environment.healthChecks().register("MethodeAPI ping", new RemoteDropWizardPingHealthCheck("methode api ping", 
        		clientForMethodeApiClient, 
        		methodeApiEndpointConfiguration.getEndpointConfiguration()));
        environment.healthChecks().register("MethodeAPI version", new RemoteDependencyHealthCheck("methode api version", 
        		clientForMethodeApiClient, 
        		methodeApiEndpointConfiguration.getEndpointConfiguration(), buildInfoResource, "minimum.methode.api.version"));

    }

	private MethodeFileService configureMethodeFileService(Environment environment, Client clientForMethodeApiClient, MethodeApiEndpointConfiguration methodeApiEndpointConfiguration) {
        return new RestMethodeFileService(environment, clientForMethodeApiClient, methodeApiEndpointConfiguration);
	}

	private ResilientClient configureContentReaderClient(Environment environment, EndpointConfiguration semanticReaderEndpointConfiguration) {
		return ResilientClientBuilder.in(environment).using(semanticReaderEndpointConfiguration).build();
	}
	
	private Client getClientForMethodeApiClient(Environment environment, MethodeApiEndpointConfiguration methodeApiEndpointConfiguration) {
		return ResilientClientBuilder.in(environment).using(methodeApiEndpointConfiguration.getEndpointConfiguration()).build();
	}

	private EomFileProcessorForContentStore configureEomFileProcessorForContentStore(MethodeFileService methodeFileService, ResilientClient semanticStoreContentReaderClient) {
		return new EomFileProcessorForContentStore(
				new BodyProcessingFieldTransformerFactory(methodeFileService, semanticStoreContentReaderClient).newInstance(),
				new BylineProcessingFieldTransformerFactory().newInstance());
	}

}
