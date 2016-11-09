package com.ft.methodearticlemapper.methode;

import java.util.UUID;

/**
 * MethodeMissingFieldException
 *
 * @author Simon.Gibbs
 */
public class MethodeMissingFieldException extends MethodeContentInvalidException {
	private static final long serialVersionUID = 1957685706838057455L;
	private final String fieldName;

    public MethodeMissingFieldException(UUID uuid, String fieldName) {
        super(uuid,String.format("Story %s missing field: %s", uuid, fieldName));
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
