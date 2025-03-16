package dev.zerek.featherjoindate.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MineToolsAPIUtility {
    private static final String API_URL = "https://api.minetools.eu/uuid/";
    private static final Map<UUID, CachedUsername> usernameCache = new HashMap<>();
    private static final long CACHE_EXPIRY_MS = 10 * 60 * 1000; // 24 hours
    
    private final Plugin plugin;
    
    public MineToolsAPIUtility(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Gets the current username for a UUID, using the MineTools API.
     * Results are cached to reduce API calls.
     *
     * @param uuid The UUID to look up
     * @return A CompletableFuture that will contain the username, or null if not found
     */
    public CompletableFuture<String> getCurrentUsername(UUID uuid) {
        // Check cache first
        CachedUsername cached = usernameCache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.getUsername());
        }
        
        // Not in cache or expired, make API call
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().info("Making MineTools API request for UUID: " + uuid +
                                       " (cache " + (cached != null ? "expired" : "miss") + ")");
                String apiResponse = makeApiRequest(uuid.toString().replace("-", ""));

                String username = parseUsernameFromResponse(apiResponse);

                if (username != null) usernameCache.put(uuid, new CachedUsername(username));
                
                return username;
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error fetching username from MineTools API", e);
                return null;
            }
        });
    }
    
    /**
     * Makes an HTTP request to the MineTools API.
     *
     * @param uuidWithoutDashes The UUID without dashes
     * @return The API response as a string
     * @throws IOException If an error occurs during the request
     */
    private String makeApiRequest(String uuidWithoutDashes) throws IOException {
        URL url = new URL(API_URL + uuidWithoutDashes);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } finally {
            connection.disconnect();
        }
        
        return response.toString();
    }
    
    /**
     * Parses the username from the API response JSON.
     *
     * @param response The API response as a string
     * @return The username, or null if not found or invalid response
     */
    private String parseUsernameFromResponse(String response) {
        try {
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            
            // Check if the response contains an error
            if (jsonObject.has("status") && "ERR".equals(jsonObject.get("status").getAsString())) {
                return null;
            }
            
            // Get the name from the response
            if (jsonObject.has("name")) {
                return jsonObject.get("name").getAsString();
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing MineTools API response", e);
            return null;
        }
    }
    
    /**
     * Helper class to store a cached username with its expiry time.
     */
    private static class CachedUsername {
        private final String username;
        private final long expiryTime;
        
        public CachedUsername(String username) {
            this.username = username;
            this.expiryTime = System.currentTimeMillis() + CACHE_EXPIRY_MS;
        }
        
        public String getUsername() {
            return username;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
