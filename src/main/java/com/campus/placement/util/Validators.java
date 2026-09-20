package com.campus.placement.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Server side input rules. Every value that arrives from a browser passes
 * through here before it reaches a bean, because the HTML5 attributes on the
 * forms are a convenience for the user and not a security control.
 */
public final class Validators {

    /**
     * Deliberately stricter than the RFC. It rejects the shapes that cause real
     * problems in a college portal: no local part longer than 64 characters, no
     * leading, trailing or doubled dot, a domain made of proper labels, and a
     * final label of at least two letters. So "admin@campus" and "a..b@x.com"
     * and "user@site" are all refused.
     */
    private static final Pattern EMAIL = Pattern.compile(
            "^(?=.{1,64}@)[A-Za-z0-9]+(?:[._%+-][A-Za-z0-9]+)*"
                    + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+"
                    + "[A-Za-z]{2,24}$");

    private static final Pattern ROLL_NO = Pattern.compile("^[A-Za-z0-9/-]{3,30}$");

    private static final Pattern PHONE = Pattern.compile("^[0-9]{10}$");

    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,119}$");

    private Validators() {
    }

    public static boolean isEmail(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim();
        if (v.isEmpty() || v.length() > 190 || v.contains("..")) {
            return false;
        }
        return EMAIL.matcher(v).matches();
    }

    public static boolean isRollNo(String value) {
        return value != null && ROLL_NO.matcher(value.trim()).matches();
    }

    public static boolean isPhone(String value) {
        return value != null && PHONE.matcher(value.trim()).matches();
    }

    public static boolean isName(String value) {
        return value != null && NAME.matcher(value.trim()).matches();
    }

    /**
     * Password rule shown on the sign up form: at least eight characters with
     * one letter and one digit. Kept honest by being checked here as well.
     */
    public static boolean isStrongPassword(String value) {
        if (value == null || value.length() < 8 || value.length() > 128) {
            return false;
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : value.toCharArray()) {
            if (Character.isLetter(c)) {
                letter = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            }
        }
        return letter && digit;
    }

    public static String normaliseEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Parses a double, returning the fallback instead of throwing. */
    public static double toDouble(String value, double fallback) {
        try {
            return Double.parseDouble(trim(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /** Parses an int, returning the fallback instead of throwing. */
    public static int toInt(String value, int fallback) {
        try {
            return Integer.parseInt(trim(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public static Long toLong(String value) {
        try {
            return Long.valueOf(trim(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
}
