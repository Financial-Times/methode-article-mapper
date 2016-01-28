package com.ft.methodearticletransformer.methode;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.entities.ErrorEntityFactory;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class MethodeArticleTransformerErrorEntityFactory implements ErrorEntityFactory {

    public MethodeArticleTransformerErrorEntityFactory() {
    }

    @Override
    public ErrorEntity entity(String message, Object context) {
        if (!(context instanceof Map)) {
            return new ErrorEntity(message);

        }

        Object uuidObj = ((Map) context).get("uuid");
        UUID uuid;
        if (uuidObj instanceof UUID) {
            uuid = (UUID) uuidObj;
        } else {
            // fall back to default format
            return new ErrorEntity(message);
        }

        Object lastModifiedObj = ((Map) context).get("lastModified");
        OffsetDateTime lastModifiedDate = null;
        if (lastModifiedObj instanceof OffsetDateTime) {
            lastModifiedDate = (OffsetDateTime) lastModifiedObj;
        }

        return new IdentifiableErrorEntity(uuid, lastModifiedDate, message);
    }
}
