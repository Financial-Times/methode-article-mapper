package com.ft.methodetransformer.health;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import com.ft.methodetransformer.http.VersionNumber;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class RemoteDependencyHealthCheck extends AdvancedHealthCheck {

	private Logger logger = LoggerFactory.getLogger(RemoteDependencyHealthCheck.class);

	private EndpointConfiguration endpointConfiguration;
	private final Client client;
	private BuildInfoResource buildInfoResource;
	private String dependencyVersionProperty;

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDependencyHealthCheck.class);

	public RemoteDependencyHealthCheck(String name, Client client, EndpointConfiguration endpointConfiguration, BuildInfoResource buildInfoResource, String dependencyVersionProperty) {
		super(name);
		this.endpointConfiguration = endpointConfiguration;
		this.buildInfoResource = buildInfoResource;
		this.dependencyVersionProperty = dependencyVersionProperty;
		this.client = client;
	}

	@Override
	protected AdvancedResult checkAdvanced() throws Exception {
		URI buildInfoUri = UriBuilder.fromPath("/build-info")
				.scheme("http")
				.host(endpointConfiguration.getHost())
				.port(endpointConfiguration.getPort())
				.build();

		ClientResponse response = null;
		try {
			response = client.resource(buildInfoUri).get(ClientResponse.class);

			if(response.getStatus()!=200) {
				logger.warn("Unexpected status [{}] from [{}]. Response body was: {}", response.getStatus(),
						endpointConfiguration.getHost(), response.getEntity(String.class));
				return AdvancedResult.error(this, "Unexpected status : " + response.getStatus());
			}

			@SuppressWarnings("unchecked") //trust me
			Map<String, Map<String, String>> responseMap = response.getEntity(Map.class);
			Map<String, String> buildInfo = responseMap.get("buildInfo");
			if (buildInfo == null) {
                String message = "Could not obtain build info.";
                LOGGER.warn(getName() + ": " + message);
				return AdvancedResult.error(this, message);
			}

			String deployedDependencyVersionString = buildInfo.get("artifact.version");
			if (StringUtils.isEmpty(deployedDependencyVersionString)) {
                String message = "Could not obtain deployed version info for " + endpointConfiguration.getShortName()
                        + " from the endpoint " + buildInfoUri.toString();
                LOGGER.warn(message);
				return AdvancedResult.error(this, message);
			}
			if (deployedDependencyVersionString.contains("SNAPSHOT")) {
				return AdvancedResult.healthy("SNAPSHOT dependency found - ignoring the check.");
			}

			String minimumDependencyVersionString = buildInfoResource.getBuildInfo().getProperty(dependencyVersionProperty);
			if (StringUtils.isEmpty(minimumDependencyVersionString)) {
                String message = "Could not obtain minimum version info for " + endpointConfiguration.getShortName()
                        + " from the buildInfoResource using " + dependencyVersionProperty;
                LOGGER.warn(message);
				return AdvancedResult.error(this, message);
			}

			VersionNumber deployedDependencyVersion = new VersionNumber(deployedDependencyVersionString);
			VersionNumber minimumDependencyVersion = new VersionNumber(minimumDependencyVersionString);

			if (deployedDependencyVersion.compareTo(minimumDependencyVersion) >= 0) {
				// Deployed higher than or equal to minimum.
				return AdvancedResult.healthy();
			} else {
				// Deployed less than minimum.
                String message = String.format("Incorrect dependency (%s), expected %s or higher, got %s",
                        endpointConfiguration.getShortName(), minimumDependencyVersionString, deployedDependencyVersionString);
                LOGGER.error(message);
				return AdvancedResult.error(this, message);
			}

		} catch (Throwable e) {
            LOGGER.error("Exception during dependency version check",e);
			return AdvancedResult.error(this, e);
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	@Override
	protected String businessImpact() {
		return "business impact";
	}

	@Override
	protected String panicGuideUrl() {
		return "http://mypanicguide.com";
	}

	@Override
	protected int severity() {
		return 0;
	}

	@Override
	protected String technicalSummary() {
		return "technical summary";
	}
}
