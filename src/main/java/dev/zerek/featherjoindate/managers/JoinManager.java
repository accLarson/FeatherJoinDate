package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class JoinManager {

    private final FeatherJoinDate plugin;

    /**
     * Constructs a new JoinManager.
     *
     * @param plugin The main plugin instance
     */
    public JoinManager(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    // === Data Storage ===

    /**
     * Stores or updates a player's join information in the database using a transaction.
     * - Updates last_login if player exists, creates new entry if they don't
     * - Stores username if it's not already stored
     *
     * @param offlinePlayer The player whose join information to store
     */
    public void storeJoin(OfflinePlayer offlinePlayer) {
        String uuid = offlinePlayer.getUniqueId().toString();
        String username = offlinePlayer.getName();
        
        if (username == null) {
            plugin.getLogger().warning("Cannot store join for player with null username");
            return;
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
           conn.setAutoCommit(false);
           
            try {
                // Update last_login for existing player or insert new player
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO joins (mojang_uuid, last_login) VALUES (?, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE last_login = CURRENT_TIMESTAMP")) {
                stmt.setString(1, uuid);
                stmt.executeUpdate();
                }
                
                // Insert username, unique constraint will prevent duplicates
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT IGNORE INTO usernames (mojang_uuid, username) VALUES (?, ?)")) {
                    stmt.setString(1, uuid);
                    stmt.setString(2, username);
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error storing join data: " + e.getMessage());
        }
    }

    // === Data Retrieval ===

    /**
     * Retrieves all player data in a single database query.
     *
     * @param offlinePlayer The player whose data to retrieve
     * @return A Map containing join date, last login, and previous usernames
     */
    public Map<String, Object> getPlayerFullData(OfflinePlayer offlinePlayer) {
        Map<String, Object> playerData = new HashMap<>();
        
        String query = "SELECT j.joindate, j.last_login, " +
                      "(SELECT GROUP_CONCAT(u.username) FROM usernames u " +
                      "WHERE u.mojang_uuid = j.mojang_uuid AND u.username != ?) AS previous_usernames " +
                      "FROM joins j WHERE j.mojang_uuid = ?";
                      
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String currentUsername = offlinePlayer.getName();
            String uuid = offlinePlayer.getUniqueId().toString();
            
            stmt.setString(1, currentUsername);
            stmt.setString(2, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp joinDate = rs.getTimestamp("joindate");
                    Timestamp lastLogin = rs.getTimestamp("last_login");
                    
                    playerData.put("exists", true);
                    playerData.put("joinDate", joinDate != null ? joinDate.getTime() : 0L);
                    playerData.put("lastLogin", lastLogin != null ? lastLogin.getTime() : 0L);
                    
                    String previousUsernamesStr = rs.getString("previous_usernames");
                    List<String> previousUsernames = new ArrayList<>();
                    if (previousUsernamesStr != null && !previousUsernamesStr.isEmpty()) {
                        previousUsernames = Arrays.asList(previousUsernamesStr.split(","));
                    }
                    playerData.put("previousUsernames", previousUsernames);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting player full data: " + e.getMessage());
        } finally {
            // Set default values if they weren't set in the try block
            playerData.putIfAbsent("exists", false);
            playerData.putIfAbsent("joinDate", 0L);
            playerData.putIfAbsent("lastLogin", 0L);
            playerData.putIfAbsent("previousUsernames", new ArrayList<String>());
        }
        return playerData;
    }

    /**
     * Retrieves all UUIDs associated with a specific username.
     *
     * @param username The username to look up
     * @return A List of String containing all UUIDs that have used this username
     */
    public List<String> getUsernameUUIDs(String username) {
        List<String> uuids = new ArrayList<>();
        String query = "SELECT mojang_uuid FROM usernames WHERE username = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    uuids.add(rs.getString("mojang_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting username UUIDs: " + e.getMessage());
        }
        return uuids;
    }

    /**
     * Retrieves the most recent username associated with a UUID from the database.
     *
     * @param uuid The UUID to look up
     * @return The most recent username, or null if not found
     */
    public String getMostRecentUsernameForUUID(String uuid) {
        String username = null;
        String query = "SELECT username FROM usernames WHERE mojang_uuid = ? " +
                      "ORDER BY id DESC LIMIT 1";
                      
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, uuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("username");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting username for UUID: " + e.getMessage());
        }
        return username;
    }
}
