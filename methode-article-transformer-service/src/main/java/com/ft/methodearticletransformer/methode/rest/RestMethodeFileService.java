package com.ft.methodearticletransformer.methode.rest;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.ft.api.jaxrs.client.exceptions.RemoteApiException;
import com.ft.methodeapi.client.MethodeApiClient;
import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.methode.MethodeApiUnavailableException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.methode.UnexpectedMethodeApiException;

public class RestMethodeFileService implements MethodeFileService {

    private final MethodeApiClient restClient;

    public RestMethodeFileService(MethodeApiClient methodeApiClient) {
		restClient = methodeApiClient;
	}

	@Override
    public EomFile fileByUuid(UUID uuid, String transactionId) {
        try {
            return restClient.findFileByUuid(uuid.toString(), transactionId);
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

    @Override
    public Map<String, EomAssetType> assetTypes(Set<String> assetIds, String transactionId) {
        return restClient.findAssetTypes(assetIds, transactionId);
    }


}
