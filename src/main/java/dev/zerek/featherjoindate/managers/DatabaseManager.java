package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.configs.JoinDateConfig;
import org.javalite.activejdbc.Base;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private final FeatherJoinDate plugin;
    private final JoinDateConfig joinDateConfig;
    private DataSource dataSource;

    public DatabaseManager(FeatherJoinDate plugin, JoinDateConfig joinDateConfig) {
        this.plugin = plugin;
        this.joinDateConfig = joinDateConfig;
        this.initDataSource();
        this.initTables();
    }

    private void initDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", 
            joinDateConfig.getMysqlHost(), 
            joinDateConfig.getMysqlPort(), 
            joinDateConfig.getMysqlDatabase()));
        config.setUsername(joinDateConfig.getMysqlUsername());
        config.setPassword(joinDateConfig.getMysqlPassword());
        config.setMaximumPoolSize(10);

        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception exception) {
            plugin.getLogger().severe("Unable to initialize connection pool.");
            plugin.getLogger().severe("Ensure connection can be made with provided config.yml MySQL settings.");
            plugin.getLogger().severe(exception.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
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

    public boolean executeUpdate(String update) {
        try (Connection conn = getConnection()) {
            conn.createStatement().executeUpdate(update);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to execute update: ||| " + update + " |||");
            plugin.getLogger().severe(e.getMessage());
            return false;
        }
    }
}
