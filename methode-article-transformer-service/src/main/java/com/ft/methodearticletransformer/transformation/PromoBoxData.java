package com.ft.methodearticletransformer.transformation;

import org.apache.commons.lang.StringUtils;

public class PromoBoxData {

	private String headline;
	private String intro;
	private String link;
	private String title;

	private boolean imagePresent;

	public String getHeadline() {
		return headline;
	}

	public String getIntro() {
		return intro;
	}

	public void setHeadline(String headline) {
		this.headline = headline;
	}

	public void setIntro(String intro) {
		this.intro = intro;
	}

	public void setLink(String link) { this.link = link; }

	public void setTitle(String title) { this.title = title;
	}

	public boolean isValidBigNumberData() {

		if ( StringUtils.isNotEmpty(link) || imagePresent || StringUtils.isNotEmpty(title)) {
			return false;
		}
		return containsValidData(this.headline) || containsValidData(this.intro);
	}


	protected boolean containsValidData(String data) {
		return !StringUtils.isBlank(data);
	}

	public void setImagePresent(boolean imagePresent) {
		this.imagePresent = imagePresent;
	}
}
