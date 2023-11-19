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
        plugin.getJoinManager().storeJoin(event.getPlayer());
    }
}
