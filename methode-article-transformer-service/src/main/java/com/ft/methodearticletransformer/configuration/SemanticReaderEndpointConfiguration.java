package com.ft.methodearticletransformer.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.google.common.base.Objects;

public class SemanticReaderEndpointConfiguration {

    private final EndpointConfiguration endpointConfiguration;

    private final ConnectionConfiguration connectionConfig;

    public SemanticReaderEndpointConfiguration(@JsonProperty("endpointConfiguration") EndpointConfiguration endpointConfiguration,
                                               @JsonProperty("connectionConfig") ConnectionConfiguration connectionConfig) {
        this.endpointConfiguration = endpointConfiguration;
        this.connectionConfig = connectionConfig;
    }

    public EndpointConfiguration getEndpointConfiguration() { return endpointConfiguration; }

    public ConnectionConfiguration getConnectionConfig() { return connectionConfig; }

    protected Objects.ToStringHelper toStringHelper() {
        return Objects
                .toStringHelper(this)
                .add("endpointConfiguration", endpointConfiguration)
                .add("connectionConfig", connectionConfig);
    }

    @Override
    public String toString() {
        return toStringHelper().toString();
    }
}
