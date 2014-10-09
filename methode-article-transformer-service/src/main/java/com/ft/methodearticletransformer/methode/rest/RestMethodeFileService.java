package com.ft.methodearticletransformer.methode.rest;

import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ft.api.jaxrs.client.exceptions.ApiNetworkingException;
import com.ft.api.jaxrs.client.exceptions.RemoteApiException;
import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.configuration.AssetTypeRequestConfiguration;
import com.ft.methodearticletransformer.configuration.MethodeApiEndpointConfiguration;
import com.ft.methodearticletransformer.methode.MethodeApiUnavailableException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.methode.UnexpectedMethodeApiException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

public class RestMethodeFileService implements MethodeFileService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RestMethodeFileService.class);

	private final int numberOfAssetIdsPerAssetTypeRequest;
	private final Client jerseyClient;
	private final String apiHost;
	private final int apiPort;
	private ExecutorService executorService;


	public RestMethodeFileService(Environment environment, Client methodeApiClient, MethodeApiEndpointConfiguration methodeApiConfiguration) {
		jerseyClient = methodeApiClient;
		apiHost = methodeApiConfiguration.getEndpointConfiguration().getHost();
		apiPort = methodeApiConfiguration.getEndpointConfiguration().getPort();

		this.executorService = buildExecutorService(environment, methodeApiConfiguration.getAssetTypeRequestConfiguration());

		AssetTypeRequestConfiguration assetTypeRequestConfiguration = methodeApiConfiguration.getAssetTypeRequestConfiguration();
		if (assetTypeRequestConfiguration != null) {
			this.numberOfAssetIdsPerAssetTypeRequest = assetTypeRequestConfiguration.getNumberOfAssetIdsPerAssetTypeRequest();
		} else { // choose sensible defaults
			this.numberOfAssetIdsPerAssetTypeRequest = 2;
		}

	}

	@Override
    public EomFile fileByUuid(UUID uuid, String transactionId) {
		try {
			return findFileByUuid(uuid.toString(), transactionId);
		} catch (RemoteApiException rae) {
			switch (rae.getStatus()) {
				case 404:
					throw new MethodeFileNotFoundException(uuid);
				case 503:
					throw new MethodeApiUnavailableException(rae);
				default:
					throw new UnexpectedMethodeApiException(rae);
			}
		}
    }

	private EomFile findFileByUuid(String uuid, String transactionId) {
		final URI fileByUuidUri = fileUrlBuilder().build(uuid);
		LOGGER.debug("making GET request to methode api {}", fileByUuidUri);

		ClientResponse clientResponse;

		try {
			clientResponse = jerseyClient
					.resource(fileByUuidUri)
					.accept(MediaType.APPLICATION_JSON_TYPE)
					.header(TransactionIdUtils.TRANSACTION_ID_HEADER, transactionId)
					.get(ClientResponse.class);
		} catch (ClientHandlerException che) {
			Throwable cause = che.getCause();
			if(cause instanceof IOException) {
				throw new ApiNetworkingException(fileByUuidUri,"GET",che);
			}
			throw che;
		}

		int responseStatusCode = clientResponse.getStatus();
		int responseStatusFamily = responseStatusCode / 100;

		if (responseStatusFamily == 2) {
			// SUCCESS!
			return clientResponse.getEntity(new GenericType<EomFile>(){});
		}

		LOGGER.error("received a {} status code when making a GET request to {}", responseStatusCode, fileByUuidUri);
		ErrorEntity entity = null;
		try {
			entity = clientResponse.getEntity(ErrorEntity.class);
		} catch (Throwable t) {
			LOGGER.warn("Failed to parse ErrorEntity when handling API transaction failure",t);
		}
		throw new RemoteApiException(fileByUuidUri,"GET",responseStatusCode,entity);
	}

	@Override
    public Map<String, EomAssetType> assetTypes(Set<String> assetIds, String transactionId) {
        return findAssetTypes(assetIds, transactionId);
    }

	private Map<String, EomAssetType> findAssetTypes(Set<String> assetIdentifiers, String transactionId) {
		final URI assetTypeUri = UriBuilder.fromPath("asset-type")
				.scheme("http")
				.host(apiHost)
				.port(apiPort)
				.build();

		Map<String, EomAssetType> results = Maps.newHashMap();

		List<List<String>> partitionedAssetIdentifiers = Lists.partition(Lists.newArrayList(assetIdentifiers), numberOfAssetIdsPerAssetTypeRequest);

		List<Future<ClientResponse>> futures = Lists.newArrayList();
		for (List<String> slice: partitionedAssetIdentifiers) {
			futures.add( executorService.submit(new MakeAssetTypeRequestTask(slice, transactionId, assetTypeUri)));
		}

		for (Future<ClientResponse> future: futures) {
			try {
				Map<String, EomAssetType> resultsFromFuture = processResponse(future.get(), assetTypeUri);
				results.putAll(resultsFromFuture);
			} catch (InterruptedException e) {
				// Restore the interrupted status of the thread that was waiting on future.get()
				// to preserve evidence that the interrupt occurred for code higher up the call stack
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if(cause instanceof ClientHandlerException) {
					if (cause.getCause() instanceof IOException) {
						throw new ApiNetworkingException(assetTypeUri,"POST", cause);
					}
					throw (ClientHandlerException) cause;
				}
			} catch (RemoteApiException rae) {
			    switch (rae.getStatus()) {
                    case 503:
                        throw new MethodeApiUnavailableException(rae);
                    default:
                        throw new UnexpectedMethodeApiException(rae);
			    }
			}
		}

		return results;
	}

	private Map<String, EomAssetType> processResponse(ClientResponse clientResponse, URI assetTypeUri) {
		int responseStatusCode = clientResponse.getStatus();
		int responseStatusFamily = responseStatusCode / 100;

		if (responseStatusFamily == 2) {
			return clientResponse.getEntity(new GenericType<Map<String, EomAssetType>>(){});
		}

		LOGGER.error("received a {} status code when making a POST request to {}", responseStatusCode, assetTypeUri);
		ErrorEntity entity = null;
		try {
			entity = clientResponse.getEntity(ErrorEntity.class);
		} catch (Throwable t) {
			LOGGER.warn("Failed to parse ErrorEntity when handling API transaction failure",t);
		}
		throw new RemoteApiException(assetTypeUri,"GET",responseStatusCode,entity);

	}

	private class MakeAssetTypeRequestTask implements Callable<ClientResponse> {

		private List<String> assetIdentifiers;
		private String transactionId;
		private URI assetTypeUri;

		public MakeAssetTypeRequestTask(List<String> assetIdentifiers, String transactionId,
										final URI assetTypeUri) {
			this.assetIdentifiers = assetIdentifiers;
			this.transactionId = transactionId;
			this.assetTypeUri = assetTypeUri;
		}

		@Override
		public ClientResponse call() throws Exception {
			LOGGER.debug("making POST request to methode api {}", assetTypeUri);

			return jerseyClient
					.resource(assetTypeUri)
					.accept(MediaType.APPLICATION_JSON_TYPE)
					.header("Content-Type", MediaType.APPLICATION_JSON_TYPE)
					.header(TransactionIdUtils.TRANSACTION_ID_HEADER, transactionId)
					.post(ClientResponse.class, assetIdentifiers);
		}

	}

	private UriBuilder fileUrlBuilder() {

		return UriBuilder.fromPath("eom-file")
				.path("{uuid}")
				.scheme("http")
				.host(apiHost)
				.port(apiPort);
	}

	private static ExecutorService buildExecutorService(Environment environment, AssetTypeRequestConfiguration requestConfiguration) {

		int numberOfParallelAssetTypeRequests = 4;
		if(requestConfiguration!=null) {
			numberOfParallelAssetTypeRequests = requestConfiguration.getNumberOfParallelAssetTypeRequests();
		}

		return environment.lifecycle().executorService("MAPI-worker-%d")
				.minThreads(numberOfParallelAssetTypeRequests)
				.maxThreads(numberOfParallelAssetTypeRequests)
				.keepAliveTime(Duration.minutes(2))
				.build();
	}
}
