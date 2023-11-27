package dev.zerek.featherjoindate.listeners;

import de.myzelyam.api.vanish.PlayerShowEvent;
import dev.zerek.featherjoindate.FeatherJoinDate;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerShowListener implements Listener {

    private final FeatherJoinDate plugin;

    public PlayerShowListener(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerShow(PlayerShowEvent event) {
        plugin.getJoinManager().storeJoin(event.getPlayer());
    }

}
