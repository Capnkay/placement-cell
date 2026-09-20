package com.campus.placement.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted password hashing built only on the JDK, no third party library.
 *
 * <p>Stored format is {@code pbkdf2$<iterations>$<saltBase64>$<hashBase64>}.
 * Every user gets a fresh 16 byte random salt, so two people who pick the same
 * password still end up with different digests. Verification is done with a
 * constant time comparison so the response time does not leak how many leading
 * bytes of a guess were right.</p>
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String PREFIX = "pbkdf2";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] digest = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS);
        Base64.Encoder enc = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + enc.encodeToString(salt) + "$"
                + enc.encodeToString(digest);
    }

    /**
     * @return true only when the raw password reproduces the stored digest.
     *         A malformed or missing stored value returns false, it never throws.
     */
    public static boolean verify(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder dec = Base64.getDecoder();
            byte[] salt = dec.decode(parts[2]);
            byte[] expected = dec.decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * Burns roughly the same amount of CPU as a real verification. Called when
     * the email does not exist so that a missing account and a wrong password
     * take the same time to answer.
     */
    public static void burn(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        pbkdf2(rawPassword == null ? new char[0] : rawPassword.toCharArray(), salt, ITERATIONS);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Password hashing is unavailable", ex);
        }
    }

    /** Only used by the seeder to show the digest shape in the server log. */
    public static String describe(String stored) {
        if (stored == null) {
            return "none";
        }
        return stored.length() > 24
                ? stored.substring(0, 24) + "..." : stored;
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
