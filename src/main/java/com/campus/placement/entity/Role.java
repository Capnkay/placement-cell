package com.campus.placement.entity;

/**
 * Application roles. ADMIN is the Training and Placement Officer,
 * STUDENT is a registered candidate.
 */
public enum Role {
    ADMIN("Placement Officer"),
    STUDENT("Student");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
