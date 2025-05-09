package dev.zerek.featherjoindate;

import dev.zerek.featherjoindate.commands.SeenCommand;
import dev.zerek.featherjoindate.configs.JoinDateConfig;
import dev.zerek.featherjoindate.configs.JoinDateMessages;
import dev.zerek.featherjoindate.listeners.PlayerJoinListener;
import dev.zerek.featherjoindate.listeners.PlayerShowListener;
import dev.zerek.featherjoindate.managers.DatabaseManager;
import dev.zerek.featherjoindate.managers.JoinManager;
import dev.zerek.featherjoindate.utils.MineToolsAPIUtility;
import org.bukkit.plugin.java.JavaPlugin;

public final class FeatherJoinDate extends JavaPlugin {

    private JoinDateConfig joinDateConfig;
    private JoinDateMessages joinDateMessages;
    private DatabaseManager databaseManager;
    private JoinManager joinManager;
    private MineToolsAPIUtility mineToolsAPIUtility;

    @Override
    public void onEnable() {
        this.joinDateConfig = new JoinDateConfig(this);
        this.joinDateMessages = new JoinDateMessages(this);
        this.databaseManager = new DatabaseManager(this, this.joinDateConfig);
        this.joinManager = new JoinManager(this);
        this.mineToolsAPIUtility = new MineToolsAPIUtility(this);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerShowListener(this), this);
        this.getCommand("seen").setExecutor(new SeenCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public JoinDateMessages getJoinDateMessages() {
        return this.joinDateMessages;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public JoinManager getJoinManager() {
        return this.joinManager;
    }
    
    public MineToolsAPIUtility getMineToolsAPIUtility() {
        return this.mineToolsAPIUtility;
    }
}
