package com.ft.methodearticlemapper.transformation;

import com.ft.methodearticlemapper.methode.ContentSource;
import org.w3c.dom.Document;

import java.util.UUID;

public class ParsedEomFile {
    private final UUID uuid;
    private final Document attributesDocument;
    private final Document systemAttributes;
    private final Document value;
    private final String body;
    private ContentSource contentSource;

    public ParsedEomFile(UUID uuid, Document value, String body, Document attributesDocument,
                         Document systemAttributes,
                         ContentSource contentSource) {
        this.uuid = uuid;
        this.value = value;
        this.body = body;
        this.attributesDocument = attributesDocument;
        this.systemAttributes = systemAttributes;
        this.contentSource = contentSource;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Document getValue() {
        return value;
    }

    public String getBody() {
        return body;
    }

    public Document getAttributes() {
        return attributesDocument;
    }

    public Document getSystemAttributes() {
        return systemAttributes;
    }

    public ContentSource getContentSource() {
        return contentSource;
    }
}
