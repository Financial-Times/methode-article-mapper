package com.ft.methodearticletransformer.transformation;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.util.HashMap;
import java.util.Map;

class EomFileAttributesBuilder {
    private Map<String, String> attributes;
    private String attributesTemplate;

    EomFileAttributesBuilder(String attributesTemplate) {
        this.attributes = new HashMap<>();
        this.attributesTemplate = attributesTemplate;
    }

    EomFileAttributesBuilder withLastPublicationDate(String lastPublicationDate) {
        this.attributes.put("lastPublicationDate", lastPublicationDate);
        return this;
    }

    EomFileAttributesBuilder withInitialPublicationDate(String initialPublicationDate) {
        this.attributes.put("initialPublicationDate", initialPublicationDate);
        return this;
    }

    EomFileAttributesBuilder withMarkedDeleted(String markedDeleted) {
        this.attributes.put("deleted", markedDeleted);
        return this;
    }

    EomFileAttributesBuilder withImageMetadata(String imageMetadata) {
        this.attributes.put("articleImage", imageMetadata);
        return this;
    }

    EomFileAttributesBuilder withCommentsEnabled(String commentsEnabled) {
        this.attributes.put("comments", commentsEnabled);
        return this;
    }

    EomFileAttributesBuilder withEditorsPick(String editorsPick) {
        this.attributes.put("editorsPick", editorsPick);
        return this;
    }

    EomFileAttributesBuilder withExclusive(String exclusive) {
        this.attributes.put("exclusive", exclusive);
        return this;
    }

    EomFileAttributesBuilder withScoop(String scoop) {
        this.attributes.put("scoop", scoop);
        return this;
    }

    EomFileAttributesBuilder withEmbargoDate(String embargoDate) {
        this.attributes.put("embargoDate", embargoDate);
        return this;
    }

    EomFileAttributesBuilder withSourceCode(String sourceCode) {
        this.attributes.put("sourceCode", sourceCode);
        return this;
    }

    EomFileAttributesBuilder withContributorRights(String contributorRights) {
        this.attributes.put("contributorRights", contributorRights);
        return this;
    }

    String build() {
        Template mustache = Mustache.compiler().compile(attributesTemplate);
        return mustache.execute(attributes);
    }
}
