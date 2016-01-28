package com.ft.methodearticletransformer.methode;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.methodearticletransformer.util.OffsetDateTimeSerializer;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

public class IdentifiableErrorEntity extends ErrorEntity {
    private final UUID uuid;
    private OffsetDateTime lastModified;

    public IdentifiableErrorEntity(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("lastModified") OffsetDateTime lastModified,
            @JsonProperty("message") String message) {
        super(message);
        this.uuid = uuid;
        this.lastModified = lastModified;
    }

    public UUID getUuid() {
        return uuid;
    }

    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    public OffsetDateTime getLastModified() {
        return lastModified;
    }

    @Override
    protected Objects.ToStringHelper toStringHelper() {
        return super.toStringHelper().add("uuid",uuid).add("lastModified", lastModified);
    }
}
