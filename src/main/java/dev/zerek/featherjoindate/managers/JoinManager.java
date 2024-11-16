package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import org.bukkit.OfflinePlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class JoinManager {

    private final FeatherJoinDate plugin;

    public JoinManager(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    public boolean isPlayerStored(OfflinePlayer offlinePlayer) {
        try {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT COUNT(*) FROM joins WHERE mojang_uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, offlinePlayer.getUniqueId().toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1) > 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking if player is stored: " + e.getMessage());
        }
        return false;
    }

    public boolean isUsernameStored(OfflinePlayer offlinePlayer) {
        try {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT COUNT(*) FROM usernames WHERE mojang_uuid = ? AND username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, offlinePlayer.getUniqueId().toString());
                    stmt.setString(2, offlinePlayer.getName());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1) > 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking if username is stored: " + e.getMessage());
        }
        return false;
    }

    public List<String> getUsernameUUIDs(String username) {
        List<String> uuids = new ArrayList<>();
        try {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT mojang_uuid FROM usernames WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            uuids.add(rs.getString("mojang_uuid"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting username UUIDs: " + e.getMessage());
        } finally {
            return uuids;
        }
    }

    public void storeJoin(OfflinePlayer offlinePlayer) {
        String uuid = offlinePlayer.getUniqueId().toString();

        try {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                if (!isPlayerStored(offlinePlayer)) {
                    String query = "INSERT INTO joins (mojang_uuid) VALUES (?)";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                } else {
                    String query = "UPDATE joins SET last_login = CURRENT_TIMESTAMP WHERE mojang_uuid = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                }

                if (!isUsernameStored(offlinePlayer)) {
                    String query = "INSERT INTO usernames (mojang_uuid, username) VALUES (?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, uuid);
                        stmt.setString(2, offlinePlayer.getName());
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error storing join: " + e.getMessage());
        } finally {
            // No need to close connection as it's handled by try-with-resources
        }
    }

    public long getJoinDate(OfflinePlayer offlinePlayer) {
        try {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT joindate FROM joins WHERE mojang_uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, offlinePlayer.getUniqueId().toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Timestamp joinDate = rs.getTimestamp("joindate");
                            return joinDate != null ? joinDate.getTime() : 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting join date for " + offlinePlayer.getName(), e);
        }
        plugin.getLogger().warning("No join date found for player: " + offlinePlayer.getName());
        return 0;
    }

    public long getLastLogin(OfflinePlayer offlinePlayer) {
        try {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT last_login FROM joins WHERE mojang_uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, offlinePlayer.getUniqueId().toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Timestamp lastLogin = rs.getTimestamp("last_login");
                            return lastLogin != null ? lastLogin.getTime() : 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting last login for " + offlinePlayer.getName(), e);
        }
        plugin.getLogger().warning("No last login found for player: " + offlinePlayer.getName());
        return 0;
    }

    public List<String> GetPreviousUsernames(OfflinePlayer offlinePlayer, String currentUsername) {
        List<String> previousUsernames = new ArrayList<>();
        try {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT username FROM usernames WHERE mojang_uuid = ? AND username != ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, offlinePlayer.getUniqueId().toString());
                    stmt.setString(2, currentUsername);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            previousUsernames.add(rs.getString("username"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting previous usernames: " + e.getMessage());
        } finally {
            return previousUsernames;
        }
    }
}
