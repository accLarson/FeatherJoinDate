package dev.zerek.featherjoindate.managers;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.data.Join;
import dev.zerek.featherjoindate.data.Username;
import org.bukkit.OfflinePlayer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class JoinManager {

    private final FeatherJoinDate plugin;

    public JoinManager(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    public boolean isPlayerStored (OfflinePlayer offlinePlayer){
        return Join.exists(offlinePlayer.getUniqueId().toString());
    }

    public boolean isUsernameStored (OfflinePlayer offlinePlayer){
        Username usernameRecord = Username.findFirst("mojang_uuid = ? and username = ?", offlinePlayer.getUniqueId().toString(),offlinePlayer.getName());
        return usernameRecord != null;
    }

    public void storeJoin(OfflinePlayer offlinePlayer) {
        String uuid = offlinePlayer.getUniqueId().toString();

        if (!isPlayerStored(offlinePlayer)) {
            try {
                Join join = new Join().set("mojang_uuid", uuid);
                join.insert();
            } catch (Exception e) {
                plugin.getLogger().severe("Error inserting " + offlinePlayer.getName() + " into joins database: " + e.getMessage());
            }
        } else {
            // Player exists in the database, update the 'last_login' column
            Join join = Join.findById(uuid);
            if (join != null) {
                try {
                    join.set("last_login", Timestamp.from(Instant.now()));
                    join.saveIt();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error updating " + offlinePlayer.getName() + " latest join in database: " + e.getMessage());
                }
            } else {
                plugin.getLogger().severe(offlinePlayer.getName() + " found but unable to retrieve the record.");
            }
        }

        if (!isUsernameStored(offlinePlayer)) {
            try {
                Username username = new Username();
                username.set("mojang_uuid", offlinePlayer.getUniqueId().toString());
                username.set("username", offlinePlayer.getName());
                username.insert();
            } catch (Exception e) {
                plugin.getLogger().severe("Error inserting " + offlinePlayer.getName() + " into username database: " + e.getMessage());
            }
        }

    }


    public long getJoinDate (OfflinePlayer offlinePlayer){
        if (isPlayerStored(offlinePlayer)) {
            Join join = Join.findById(offlinePlayer.getUniqueId().toString());
            return join.getLong("joindate");
        }
        else return 0;
    }

    public long getLastLogin(OfflinePlayer offlinePlayer) {
        if (isPlayerStored(offlinePlayer)) {
            Join join = Join.findById(offlinePlayer.getUniqueId().toString());
            return join.getLong("last_login");
        }
        else return 0;
    }

    public List<String> getUsernames(OfflinePlayer offlinePlayer) {
        List<Username> offlinePlayerUsernames = Username.where("mojang_uuid = ? and username = ?", offlinePlayer.getUniqueId().toString(), offlinePlayer.getName());
        return offlinePlayerUsernames.stream().map(u -> u.getString("username")).collect(Collectors.toList());
    }

}
