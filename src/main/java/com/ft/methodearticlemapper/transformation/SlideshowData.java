package com.ft.methodearticlemapper.transformation;

import org.apache.commons.lang.StringUtils;

import java.util.List;

public class SlideshowData {
    private String uuid;
	private String title;

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
