package com.ft.methodearticletransformer.methode;

/**
 * The post is a valid WordPress post, but cannot be published by the transformer.
 * For example:
 * <ul>
 * <li>it has a type or custom type other than "post", meaning it has custom formatting associated with it</li>
 * <li>it has no body text</li>
 * </ul>
 */
public class UntransformableMethodeContentException
        extends RuntimeException {

    private final String uuid;

    public UntransformableMethodeContentException(String uuid, String reason) {
        super(reason);
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}
