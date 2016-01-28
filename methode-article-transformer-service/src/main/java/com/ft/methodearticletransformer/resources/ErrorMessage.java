package com.ft.methodearticletransformer.resources;

/**
 * Created by julia.fernee on 28/01/2016.
 */
enum ErrorMessage {
    METHODE_FILE_NOT_FOUND("Article cannot be found in Methode"),
    UUID_REQUIRED("No UUID was passed"),
    INVALID_UUID("The UUID passed was invalid"),
    METHODE_CONTENT_TYPE_NOT_SUPPORTED("Invalid request - resource not an article"),
    NOT_WEB_CHANNEL("This is not a web channel story"),
    METHODE_FIELD_MISSING("Required methode field [%s] is missing"),
    METHODE_API_UNAVAILABLE("Methode api was unavailable"),
    DOCUMENT_STORE_API_UNAVAILABLE("Document store API was unavailable");

    private final String text;

    ErrorMessage(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
