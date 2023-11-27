package dev.zerek.featherjoindate.listeners;

import dev.zerek.featherjoindate.FeatherJoinDate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.MetadataValue;

public class PlayerJoinListener implements Listener {

    private final FeatherJoinDate plugin;

    public PlayerJoinListener(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    private boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isVanished(event.getPlayer())) {
            plugin.getJoinManager().storeJoin(event.getPlayer());
        }
    }
}
