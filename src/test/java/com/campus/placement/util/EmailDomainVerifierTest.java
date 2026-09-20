package com.campus.placement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises the DNS backed half of registration validation.
 *
 * <p>These tests need a real resolver reachable on the network, the same
 * dependency {@code DatabaseConnectivityTest} has on a running MySQL. The
 * same pattern is used here: a one time check in {@code @BeforeAll} decides
 * whether DNS answers at all, and every DNS dependent test is skipped rather
 * than failed if it does not, so an offline sandbox does not turn into a
 * flaky suite.</p>
 */
@DisplayName("Email domain verification")
class EmailDomainVerifierTest {

    private static boolean dnsReachable;

    @BeforeAll
    static void checkDnsIsUp() {
        // gmail.com is about as safe a bet as exists for "DNS works at all".
        dnsReachable = EmailDomainVerifier.hasValidMxOrA("gmail.com");
        if (!dnsReachable) {
            System.out.println("DNS does not appear reachable, MX/A lookup tests skipped.");
        }
    }

    @ParameterizedTest
    @DisplayName("real, mail capable domains resolve")
    @ValueSource(strings = {"gmail.com", "outlook.com", "yahoo.com", "campus.edu"})
    void acceptsRealDomains(String domain) {
        assumeTrue(dnsReachable, "DNS is not reachable in this environment");
        assertTrue(EmailDomainVerifier.hasValidMxOrA(domain), domain + " should resolve");
    }

    @Test
    @DisplayName("an invented domain has no MX or A record")
    void refusesInventedDomain() {
        assumeTrue(dnsReachable, "DNS is not reachable in this environment");
        assertFalse(EmailDomainVerifier.hasValidMxOrA(
                "thisdomaindoesnotexist-placementcell-2026.zzz"));
    }

    @ParameterizedTest
    @DisplayName("well known disposable providers are refused")
    @ValueSource(strings = {"mailinator.com", "guerrillamail.com", "10minutemail.com",
            "yopmail.com", "tempmail.com", "MAILINATOR.COM"})
    void refusesDisposableDomains(String domain) {
        assertTrue(EmailDomainVerifier.isDisposableDomain(domain));
    }

    @ParameterizedTest
    @DisplayName("ordinary domains are not treated as disposable")
    @ValueSource(strings = {"gmail.com", "campus.edu", "outlook.com"})
    void doesNotFlagOrdinaryDomains(String domain) {
        assertFalse(EmailDomainVerifier.isDisposableDomain(domain));
    }

    @Test
    @DisplayName("the domain half of an address is extracted and lower cased")
    void extractsDomain() {
        assertEquals("campus.edu", EmailDomainVerifier.domainOf("Aarti.Deshpande@Campus.EDU"));
        assertEquals("", EmailDomainVerifier.domainOf(null));
        assertEquals("", EmailDomainVerifier.domainOf("not-an-email"));
    }
}
