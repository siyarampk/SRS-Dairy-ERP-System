package dairy.erp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Password hashing utility using a per-user random salt and SHA-256.
 * Stored representation: {@code <saltHex>$<hashHex>}.
 * Plain-text passwords are never persisted.
 */
public final class HashUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;

    private HashUtil() {
    }

    /**
     * Hashes a plain-text password with a fresh random salt.
     * @return a string of the form salt$hash
     */
    public static String hashPassword(String plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        String hash = sha256Hex(saltHex, plainPassword);
        return saltHex + "$" + hash;
    }

    /**
     * Verifies a plain-text password against a stored {@code salt$hash} value.
     * Constant-time comparison is used to avoid trivial timing side channels.
     */
    public static boolean verifyPassword(String plainPassword, String stored) {
        if (stored == null || !stored.contains("$")) {
            return false;
        }
        String[] parts = stored.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        String saltHex = parts[0];
        String expectedHash = parts[1];
        String actualHash = sha256Hex(saltHex, plainPassword);
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String saltHex, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(HexFormat.of().parseHex(saltHex));
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
