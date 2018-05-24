package com.ft.methodearticlemapper.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Block {
    private String key;
    private String valueXML;

    public Block(@JsonProperty("key") String key, @JsonProperty("valueXML") String valueXML) {
        this.key = key;
        this.valueXML = valueXML;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValueXML() {
        return valueXML;
    }

    public void setValueXML(String valueXML) {
        this.valueXML = valueXML;
    }
}
