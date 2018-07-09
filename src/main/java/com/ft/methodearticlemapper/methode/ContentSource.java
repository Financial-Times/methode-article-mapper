package com.ft.methodearticlemapper.methode;

import java.util.HashMap;
import java.util.Map;

public enum ContentSource {
    FT("FT"),
    Reuters("REU2"),
    DynamicContent("DynamicContent");

    private static Map<String, ContentSource> contentSourceByCode = new HashMap<>();

    static {
        for (ContentSource contentSource : values()) {
            contentSourceByCode.put(contentSource.code, contentSource);
        }
    }

    private String code;

    ContentSource(String code) {
        this.code = code;
    }

    public static ContentSource getByCode(String code) {
        return contentSourceByCode.get(code);
    }
}
