package com.ft.methodearticlemapper.transformation;

import org.w3c.dom.Document;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

public class ParsedEomFile {
  private final UUID uuid;
  private final Document attributesDocument;
  private final Document value;
  private final String body;
  private final URI webUrl;

  public ParsedEomFile(UUID uuid, Document value, String body, Document attributesDocument, URI webUrl) {
    this.uuid = uuid;
    this.value = value;
    this.body = body;
    this.attributesDocument = attributesDocument;
    this.webUrl = webUrl;
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

  public URI getWebUrl() {
    return webUrl;
  }
}
