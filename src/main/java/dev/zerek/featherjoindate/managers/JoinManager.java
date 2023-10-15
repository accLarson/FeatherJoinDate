package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.data.Join;
import org.bukkit.OfflinePlayer;

public class JoinManager {

    private final FeatherJoinDate plugin;

    public JoinManager(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    public boolean isPlayerStored (OfflinePlayer offlinePlayer){
        return Join.exists(offlinePlayer.getUniqueId().toString());
    }

    public boolean attemptStoreJoinDate(OfflinePlayer offlinePlayer){
        if (!isPlayerStored(offlinePlayer)) {
            Join join = new Join().set("mojang_uuid", offlinePlayer.getUniqueId().toString());
            join.saveIt();
            return true;
        }
        else return false;
    }

    public long getJoinDate (OfflinePlayer offlinePlayer){

        if (isPlayerStored(offlinePlayer)) {
            Join join = Join.findById(offlinePlayer.getUniqueId().toString());
            return join.getLong("joindate");
        }
        else return 0;
    }
}
