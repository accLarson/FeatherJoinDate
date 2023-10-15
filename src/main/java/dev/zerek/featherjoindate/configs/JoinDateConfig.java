package dev.zerek.featherjoindate.configs;

import dev.zerek.featherjoindate.FeatherJoinDate;
import org.bukkit.configuration.file.FileConfiguration;

public class JoinDateConfig {

    private final FeatherJoinDate plugin;
    private final FileConfiguration config;

    private String mysqlUsername;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlPassword;
    private String mysqlDatabase;
    private boolean mysqlEnabled;

    public JoinDateConfig(FeatherJoinDate plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = this.plugin.getConfig();
        this.loadConfig();
    }

    private void loadConfig() {
        this.mysqlEnabled = config.getBoolean("settings.mysql.enabled");
        this.mysqlUsername = config.getString("settings.mysql.username");
        this.mysqlHost = config.getString("settings.mysql.host");
        this.mysqlPort = config.getInt("settings.mysql.port");
        this.mysqlPassword = config.getString("settings.mysql.password");
        this.mysqlDatabase = config.getString("settings.mysql.database");
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public boolean isMysqlEnabled() {
        return mysqlEnabled;
    }
}
