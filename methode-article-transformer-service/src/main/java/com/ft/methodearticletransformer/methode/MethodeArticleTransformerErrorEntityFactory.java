package com.ft.methodearticletransformer.methode;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.entities.ErrorEntityFactory;

public class MethodeArticleTransformerErrorEntityFactory implements ErrorEntityFactory {

    public MethodeArticleTransformerErrorEntityFactory() {
    }

    @Override
    public ErrorEntity entity(String message, Object context) {

        if (context instanceof Map) {
            Object uuidObj = ((Map) context).get("uuid");
            UUID uuid;
            if (uuidObj instanceof UUID) {
                uuid = (UUID) uuidObj;
            } else {
                return new ErrorEntity(message);
            }
            
            Object lastModifiedDateObj = ((Map) context).get("lastModified");
            Date lastModifiedDate = null;
            if (lastModifiedDateObj instanceof Date) {
                lastModifiedDate = (Date) lastModifiedDateObj;
            }
            
            return new IdentifiableErrorEntity(uuid, lastModifiedDate, message);
        }


        // fall back to default format
        return new ErrorEntity(message);
    }
}
