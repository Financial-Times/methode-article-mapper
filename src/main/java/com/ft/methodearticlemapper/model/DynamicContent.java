package com.ft.methodearticlemapper.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class DynamicContent {
    private String id;
    private String title;
    private String bodyXML;
    private String blocks;

    private String uuid;
    private Date lastModified;
    private String publishReference;

    public DynamicContent(@JsonProperty("id") String id, @JsonProperty("title") String title,
                          @JsonProperty("bodyXML") String bodyXML, @JsonProperty("blocks") String blocks,
                          @JsonProperty("uuid") String uuid, @JsonProperty("lastModified") Date lastModified,
                          @JsonProperty("publishReference") String publishReference) {
        this.id = id;
        this.title = title;
        this.bodyXML = bodyXML;
        this.blocks = blocks;

        this.uuid = uuid;
        this.lastModified = lastModified;
        this.publishReference = publishReference;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBodyXML() {
        return bodyXML;
    }

    public void setBodyXML(String bodyXML) {
        this.bodyXML = bodyXML;
    }

    public String getBlocks() {
        return blocks;
    }

    public void setBlocks(String blocks) {
        this.blocks = blocks;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getPublishReference() {
        return publishReference;
    }

    public void setPublishReference(String publishReference) {
        this.publishReference = publishReference;
    }
}
