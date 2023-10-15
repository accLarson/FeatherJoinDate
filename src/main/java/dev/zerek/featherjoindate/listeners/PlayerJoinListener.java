package dev.zerek.featherjoindate.listeners;

import dev.zerek.featherjoindate.FeatherJoinDate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final FeatherJoinDate plugin;

    public PlayerJoinListener(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().hasPlayedBefore()) {
            if (plugin.getJoinManager().attemptStoreJoinDate(event.getPlayer())) {
                plugin.getLogger().info(event.getPlayer().getName() + " has just been added to the FeatherJoinDate table.");
            }
            else plugin.getLogger().info(event.getPlayer().getName() + " is already in the FeatherJoinDate table; likely a past-season player.");
        }

    }
}
