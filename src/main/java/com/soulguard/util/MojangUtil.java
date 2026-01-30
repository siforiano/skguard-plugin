package com.soulguard.util;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MojangUtil {

    private static final String MOJANG_PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";

    /**
     * Verifies if a username belongs to a premium account and returns its official UUID.
     * 
     * @param username The username to check.
     * @return The Mojang UUID if premium, null otherwise.
     */
    public static UUID getMojangUUID(String username) {
        try {
            URL url = URI.create(MOJANG_PROFILE_URL + username).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        response.append(scanner.nextLine());
                    }
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                    String id = json.get("id").getAsString();
                    return parseUUID(id);
                }
            }
        } catch (java.io.IOException ignored) {
            // Silently fail if API is unreachable or rate limited
        }
        return null;
    }

    private static UUID parseUUID(String id) {
        // Mojang IDs are hex without dashes: 8-4-4-4-12
        String formatted = id.replaceFirst(
            "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
            "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(formatted);
    }
}
