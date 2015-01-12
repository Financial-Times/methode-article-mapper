package com.ft.methodearticletransformer.transformation;

import org.apache.commons.lang.StringUtils;

public class PullQuoteData {

	private String quoteText;
	private String quoteSource;

	public String getQuoteText() {
		return quoteText;
	}

	public String getQuoteSource() {
		return quoteSource;
	}

	public void setQuoteText(String quoteText) {
		this.quoteText = quoteText;
	}

	public void setQuoteSource(String quoteSource) {
		this.quoteSource = quoteSource;
	}

	public boolean isAllRequiredDataPresent() {
		return containsValidData(this.quoteText) || containsValidData(this.quoteSource);
	}

	protected boolean containsValidData(String data) {
		return !StringUtils.isBlank(data);
	}
}
