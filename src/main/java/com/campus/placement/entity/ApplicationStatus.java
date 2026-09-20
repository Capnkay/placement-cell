package com.campus.placement.entity;

/**
 * Lifecycle of a single student application against a single drive.
 */
public enum ApplicationStatus {
    APPLIED("Applied", "badge-neutral"),
    SHORTLISTED("Shortlisted", "badge-info"),
    INTERVIEW("Interview Scheduled", "badge-warn"),
    SELECTED("Selected", "badge-ok"),
    REJECTED("Not Selected", "badge-bad");

    private final String label;
    private final String badgeClass;

    ApplicationStatus(String label, String badgeClass) {
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
