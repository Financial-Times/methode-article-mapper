package com.ft.methodearticletransformer.transformation;

import org.apache.commons.lang.StringUtils;

public class SlideshowData {
private String uuid;
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public boolean isAllRequiredDataPresent() {
        return containsValidData(this.uuid);
    }
    
    protected boolean containsValidData(String data) {
        return !StringUtils.isBlank(data);
    }
}
