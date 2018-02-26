package com.ft.methodearticlemapper.model;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EomFileTest {
    private static final String COMPOUND_STORY = "EOM::CompoundStory";
    private static final String WEB_READY = "Stories/WebReady";
    private static final byte[] VALUE = Base64.getEncoder().encode("A long time ago, in a galaxy far, far away ...".getBytes());
    private static final String ATTRS = "<?xml version=\"1.0\"><ObjectMetadata></ObjectMetadata>";
    private static final String SYS_ATTRS = "<props></props>";
    private static final String TICKETS = "<?xml version='1.0' encoding='UTF-8'?><tl></tl>";
    
    static {
        Map<String,String> additionalProperties = new HashMap<>();
        additionalProperties.put("lastModified", "lastModified");
        additionalProperties.put("publishReference", "publishReference");
        
        EomFile.setAdditionalMappings(additionalProperties);
    }
    
    @Test
    public void testEomFileWithKnownPropertiesOnly() throws IOException {
        Map<String,Object> value = new HashMap<>();
        String uuid = UUID.randomUUID().toString();
        value.put("uuid", uuid);
        value.put("type", COMPOUND_STORY);
        value.put("value", VALUE);
        value.put("attributes", ATTRS);
        value.put("systemAttributes", SYS_ATTRS);
        value.put("workflowStatus", WEB_READY);
        value.put("usageTickets", TICKETS);
        
        ObjectMapper mapper = new ObjectMapper();
        byte[] src = mapper.writeValueAsBytes(value);
        
        EomFile actual = mapper.readValue(src, EomFile.class);
        assertThat(actual.getUuid(), equalTo(uuid));
        assertThat(actual.getType(), equalTo(COMPOUND_STORY));
        assertThat(actual.getValue(), equalTo(VALUE));
        assertThat(actual.getAttributes(), equalTo(ATTRS));
        assertThat(actual.getSystemAttributes(), equalTo(SYS_ATTRS));
        assertThat(actual.getWorkflowStatus(), equalTo(WEB_READY));
        assertThat(actual.getUsageTickets(), equalTo(TICKETS));
        assertThat(actual.getWebUrl(), nullValue());
        assertThat(actual.getAdditionalProperties(), equalTo(Collections.emptyMap()));
    }
    
    @Test
    public void thatAdditionalPropertiesAreDeserialized() throws IOException {
        String uuid = UUID.randomUUID().toString();
        String lastModified = "2018-02-23T15:21:00Z";
        String publishReference = "tid_1234abcd";
        
        Map<String,Object> value = new HashMap<>();
        value.put("uuid", uuid);
        value.put("type", COMPOUND_STORY);
        value.put("value", VALUE);
        value.put("attributes", ATTRS);
        value.put("systemAttributes", SYS_ATTRS);
        value.put("workflowStatus", WEB_READY);
        value.put("usageTickets", TICKETS);
        value.put("lastModified", lastModified);
        value.put("publishReference", publishReference);
        
        ObjectMapper mapper = new ObjectMapper();
        byte[] src = mapper.writeValueAsBytes(value);
        
        EomFile actual = mapper.readValue(src, EomFile.class);
        assertThat(actual.getUuid(), equalTo(uuid));
        assertThat(actual.getType(), equalTo(COMPOUND_STORY));
        assertThat(actual.getValue(), equalTo(VALUE));
        assertThat(actual.getAttributes(), equalTo(ATTRS));
        assertThat(actual.getSystemAttributes(), equalTo(SYS_ATTRS));
        assertThat(actual.getWorkflowStatus(), equalTo(WEB_READY));
        assertThat(actual.getUsageTickets(), equalTo(TICKETS));
        assertThat(actual.getWebUrl(), nullValue());
        
        Map<String,String> expectedAdditionalProperties = new HashMap<>();
        expectedAdditionalProperties.put("lastModified", lastModified);
        expectedAdditionalProperties.put("publishReference", publishReference);
        assertThat(actual.getAdditionalProperties(), equalTo(expectedAdditionalProperties));
    }
    
    @Test
    public void thatUnsupportedPropertiesAreIgnored() throws IOException {
        String uuid = UUID.randomUUID().toString();
        String lastModified = "2018-02-23T15:21:00Z";
        String publishReference = "tid_1234abcd";
        
        Map<String,Object> value = new HashMap<>();
        value.put("uuid", uuid);
        value.put("type", COMPOUND_STORY);
        value.put("value", VALUE);
        value.put("attributes", ATTRS);
        value.put("systemAttributes", SYS_ATTRS);
        value.put("workflowStatus", WEB_READY);
        value.put("usageTickets", TICKETS);
        value.put("lastModified", lastModified);
        value.put("publishReference", publishReference);
        value.put("foo", "bar");
        
        ObjectMapper mapper = new ObjectMapper();
        byte[] src = mapper.writeValueAsBytes(value);
        
        EomFile actual = mapper.readValue(src, EomFile.class);
        assertThat(actual.getUuid(), equalTo(uuid));
        assertThat(actual.getType(), equalTo(COMPOUND_STORY));
        assertThat(actual.getValue(), equalTo(VALUE));
        assertThat(actual.getAttributes(), equalTo(ATTRS));
        assertThat(actual.getSystemAttributes(), equalTo(SYS_ATTRS));
        assertThat(actual.getWorkflowStatus(), equalTo(WEB_READY));
        assertThat(actual.getUsageTickets(), equalTo(TICKETS));
        assertThat(actual.getWebUrl(), nullValue());
        
        Map<String,String> expectedAdditionalProperties = new HashMap<>();
        expectedAdditionalProperties.put("lastModified", lastModified);
        expectedAdditionalProperties.put("publishReference", publishReference);
        assertThat(actual.getAdditionalProperties(), equalTo(expectedAdditionalProperties));
    }
    
    @Test
    public void thatUnsupportedArrayPropertiesAreIgnored() throws IOException {
        String uuid = UUID.randomUUID().toString();
        String lastModified = "2018-02-23T15:21:00Z";
        String publishReference = "tid_1234abcd";
        
        Map<String,Object> value = new HashMap<>();
        value.put("uuid", uuid);
        value.put("type", COMPOUND_STORY);
        value.put("value", VALUE);
        value.put("attributes", ATTRS);
        value.put("systemAttributes", SYS_ATTRS);
        value.put("workflowStatus", WEB_READY);
        value.put("usageTickets", TICKETS);
        value.put("lastModified", lastModified);
        value.put("publishReference", publishReference);
        value.put("linkedObjects", new Object[] {});
        
        ObjectMapper mapper = new ObjectMapper();
        byte[] src = mapper.writeValueAsBytes(value);
        
        EomFile actual = mapper.readValue(src, EomFile.class);
        assertThat(actual.getUuid(), equalTo(uuid));
        assertThat(actual.getType(), equalTo(COMPOUND_STORY));
        assertThat(actual.getValue(), equalTo(VALUE));
        assertThat(actual.getAttributes(), equalTo(ATTRS));
        assertThat(actual.getSystemAttributes(), equalTo(SYS_ATTRS));
        assertThat(actual.getWorkflowStatus(), equalTo(WEB_READY));
        assertThat(actual.getUsageTickets(), equalTo(TICKETS));
        assertThat(actual.getWebUrl(), nullValue());
        
        Map<String,String> expectedAdditionalProperties = new HashMap<>();
        expectedAdditionalProperties.put("lastModified", lastModified);
        expectedAdditionalProperties.put("publishReference", publishReference);
        assertThat(actual.getAdditionalProperties(), equalTo(expectedAdditionalProperties));
    }
}
