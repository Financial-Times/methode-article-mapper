package com.ft.methodearticlemapper.http;

public class VersionNumber implements Comparable<VersionNumber> {

	private String versionString;

	public VersionNumber(String versionString) {
		if (versionString == null) {
			throw new IllegalArgumentException("versionString cannot be null");
		}
		this.versionString = versionString;
	}

	private String getVersionString() {
		return versionString;
	}

	@Override
	public int compareTo(VersionNumber otherVersionNumber) {
		String[] version1Array = getVersionString().split("\\.");
		String[] version2Array = otherVersionNumber.getVersionString().split("\\.");

		int i=0;
		while (i<version1Array.length && i<version2Array.length && version1Array[i].equals(version2Array[i])) {
			i++;
		}

		if (i<version1Array.length && i<version2Array.length) {
			int diff = Integer.valueOf(version1Array[i]).compareTo(Integer.valueOf(version2Array[i]));
			return Integer.signum(diff);
		}

		return Integer.signum(version1Array.length - version2Array.length);
	}
}
