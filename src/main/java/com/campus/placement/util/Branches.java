package com.campus.placement.util;

import java.util.List;

/**
 * The branches the college runs. Kept as a fixed list so a typed branch name
 * can never drift away from the branch names a drive is filtered on.
 */
public final class Branches {

    public static final List<String> ALL = List.of(
            "Computer Science",
            "Information Technology",
            "Electronics",
            "Statistics",
            "Mechanical",
            "Civil");

    private Branches() {
    }

    public static boolean isKnown(String branch) {
        return branch != null && ALL.stream().anyMatch(b -> b.equalsIgnoreCase(branch.trim()));
    }

    /** Returns the canonical spelling, so "computer science" is stored correctly. */
    public static String canonical(String branch) {
        if (branch == null) {
            return "";
        }
        return ALL.stream()
                .filter(b -> b.equalsIgnoreCase(branch.trim()))
                .findFirst()
                .orElse(branch.trim());
    }
}
