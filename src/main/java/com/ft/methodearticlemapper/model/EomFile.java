package com.ft.methodearticlemapper.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class EomFile {

  public static final String WEB_REVISE = "Stories/WebRevise";
  public static final String WEB_READY = "Stories/WebReady";
  public static final String WEB_CHANNEL = "FTcom";

  public static final String SOURCE_ATTR_XPATH =
      "/ObjectMetadata//EditorialNotes/Sources/Source/SourceCode";

  public static final String LAST_PUBLICATION_DATE_XPATH =
      "/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomLastPublication";
  public static final String INITIAL_PUBLICATION_DATE_XPATH =
      "/ObjectMetadata/OutputChannels/DIFTcom/DIFTcomInitialPublication";
  public static final String STORY_PACKAGE_LINK_XPATH = "/doc/lead/editor-choice/a/@href";
  public static final String WORK_FOLDER_SYSTEM_ATTRIBUTE_XPATH = "/props/workFolder";
  public static final String SUB_FOLDER_SYSTEM_ATTRIBUTE_XPATH = "/props/subFolder";

  private static final Map<String, String> ADDITIONAL_PROPERTY_MAPPINGS = new LinkedHashMap<>();

  private final String uuid;
  private final String type;

  private final byte[] value;
  private final String attributes;
  private final String workflowStatus;
  private final String systemAttributes;
  private final String usageTickets;

  private final Map<String, String> additionalProperties = new LinkedHashMap<>();

  public static void setAdditionalMappings(Map<String, String> additionalProperties) {
    ADDITIONAL_PROPERTY_MAPPINGS.putAll(additionalProperties);
  }

  public EomFile(
      @JsonProperty("uuid") String uuid,
      @JsonProperty("type") String type,
      @JsonProperty("value") byte[] bytes,
      @JsonProperty("attributes") String attributes,
      @JsonProperty("workflowStatus") String workflowStatus,
      @JsonProperty("systemAttributes") String systemAttributes,
      @JsonProperty("usageTickets") String usageTickets) {
    this.uuid = uuid;
    this.type = type;
    this.value = bytes;
    this.attributes = attributes;
    this.workflowStatus = workflowStatus;
    this.systemAttributes = systemAttributes;
    this.usageTickets = usageTickets;
  }

  public String getUuid() {
    return uuid;
  }

  public String getType() {
    return type;
  }

  @SuppressWarnings(value = "EI_EXPOSE_REP")
  public byte[] getValue() {
    return value;
  }

  public String getAttributes() {
    return attributes;
  }

  public String getWorkflowStatus() {
    return workflowStatus;
  }

  public String getSystemAttributes() {
    return systemAttributes;
  }

  public String getUsageTickets() {
    return usageTickets;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    String mappedName = ADDITIONAL_PROPERTY_MAPPINGS.get(name);
    if ((mappedName != null) && (value instanceof String)) {
      additionalProperties.put(mappedName, (String) value);
    }
  }

  @JsonAnyGetter
  public Map<String, String> getAdditionalProperties() {
    return Collections.unmodifiableMap(additionalProperties);
  }

  public static class Builder {
    private String uuid;
    private String type;
    private byte[] value;
    private String attributes;
    private String workflowStatus;
    private String systemAttributes;
    private String usageTickets;

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    @SuppressWarnings(value = "EI_EXPOSE_REP")
    public Builder withValue(byte[] value) {
      this.value = value;
      return this;
    }

    public Builder withAttributes(String attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder withWorkflowStatus(String workflowStatus) {
      this.workflowStatus = workflowStatus;
      return this;
    }

    public Builder withSystemAttributes(String systemAttributes) {
      this.systemAttributes = systemAttributes;
      return this;
    }

    public Builder withUsageTickets(String usageTickets) {
      this.usageTickets = usageTickets;
      return this;
    }

    public Builder withValuesFrom(EomFile eomFile) {
      return withUuid(eomFile.getUuid())
          .withType(eomFile.getType())
          .withValue(eomFile.getValue())
          .withAttributes(eomFile.getAttributes())
          .withWorkflowStatus(eomFile.getWorkflowStatus())
          .withSystemAttributes(eomFile.getSystemAttributes())
          .withUsageTickets(eomFile.getUsageTickets());
    }

    public EomFile build() {
      return new EomFile(
          uuid, type, value, attributes, workflowStatus, systemAttributes, usageTickets);
    }
  }
}
