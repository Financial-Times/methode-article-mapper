package com.ft.methodearticletransformer.methode;

import java.util.UUID;

public class MethodeMarkedDeletedException extends RuntimeException {

    public MethodeMarkedDeletedException(UUID uuid) {
        super(String.format("Story has been marked as deleted %s", uuid));
    }

}
