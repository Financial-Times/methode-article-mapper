package com.ft.methodearticletransformer.configuration;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class AssetTypeRequestConfiguration {

	private final int numberOfAssetIdsPerAssetTypeRequest;
	private final int numberOfParallelAssetTypeRequests;

	public AssetTypeRequestConfiguration(@JsonProperty("numberOfAssetIdsPerAssetTypeRequest") Integer numberOfAssetIdsPerAssetTypeRequest,
										 @JsonProperty("numberOfParallelAssetTypeRequests") Integer numberOfParallelAssetTypeRequests) {
		super();
		this.numberOfAssetIdsPerAssetTypeRequest = numberOfAssetIdsPerAssetTypeRequest;
		this.numberOfParallelAssetTypeRequests = numberOfParallelAssetTypeRequests;
	}

	@Min(1)
	@Max(20)
	@NotEmpty
	public int getNumberOfAssetIdsPerAssetTypeRequest() {
		return numberOfAssetIdsPerAssetTypeRequest;
	}

	@Min(1)
	@Max(20)
	@NotEmpty
	public int getNumberOfParallelAssetTypeRequests() {
		return numberOfParallelAssetTypeRequests;
	}

	protected Objects.ToStringHelper toStringHelper() {
		return Objects
				.toStringHelper(this)
				.add("numberOfAssetIdsPerAssetTypeRequest", numberOfAssetIdsPerAssetTypeRequest)
				.add("numberOfParallelAssetTypeRequests", numberOfParallelAssetTypeRequests);
	}

	@Override
	public String toString() {
		return toStringHelper().toString();
	}


}

