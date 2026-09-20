package com.campus.placement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Input validation")
class ValidatorsTest {

    @ParameterizedTest
    @DisplayName("well formed addresses are accepted")
    @ValueSource(strings = {
            "aarti.deshpande@campus.edu",
            "tpo@campus.edu",
            "a@b.co",
            "first.last+tag@sub.domain.example",
            "roll-2021@college-campus.ac.in",
            "n_a_m_e@example.com"
    })
    void acceptsValidEmails(String email) {
        assertTrue(Validators.isEmail(email), email + " should be accepted");
    }

    @ParameterizedTest
    @DisplayName("malformed addresses are refused")
    @ValueSource(strings = {
            "admin@campus",          // no top level domain
            "user@site",             // same
            "a..b@example.com",      // doubled dot
            ".lead@example.com",     // leading dot
            "trail.@example.com",    // trailing dot before the at sign
            "no-at-sign.example.com",
            "two@@example.com",
            "spaced name@example.com",
            "user@example..com",
            "user@.example.com",
            "user@example.c",        // single letter top level domain
            "@example.com",
            "user@",
            "<script>@example.com"
    })
    void refusesMalformedEmails(String email) {
        assertFalse(Validators.isEmail(email), email + " should be refused");
    }

    @Test
    @DisplayName("surrounding whitespace is trimmed rather than rejected")
    void trimsSurroundingWhitespace() {
        assertTrue(Validators.isEmail("  tpo@campus.edu  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("a missing address is refused")
    void refusesMissingEmail(String email) {
        assertFalse(Validators.isEmail(email));
    }

    @Test
    @DisplayName("an over long local part is refused")
    void refusesOverLongLocalPart() {
        assertFalse(Validators.isEmail("a".repeat(65) + "@example.com"));
        assertTrue(Validators.isEmail("a".repeat(64) + "@example.com"));
    }

    @Test
    @DisplayName("addresses are normalised to lower case")
    void normalisesEmail() {
        assertEquals("tpo@campus.edu", Validators.normaliseEmail("  TPO@Campus.EDU  "));
        assertEquals("", Validators.normaliseEmail(null));
    }

    @ParameterizedTest
    @DisplayName("roll numbers accept letters, digits, slash and hyphen")
    @ValueSource(strings = {"CS21001", "it-2021-07", "20/CS/114", "abc"})
    void acceptsRollNumbers(String roll) {
        assertTrue(Validators.isRollNo(roll));
    }

    @ParameterizedTest
    @DisplayName("roll numbers refuse spaces, symbols and short values")
    @ValueSource(strings = {"CS 21001", "cs@21", "ab", "'; DROP TABLE student_profile; --"})
    void refusesBadRollNumbers(String roll) {
        assertFalse(Validators.isRollNo(roll));
    }

    @ParameterizedTest
    @DisplayName("a ten digit mobile number is required")
    @ValueSource(strings = {"9820011223", "0000000000"})
    void acceptsPhone(String phone) {
        assertTrue(Validators.isPhone(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"98200112", "98200112234", "98200 11223", "+919820011223", "abcdefghij"})
    @DisplayName("anything that is not ten digits is refused")
    void refusesPhone(String phone) {
        assertFalse(Validators.isPhone(phone));
    }

    @ParameterizedTest
    @DisplayName("passwords need eight characters with a letter and a digit")
    @ValueSource(strings = {"Campus@2026", "abcdefg1", "1a345678"})
    void acceptsStrongPasswords(String password) {
        assertTrue(Validators.isStrongPassword(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "allletters", "12345678", "       1", "Ab1"})
    @DisplayName("weak passwords are refused")
    void refusesWeakPasswords(String password) {
        assertFalse(Validators.isStrongPassword(password));
    }

    @Test
    @DisplayName("names accept the punctuation real names contain")
    void acceptsNames() {
        assertTrue(Validators.isName("Aarti Deshpande"));
        assertTrue(Validators.isName("O'Brien"));
        assertTrue(Validators.isName("Jean-Luc Picard"));
        assertTrue(Validators.isName("A. P. J. Abdul Kalam"));
        assertFalse(Validators.isName("X"));
        assertFalse(Validators.isName("123"));
        assertFalse(Validators.isName("<b>bold</b>"));
    }

    @Test
    @DisplayName("number parsing falls back instead of throwing")
    void parsesNumbersSafely() {
        assertEquals(8.7, Validators.toDouble("8.7", -1));
        assertEquals(-1, Validators.toDouble("not a number", -1));
        assertEquals(-1, Validators.toDouble(null, -1));
        assertEquals(3, Validators.toInt("3", -1));
        assertEquals(-1, Validators.toInt("3.5", -1));
        assertEquals(null, Validators.toLong("abc"));
        assertEquals(42L, Validators.toLong(" 42 "));
    }

    @Test
    @DisplayName("range checks are inclusive at both ends")
    void checksRanges() {
        assertTrue(Validators.inRange(0, 0, 10));
        assertTrue(Validators.inRange(10, 0, 10));
        assertFalse(Validators.inRange(10.01, 0, 10));
        assertFalse(Validators.inRange(-0.01, 0, 10));
    }
}
