package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.configs.JoinDateConfig;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
        this.initMySQLTables();
    }

    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to establish database connection: " + e.getMessage());
            throw e;
        }
    }

    private boolean existsTable(String table) {
        try (Connection connection = this.getConnection()) {
            ResultSet set = connection.getMetaData().getTables(null, null, table, null);
            return set.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Unable to query table metadata.");
            return false;
        }
    }

    private boolean columnExists(String table, String column) {
        try (Connection connection = this.getConnection()) {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getColumns(null, null, table, column);
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Unable to check if column exists: " + column);
            return false;
        }
    }

    private boolean uniqueConstraintExists(String table, String constraintName) {
        try (Connection connection = this.getConnection()) {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getIndexInfo(null, null, table, true, false);
            while (rs.next()) {
                if (constraintName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Unable to check if unique constraint exists: " + constraintName);
            return false;
        }
    }

    private boolean foreignKeyExists(String table, String constraintName) {
        try (Connection connection = this.getConnection()) {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getImportedKeys(null, null, table);
            while (rs.next()) {
                String fkName = rs.getString("FK_NAME");
                if (fkName != null && fkName.equalsIgnoreCase(constraintName)) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            plugin.getLogger().severe("Unable to check if foreign key exists: " + constraintName);
            return false;
        }
    }

    private void createTableIfNotExists(String table, String primaryColumn, String primaryColumnDefinition) {
        if (!existsTable(table)) {
            String query = String.format("CREATE TABLE IF NOT EXISTS `%s` (`%s` %s)", table, primaryColumn, primaryColumnDefinition);
            try (Connection connection = this.getConnection()) {
                connection.createStatement().execute(query);
                plugin.getLogger().info(String.format("Created table '%s' with primary key '%s'", table, primaryColumn));
            } catch (SQLException e) {
                plugin.getLogger().severe(String.format("Failed to create base `%s` table.", table));
            }
        }
    }

    private void addColumnIfNotExists(String table, String column, String columnDefinition) {
        if (!columnExists(table, column)) {
            try (Connection connection = this.getConnection()) {
                String query = String.format("ALTER TABLE `%s` ADD COLUMN `%s` %s", table, column, columnDefinition);
                connection.createStatement().execute(query);
                plugin.getLogger().info(String.format("Added column '%s' to table '%s'", column, table));
            } catch (SQLException e) {
                plugin.getLogger().severe(String.format("Failed to add column '%s' to table '%s'", column, table));
            }
        }
    }

    private void addUniqueConstraintIfNotExists(String table, String constraintName, String... columns) {
        if (!uniqueConstraintExists(table, constraintName)) {
            try (Connection connection = this.getConnection()) {
                String columnList = String.join("`, `", columns);
                String query = String.format("ALTER TABLE `%s` ADD UNIQUE INDEX `%s` (`%s`)", table, constraintName, columnList);
                connection.createStatement().execute(query);
                plugin.getLogger().info(String.format("Added unique constraint '%s' to table '%s'", constraintName, table));
            } catch (SQLException e) {
                plugin.getLogger().severe(String.format("Failed to add unique constraint '%s' to table '%s': %s", constraintName, table, e.getMessage()));
            }
        }
    }

    private void addForeignKeyIfNotExists(String table, String constraintName, String column, String refTable, String refColumn) {
        if (!foreignKeyExists(table, constraintName)) {
            try (Connection connection = this.getConnection()) {
                String query = String.format("ALTER TABLE `%s` ADD CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s`(`%s`)", 
                    table, constraintName, column, refTable, refColumn);
                connection.createStatement().execute(query);
                plugin.getLogger().info(String.format("Added foreign key '%s' to table '%s'", constraintName, table));
            } catch (SQLException e) {
                plugin.getLogger().severe(String.format("Failed to add foreign key '%s' to table '%s': %s", constraintName, table, e.getMessage()));
            }
        }
    }

    private void initMySQLTables() {
        // Create joins Table
        createTableIfNotExists("joins", "mojang_uuid", "VARCHAR(64) PRIMARY KEY");
        addColumnIfNotExists("joins", "last_login", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        addColumnIfNotExists("joins", "joindate", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

        // Create usernames Table
        createTableIfNotExists("usernames", "id", "INT AUTO_INCREMENT PRIMARY KEY");
        addColumnIfNotExists("usernames", "mojang_uuid", "VARCHAR(64)");
        addColumnIfNotExists("usernames", "username", "VARCHAR(32)");
        addForeignKeyIfNotExists("usernames", "fk_usernames_mojang_uuid", "mojang_uuid", "joins", "mojang_uuid");
        addUniqueConstraintIfNotExists("usernames", "unique_uuid_username", "mojang_uuid", "username");
    }
}
