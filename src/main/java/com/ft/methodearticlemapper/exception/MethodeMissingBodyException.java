package com.ft.methodearticlemapper.exception;

import java.util.UUID;


public class MethodeMissingBodyException extends MethodeContentInvalidException {
    private static final long serialVersionUID = -4315840020614523049L;

    public MethodeMissingBodyException(UUID uuid) {
        super(uuid,String.format("Story %s missing body text", uuid));
    }
}
