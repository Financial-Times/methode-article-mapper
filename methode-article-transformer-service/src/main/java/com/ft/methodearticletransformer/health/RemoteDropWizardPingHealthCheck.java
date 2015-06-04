package com.ft.methodearticletransformer.health;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

/**
 * RemoteDropWizardPingHealthCheck
 *
 * @author Simon.Gibbs
 */
public class RemoteDropWizardPingHealthCheck extends AdvancedHealthCheck {

	private static final String EXPECTED_RESPONSE = "pong";
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDropWizardPingHealthCheck.class);

    private EndpointConfiguration endpointConfiguration;
	private final Client client;

	public RemoteDropWizardPingHealthCheck(String name, Client client, EndpointConfiguration endpointConfiguration) {
        super(name);
        this.endpointConfiguration = endpointConfiguration;
		this.client = client;
    }

	@Override
	protected AdvancedResult checkAdvanced() throws Exception {

    	URI pingUri = UriBuilder.fromPath("/__health")
                                .scheme("http")
                                .host(endpointConfiguration.getHost())
                                .port(endpointConfiguration.getAdminPort())
                                .build();

		ClientResponse response = null;
        try {
            response = client.resource(pingUri).get(ClientResponse.class);

            if(response.getStatus()!=200) {
                String message = String.format("Unexpected status : %s",response.getStatus());
                return reportUnhealthy(message);
            }

            String responseBody = response.getEntity(String.class);
            
            if (responseBody != null) {
            	responseBody = responseBody.trim();
            }

            if(!EXPECTED_RESPONSE.equals(responseBody)) {
                String message = String.format("Unexpected response : %s",responseBody) ;
                return reportUnhealthy(message);
            }

            return AdvancedResult.healthy();

        } catch (Throwable e) {
        	String message = getName() + ": " + "Exception during ping, " + e.getLocalizedMessage();
        	return reportUnhealthy(message);
        } finally {
			if (response != null) {
				response.close();
			}
		}

    }

    private AdvancedResult reportUnhealthy(String message) {
        LOGGER.warn(getName() + ": " + message);
        return AdvancedResult.error(this, message);
    }
    
    @Override
	protected String businessImpact() {
		return "Content being published by journalists created in Methode will not be available.";
	}

	@Override
	protected String panicGuideUrl() {
		return "https://sites.google.com/a/ft.com/dynamic-publishing-team/methode-article-transformer-panic-guide";
	}

	@Override
	protected int severity() {
		return 1;
	}

	@Override
	protected String technicalSummary() {
		return "Cannot ping Methode API. Check the service is up and running.";
	}

}