package com.checkin.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

/**
 * Enterprise-grade cryptographic password hasher using PBKDF2 with HMAC-SHA256.
 * Complies with NIST SP 800-132 recommendations.
 * Uses 65,536 iterations and a 128-bit (16-byte) cryptographically secure salt.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    /**
     * Hashes a plaintext password using PBKDF2 with a new random salt.
     * Output format: <salt_hex>$<iterations>$<hash_hex>
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(plaintext.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        HexFormat hex = HexFormat.of();
        return hex.formatHex(salt) + "$" + ITERATIONS + "$" + hex.formatHex(hash);
    }

    /**
     * Verifies a plaintext password against a stored PBKDF2 hash string.
     * Protects against timing attacks using MessageDigest.isEqual.
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split("\\$");
        if (parts.length != 3) {
            // Legacy / plaintext fallback comparison if migrating unhashed accounts
            return MessageDigest.isEqual(plaintext.getBytes(), storedHash.getBytes());
        }

        try {
            HexFormat hex = HexFormat.of();
            byte[] salt = hex.parseHex(parts[0]);
            int iterations = Integer.parseInt(parts[1]);
            byte[] expectedHash = hex.parseHex(parts[2]);

            byte[] computedHash = pbkdf2(plaintext.toCharArray(), salt, iterations, expectedHash.length * 8);

            return MessageDigest.isEqual(computedHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cryptographic algorithm unavailable: " + ALGORITHM, e);
        }
    }
}
