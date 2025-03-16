package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.configs.JoinDateConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final FeatherJoinDate plugin;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseManager(FeatherJoinDate plugin, JoinDateConfig joinDateConfig) {
        this.plugin = plugin;
        this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s",
            joinDateConfig.getMysqlHost(),
            joinDateConfig.getMysqlPort(),
            joinDateConfig.getMysqlDatabase());
        this.username = joinDateConfig.getMysqlUsername();
        this.password = joinDateConfig.getMysqlPassword();
        this.initTables();
    }

    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to establish database connection: " + e.getMessage());
            throw e;
        }
    }

    private void initTables() {
        plugin.getLogger().info("Ensuring joins table exists.");
        String query = "CREATE TABLE IF NOT EXISTS `joins` ("
                + " `mojang_uuid`   VARCHAR(64) PRIMARY KEY, "
                + " `last_login`    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + " `joindate`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection()) {
            conn.createStatement().execute(query);
        } catch (Exception e) {
            plugin.getLogger().severe("Unable to ensure joins table exists.");
            plugin.getLogger().warning(e.getMessage());
        }

        String query2 = "CREATE TABLE IF NOT EXISTS `usernames` ("
                + " `id`            INT AUTO_INCREMENT PRIMARY KEY, "
                + " `mojang_uuid`   VARCHAR(64), "
                + " `username`      VARCHAR(32), "
                + " FOREIGN KEY (mojang_uuid) REFERENCES joins(mojang_uuid))";
        try (Connection conn = getConnection()) {
            conn.createStatement().execute(query2);
        } catch (Exception e) {
            plugin.getLogger().severe("Unable to ensure usernames table exists.");
            plugin.getLogger().warning(e.getMessage());
        }
    }
}
