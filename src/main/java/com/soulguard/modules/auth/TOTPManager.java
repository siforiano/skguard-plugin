package com.soulguard.modules.auth;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

/**
 * Basic TOTP (RFC 6238) implementation for 2FA.
 */
public class TOTPManager implements SecurityModule {

    @SuppressWarnings("unused")
    private final SoulGuard plugin;
    private boolean enabled;
    private static final int SECRET_SIZE = 10; // 80 bits is enough for most
    private static final String ALGORITHM = "HmacSHA1";

    public TOTPManager(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "TOTPManager";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
    }

    public String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        new SecureRandom().nextBytes(buffer);
        return encodeBase32(buffer);
    }

    public boolean verify(String secret, int code) {
        if (secret == null || secret.isEmpty()) return false;
        long timeIndex = System.currentTimeMillis() / 1000 / 30;
        
        // Allow a small window of 1 step before/after for clock drift
        for (int i = -1; i <= 1; i++) {
            if (getTOTPCode(secret, timeIndex + i) == code) return true;
        }
        return false;
    }

    /**
     * Generates a set of secure backup codes.
     */
    public java.util.List<String> generateBackupCodes(int count) {
        java.util.List<String> codes = new java.util.ArrayList<>();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                sb.append(random.nextInt(10));
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    private int getTOTPCode(String secret, long timeIndex) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(timeIndex).array();

            SecretKeySpec signKey = new SecretKeySpec(key, ALGORITHM);
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= 1000000;

            return truncatedHash;
        } catch (GeneralSecurityException e) {
            return -1;
        }
    }

    // --- Simple Base32 Implementation ---
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= (data[next++] & 0xFF);
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = 0x1F & (buffer >> (bitsLeft - 5));
            bitsLeft -= 5;
            sb.append(BASE32_CHARS.charAt(index));
        }
        return sb.toString();
    }

    private byte[] decodeBase32(String base32) {
        base32 = base32.toUpperCase();
        byte[] bytes = new byte[base32.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (char c : base32.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer <<= 5;
            buffer |= val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[count++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
