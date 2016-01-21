package com.ft.methodearticletransformer.methode;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.api.jaxrs.errors.ErrorEntity;
import com.google.common.base.Objects;

import javax.validation.constraints.NotNull;

public class IdentifiableErrorEntity extends ErrorEntity {
    private final UUID uuid;
    private Date lastModified;

    public IdentifiableErrorEntity(@JsonProperty("uuid") UUID uuid, @JsonProperty("lastModified") Date lastModified, @JsonProperty("message") String message) {
        super(message);
        this.uuid = uuid;
        this.lastModified = lastModified;
    }

    public UUID getUuid() {
        return uuid;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    protected Objects.ToStringHelper toStringHelper() {
        return super.toStringHelper().add("uuid",uuid).add("lastModified", lastModified);
    }
}
