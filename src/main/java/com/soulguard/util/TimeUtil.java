package com.soulguard.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(?:([0-9]+)d)?(?:([0-9]+)h)?(?:([0-9]+)m)?(?:([0-9]+)s)?");

    /**
     * Parses a duration string (e.g. "1d2h", "30m", "10s") into milliseconds.
     * 
     * @param input The duration string.
     * @return Duration in milliseconds, or 0 if unparseable.
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) return 0;
        
        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase());
        if (!matcher.matches()) return 0;

        long totalMs = 0;
        
        if (matcher.group(1) != null) totalMs += Long.parseLong(matcher.group(1)) * 24 * 60 * 60 * 1000L;
        if (matcher.group(2) != null) totalMs += Long.parseLong(matcher.group(2)) * 60 * 60 * 1000L;
        if (matcher.group(3) != null) totalMs += Long.parseLong(matcher.group(3)) * 60 * 1000L;
        if (matcher.group(4) != null) totalMs += Long.parseLong(matcher.group(4)) * 1000L;

        return totalMs;
    }

    public static String formatTimeLeft(long expiry) {
        long diff = expiry - System.currentTimeMillis();
        if (diff <= 0) return "Expired";

        long seconds = diff / 1000;
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (sb.length() == 0 || seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
