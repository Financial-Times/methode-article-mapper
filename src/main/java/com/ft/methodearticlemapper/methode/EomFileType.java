package com.ft.methodearticlemapper.methode;

public enum EomFileType {

    EOMCompoundStory("EOM::CompoundStory"), EOMStory("EOM::Story");


    private final String typeName;

    EomFileType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }
}
