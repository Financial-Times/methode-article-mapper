package com.ft.methodearticlemapper.util;

import com.ft.methodearticlemapper.methode.ContentSource;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

public class ContentType {

    public interface Type {
        String CONTENT_PACKAGE = "ContentPackage";
        String ARTICLE = "Article";
        String DYNAMIC_CONTENT = "DynamicContent";
    }

    public static String determineType(final XPath xpath,
                                 final Document attributesDocument,
                                 ContentSource contentSource) throws XPathExpressionException {
        final String isContentPackage = xpath.evaluate("/ObjectMetadata/OutputChannels/DIFTcom/isContentPackage", attributesDocument);
        if (Boolean.TRUE.toString().equalsIgnoreCase(isContentPackage)) {
            return ContentType.Type.CONTENT_PACKAGE;
        }

        if (contentSource.equals(ContentSource.DynamicContent)) {
            return ContentType.Type.DYNAMIC_CONTENT;
        }

        return ContentType.Type.ARTICLE;
    }

}
