package com.ft.methodearticlemapper.exception;


import java.util.UUID;

public class UnsupportedObjectTypeException extends MethodeContentNotEligibleForPublishException {
    public UnsupportedObjectTypeException(UUID uuid, String fileType) {
        super(uuid, String.format("File name for [%s] does not end with .xml [%s].", uuid, fileType));
    }
}
