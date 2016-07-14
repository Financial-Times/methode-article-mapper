package com.ft.methodearticletransformer.transformation;

import java.util.Date;
import java.util.UUID;

import org.w3c.dom.Document;

public class ParsedEomFile {
  private final UUID uuid;
  private final Document attributesDocument;
  private final Document value;
  private final String body;
  private final Date lastModified;
  
  public ParsedEomFile(UUID uuid, Document value, String body, Document attributesDocument, Date lastModified) {
    this.uuid = uuid;
    this.value = value;
    this.body = body;
    this.attributesDocument = attributesDocument;
    this.lastModified = lastModified;
  }
  
  public UUID getUUID() {
    return uuid;
  }
  
  public Document getValue() {
    return value;
  }
  
  public String getBody() {
    return body;
  }
  
  public Document getAttributes() {
    return attributesDocument;
  }
  
  public Date getLastModified() {
    return lastModified;
  }
}
