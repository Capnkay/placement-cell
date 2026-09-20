package com.campus.placement.entity;

public enum DriveStatus {
    OPEN("Open", "badge-ok"),
    CLOSED("Closed", "badge-neutral");

    private final String label;
    private final String badgeClass;

    DriveStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
