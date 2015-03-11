package com.ft.methodearticletransformer.transformation;

import org.apache.commons.lang.StringUtils;

public class PromoBoxData {

	private String headline;
	private String intro;
	private String link;
	private String title;
	private String imageHtml;

	public String getHeadline() {
		return headline;
	}

	public String getIntro() {
		return intro;
	}

	public String getLink() {
		return link;
	}

	public String getTitle() {
		return title;
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
		return containsValidData(this.headline) || containsValidData(this.intro);
	}


	protected boolean containsValidData(String data) {
		return !StringUtils.isBlank(data);
	}

	public boolean isValidPromoBoxData() {
		return containsValidData(this.intro) || containsValidData(this.link) || containsValidData(this.headline)
				|| containsValidData(this.headline) || containsValidData(this.imageHtml);
	}

	public void setImageHtml(String imageHtml) {
		this.imageHtml = imageHtml;
	}

	public String getImageHtml() {
		return imageHtml;
	}
}
