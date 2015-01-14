package com.ft.methodearticletransformer.transformation;

import org.apache.commons.lang.StringUtils;

public class BigNumberData {

	private String headline;
	private String intro;
	private String link;

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

	public boolean isValidBigNumberData() {

		if ( StringUtils.isNotEmpty(link)) {
			return false;
		}
		return containsValidData(this.headline) || containsValidData(this.intro);
	}


	protected boolean containsValidData(String data) {
		return !StringUtils.isBlank(data);
	}

}
