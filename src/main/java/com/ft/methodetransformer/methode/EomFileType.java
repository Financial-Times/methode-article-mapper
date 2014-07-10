package com.ft.methodetransformer.methode;

public enum EomFileType {

    EOMCompoundStory("EOM::CompoundStory");

    private final String typeName;

    EomFileType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }
}
