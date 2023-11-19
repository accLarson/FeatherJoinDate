package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.configs.JoinDateConfig;
import org.javalite.activejdbc.Base;

public class DatabaseManager {

    private final FeatherJoinDate plugin;
    private final JoinDateConfig joinDateConfig;

    public DatabaseManager(FeatherJoinDate plugin) {
        this.plugin = plugin;
        this.joinDateConfig = plugin.getJoinDateConfig();
        this.initMySQLConnection();
        this.initTables();
    }

    private void initMySQLConnection() {
        JoinDateConfig config = this.plugin.getJoinDateConfig();

        String host = config.getMysqlHost();
        int port = config.getMysqlPort();
        String database = config.getMysqlDatabase();
        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        String username = config.getMysqlUsername();
        String password = config.getMysqlPassword();

        try {
            if (!Base.hasConnection()) {
                Base.open("com.mysql.cj.jdbc.Driver", url, username, password);
            }
        } catch (Exception exception) {
            plugin.getLogger().severe("Unable to initialize connection.");
            plugin.getLogger().severe("Ensure connection can be made with provided config.yml MySQL strings.");
            plugin.getLogger().severe("Connection URL: " + url);
        }
    }

    public void close() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    private void initTables() {
        plugin.getLogger().info("Ensuring joins table exists.");
        String query = "CREATE TABLE IF NOT EXISTS `joins` ("
                + " `mojang_uuid`   VARCHAR(64) PRIMARY KEY, "
                + " `updated_at`    DATETIME, "
                + " `joindate`      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";
        try {
            Base.exec(query);
        } catch (Exception e) {
            plugin.getLogger().severe("Unable to ensure joins table exists.");
            plugin.getLogger().warning(e.getMessage());
        }

        String query2 = "CREATE TABLE IF NOT EXISTS `usernames` ("
                + " `id`            INT AUTO_INCREMENT PRIMARY KEY, "
                + " `mojang_uuid`   VARCHAR(64), "
                + " `username`      VARCHAR(32), "
                + " FOREIGN KEY (mojang_uuid) REFERENCES joins(mojang_uuid))";
        try {
            Base.exec(query2);
        } catch (Exception e) {
            plugin.getLogger().severe("Unable to ensure usernames table exists.");
            plugin.getLogger().warning(e.getMessage());
        }
    }

    // Use ActiveJDBC to execute updates and queries
    public boolean executeUpdate(String update) {
        try {
            Base.exec(update);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to execute update: ||| " + update + " |||");
            return false;
        }
    }
}
