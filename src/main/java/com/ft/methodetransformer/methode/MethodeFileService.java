package com.ft.methodetransformer.methode;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodeapi.model.EomFile;

public interface MethodeFileService {

    EomFile fileByUuid(UUID uuid, String transactionId);

    Map<String, EomAssetType> assetTypes(Set<String> assetIds, String transactionId);
}
