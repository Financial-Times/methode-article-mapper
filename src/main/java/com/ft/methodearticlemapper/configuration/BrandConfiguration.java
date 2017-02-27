package com.ft.methodearticlemapper.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BrandConfiguration {

    private String name;

    private String id;

    public BrandConfiguration(@JsonProperty("brand") String name, @JsonProperty("id") String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
}
