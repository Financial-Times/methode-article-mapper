package com.ft.methodearticlemapper.transformation;

public enum ContributorRights {
    FIFTY_FIFTY_NEW("1"),
    FIFTY_FIFTY_OLD("2"),
    ALL_NO_PAYMENT("3"),
    CONTRACT("4"),
    NO_RIGHTS("5");

    private String value;

    ContributorRights(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    static ContributorRights fromString(String value) throws ContributorRightsException {
        if (value != null) {
            for (ContributorRights rights : ContributorRights.values()) {
                if (value.equals(rights.getValue())) {
                    return rights;
                }
            }
        }
        throw new ContributorRightsException("Unmatched type=" + value);
    }
}
