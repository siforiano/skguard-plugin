package com.soulguard.modules.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {

    private static final int ITERATIONS = 100000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // Argon2 Parameters (10/10 Enterprise Security)
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_MEMORY = 65536;
    private static final int ARGON2_PARALLELISM = 4;

    public static String hashPassword(String password) {
        // We use Argon2 by default for all new registrations
        try {
            Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
            try {
                return "$argon2id$"
                        + argon2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY, ARGON2_PARALLELISM, password.toCharArray());
            } finally {
                argon2.wipeArray(password.toCharArray());
            }
        } catch (NoClassDefFoundError | Exception e) {
            // Fallback to PBKDF2 if Argon2 is not available
            char[] passwordChars = password.toCharArray();
            byte[] salt = generateSalt();
            byte[] hash = hash(passwordChars, salt);
            return "PBKDF2:" + ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                    + Base64.getEncoder().encodeToString(hash);
        }
    }

    public static boolean checkPassword(String password, String storedHash) {
        if (storedHash.startsWith("$argon2id$")) {
            Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
            try {
                return argon2.verify(storedHash.substring(10), password.toCharArray());
            } finally {
                argon2.wipeArray(password.toCharArray());
            }
        }

        // Handle legacy PBKDF2 or early format
        String[] parts = storedHash.split(":");
        if (parts.length == 4 && parts[0].equals("PBKDF2")) {
            return checkPBKDF2(password, parts[2], parts[3]);
        } else if (parts.length == 3) {
            return checkPBKDF2(password, parts[1], parts[2]);
        }

        return false;
    }

    private static boolean checkPBKDF2(String password, String saltStr, String hashStr) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltStr);
            byte[] hash = Base64.getDecoder().decode(hashStr);
            byte[] testHash = hash(password.toCharArray(), salt);
            return slowEquals(hash, testHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++)
            diff |= a[i] ^ b[i];
        return diff == 0;
    }
}
