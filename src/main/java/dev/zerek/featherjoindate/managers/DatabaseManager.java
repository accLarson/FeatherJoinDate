package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.configs.JoinDateConfig;

import java.io.File;
import java.sql.*;

public class DatabaseManager {

    private static Connection connection;
    private final FeatherJoinDate plugin;
    private final JoinDateConfig joinDateConfig;
    private final boolean isMySQLEnabled;

    public DatabaseManager(FeatherJoinDate plugin) {
        this.plugin = plugin;
        this.joinDateConfig = plugin.getJoinDateConfig();
        this.isMySQLEnabled = this.joinDateConfig.isMysqlEnabled();
        this.initMySQLConnection();
        if (connection != null) {
            this.initTables();
        }
    }

    private void initMySQLConnection(){
        JoinDateConfig config = this.plugin.getJoinDateConfig();

        String host = config.getMysqlHost();
        int port = config.getMysqlPort();
        String database = config.getMysqlDatabase();

        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);

        String username = config.getMysqlUsername();
        String password = config.getMysqlPassword();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DatabaseManager.connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException | ClassNotFoundException exception) {
            plugin.getLogger().severe("Unable to initialize connection.");
            plugin.getLogger().severe("Ensure connection can be made with provided config.yml MySQL strings.");
            plugin.getLogger().severe("Connection URL: " + url);
            plugin.getLogger().severe("Disabling FeatherJoinDate.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }

    }

    public void close() {
        try {
            if (DatabaseManager.connection != null) {
                if (!DatabaseManager.connection.isClosed()){
                    DatabaseManager.connection.close();
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Unable to close connection.");
        }
    }

    private boolean existsTable(String table) {
        try {
            if (!connection.isClosed()) {
                ResultSet rs = connection.getMetaData().getTables(null, null, table, null);
                return rs.next();
            } else {
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Unable to query table metadata.");
            return false;
        }
    }

    private void initTables() {
        if (!this.existsTable("JOINS")) {
            plugin.getLogger().info("Creating JOINS table.");
            String query = "CREATE TABLE IF NOT EXISTS `JOINS` ("
                    + " `mojang_uuid` VARCHAR(255) PRIMARY KEY, "
                    + " `joindate` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, ";
            try {
                if (!connection.isClosed()) {
                    connection.createStatement().execute(query);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                plugin.getLogger().severe("Unable to create JOINS table.");
            }
        }
    }

    public boolean executeUpdate(String update) {
        if (DatabaseManager.connection == null) {
            return false;
        }
        Statement statement;
        try {
            statement = DatabaseManager.connection.createStatement();
            statement.executeUpdate(update);
            statement.close();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to execute update: ||| " + update + " |||");
            return false;
        }
    }

    public ResultSet executeQuery(String query) {
        if (DatabaseManager.connection == null) {
            return null;
        }
        try {
            return DatabaseManager.connection.createStatement().executeQuery(query);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to execute query: ||| " + query + " |||");
            return null;
        }
    }
}
