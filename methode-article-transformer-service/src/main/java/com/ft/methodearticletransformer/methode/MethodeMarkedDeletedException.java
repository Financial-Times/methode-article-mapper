package com.ft.methodearticletransformer.methode;

import java.util.UUID;

public class MethodeMarkedDeletedException extends RuntimeException {
    private UUID uuid;

    public MethodeMarkedDeletedException(UUID uuid) {
        super(String.format("Story has been marked as deleted %s", uuid));
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}