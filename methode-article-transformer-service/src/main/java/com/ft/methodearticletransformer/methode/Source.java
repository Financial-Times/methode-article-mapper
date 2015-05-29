package com.ft.methodearticletransformer.methode;

import com.ft.methodearticletransformer.model.EomAssetType;
import com.ft.methodearticletransformer.model.EomFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface Source {

    EomFile fileByUuid(UUID uuid, String transactionId);

    Map<String, EomAssetType> assetTypes(Set<String> assetIds, String transactionId);
}
