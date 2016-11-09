package com.ft.methodearticlemapper.methode;

import com.ft.methodearticlemapper.model.EomAssetType;
import com.ft.methodearticlemapper.model.EomFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ContentSourceService {

    EomFile fileByUuid(UUID uuid, String transactionId);

    Map<String, EomAssetType> assetTypes(Set<String> assetIds, String transactionId);
}
