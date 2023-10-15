package dev.zerek.featherjoindate;

import dev.zerek.featherjoindate.configs.JoinDateConfig;
import dev.zerek.featherjoindate.configs.JoinDateMessages;
import dev.zerek.featherjoindate.managers.DatabaseManager;
import dev.zerek.featherjoindate.managers.JoinManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FeatherJoinDate extends JavaPlugin {

    private JoinDateConfig joinDateConfig;
    private JoinDateMessages joinDateMessages;
    private DatabaseManager databaseManager;
    private JoinManager joinManager;


    @Override
    public void onEnable() {
        this.joinDateConfig = new JoinDateConfig(this);
        this.joinDateMessages = new JoinDateMessages(this);
        this.databaseManager = new DatabaseManager(this);
        this.joinManager = new JoinManager(this);
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) this.databaseManager.close();
        // Plugin shutdown logic
    }

    public JoinDateConfig getJoinDateConfig() {
        return this.joinDateConfig;
    }

    public JoinDateMessages getJoinDateMessages() {
        return this.joinDateMessages;
    }

    public  JoinManager getJoinManager() {
        return this.joinManager;
    }
}
