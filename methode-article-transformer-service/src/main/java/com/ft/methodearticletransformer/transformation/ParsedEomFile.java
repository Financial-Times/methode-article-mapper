package com.ft.methodearticletransformer.transformation;

import org.w3c.dom.Document;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

public class ParsedEomFile {
  private final UUID uuid;
  private final Document attributesDocument;
  private final Document value;
  private final String body;
  private final Date lastModified;
  private final URI webUrl;

  public ParsedEomFile(UUID uuid, Document value, String body, Document attributesDocument, Date lastModified, URI webUrl) {
    this.uuid = uuid;
    this.value = value;
    this.body = body;
    this.attributesDocument = attributesDocument;
    this.webUrl = webUrl;
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

  public URI getWebUrl() {
    return webUrl;
  }
}
