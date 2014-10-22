package com.ft.methodearticletransformer.methode;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.api.jaxrs.errors.ErrorEntity;
import com.google.common.base.Objects;

public class IdentifiableErrorEntity extends ErrorEntity {
    private final UUID uuid;

    public IdentifiableErrorEntity(@JsonProperty("uuid") UUID uuid, @JsonProperty("message") String message) {
        super(message);
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    protected Objects.ToStringHelper toStringHelper() {
        return super.toStringHelper().add("uuid",uuid);
    }

}
