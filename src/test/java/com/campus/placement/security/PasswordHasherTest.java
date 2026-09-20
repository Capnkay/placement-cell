package com.campus.placement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Password hashing")
class PasswordHasherTest {

    @Test
    @DisplayName("the stored value never contains the password")
    void doesNotStoreThePassword() {
        String stored = PasswordHasher.hash("Campus@2026");
        assertFalse(stored.contains("Campus@2026"),
                "the digest must not carry the password in the clear");
        assertTrue(stored.startsWith("pbkdf2$120000$"),
                "the format should record the algorithm and the iteration count");
        assertEquals(4, stored.split("\\$").length,
                "expected algorithm, iterations, salt and hash");
    }

    @Test
    @DisplayName("the right password verifies")
    void verifiesTheRightPassword() {
        String stored = PasswordHasher.hash("Campus@2026");
        assertTrue(PasswordHasher.verify("Campus@2026", stored));
    }

    @ParameterizedTest
    @DisplayName("a wrong password is refused")
    @ValueSource(strings = {"campus@2026", "Campus@2025", "Campus@20266", "", " Campus@2026"})
    void refusesAWrongPassword(String attempt) {
        String stored = PasswordHasher.hash("Campus@2026");
        assertFalse(PasswordHasher.verify(attempt, stored));
    }

    @Test
    @DisplayName("two people with the same password get different digests")
    void saltsEveryHash() {
        String first = PasswordHasher.hash("Campus@2026");
        String second = PasswordHasher.hash("Campus@2026");
        assertNotEquals(first, second,
                "a per user salt is what stops one cracked hash unlocking every matching account");
        assertTrue(PasswordHasher.verify("Campus@2026", first));
        assertTrue(PasswordHasher.verify("Campus@2026", second));
    }

    @ParameterizedTest
    @DisplayName("a damaged stored value is refused rather than throwing")
    @ValueSource(strings = {"", "not-a-hash", "pbkdf2$120000$onlythree", "md5$1$aa$bb",
                            "pbkdf2$notanumber$aa$bb", "pbkdf2$120000$!!!$!!!"})
    void refusesMalformedStoredValues(String stored) {
        assertFalse(PasswordHasher.verify("Campus@2026", stored));
    }

    @Test
    @DisplayName("nulls are refused, not thrown on")
    void handlesNulls() {
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("Campus@2026")));
        assertFalse(PasswordHasher.verify("Campus@2026", null));
        assertFalse(PasswordHasher.verify(null, null));
    }

    @Test
    @DisplayName("burning a password for an unknown account does not throw")
    void burnIsSafe() {
        PasswordHasher.burn("anything");
        PasswordHasher.burn(null);
    }

    @Test
    @DisplayName("unicode passwords survive the round trip")
    void handlesUnicode() {
        String password = "çampus❤Pass1";
        assertTrue(PasswordHasher.verify(password, PasswordHasher.hash(password)));
    }
}
