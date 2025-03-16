package dev.zerek.featherjoindate.utils;

import dev.zerek.featherjoindate.FeatherJoinDate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageFormatterUtility {
    
    /**
     * Formats a join date message with all relevant player information
     * and selects the appropriate message template based on player status.
     */
    public static TextComponent formatJoinDateMessage(
            FeatherJoinDate plugin,
            OfflinePlayer offlinePlayer,
            long joinDateMillis,
            long lastLoginMillis,
            List<String> previousUsernames) {
        
        return formatJoinDateMessage(plugin, offlinePlayer, joinDateMillis, lastLoginMillis, previousUsernames, null);
    }
    
    /**
     * Formats a join date message with all relevant player information
     * and selects the appropriate message template based on player status.
     * Allows custom parameters to override default values.
     */
    public static TextComponent formatJoinDateMessage(
            FeatherJoinDate plugin,
            OfflinePlayer offlinePlayer,
            long joinDateMillis,
            long lastLoginMillis,
            List<String> previousUsernames,
            Map<String, String> customParams) {
        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown Player";
        boolean hasUnknownValues = TimeFormatterUtility.hasUnknownValues(joinDateMillis, lastLoginMillis);

        String joinDate = TimeFormatterUtility.formatDate(joinDateMillis);
        String joinTime = TimeFormatterUtility.formatTime(joinDateMillis);

        String lastLoginDate = TimeFormatterUtility.formatDate(lastLoginMillis);
        String lastLoginTime = TimeFormatterUtility.formatTime(lastLoginMillis);

        boolean hasPastUsernames = !previousUsernames.isEmpty();
        String usernames = String.join(", ", previousUsernames);

        Map<String, String> messageParams = new HashMap<>();
        messageParams.put("player", name);
        messageParams.put("joindate", joinDate);
        messageParams.put("jointime", joinTime);
        messageParams.put("lastlogindate", lastLoginDate);
        messageParams.put("lastlogintime", lastLoginTime);
        messageParams.put("usernames", usernames);

        if (offlinePlayer.isOnline() && lastLoginMillis > 0) {
            String timeOnline = TimeFormatterUtility.formatOnlineTime(lastLoginMillis);
            messageParams.put("timeonline", timeOnline);
        }

        // Apply any custom parameters that should override the defaults
        if (customParams != null) {
            for (Map.Entry<String, String> entry : customParams.entrySet()) {
                messageParams.put(entry.getKey(), entry.getValue());
            }
        }

        // Select the appropriate message template based on player status
        String messageKey = selectMessageTemplate(offlinePlayer, hasPastUsernames);
        TextComponent baseMessage = plugin.getJoinDateMessages().get(messageKey, messageParams);
        
        if (hasUnknownValues) return plugin.getJoinDateMessages().get("unknown-value-warning").append(Component.newline()).append(baseMessage);
        else return baseMessage;
    }

    /**
     * Selects the appropriate message template based on player status and username history.
     * 
     * @param offlinePlayer The player to check status for
     * @param hasPastUsernames Whether the player has previous usernames
     * @return The message key to use
     */
    private static String selectMessageTemplate(OfflinePlayer offlinePlayer, boolean hasPastUsernames) {
        boolean isEffectivelyOnline = offlinePlayer.isOnline() && 
                                     (offlinePlayer.getPlayer() != null && !isVanished(offlinePlayer.getPlayer()));
        
        if (isEffectivelyOnline) {
            return hasPastUsernames ? "joindate" : "joindate-no-usernames";
        } else {
            return hasPastUsernames ? "joindate-offline" : "joindate-offline-no-usernames";
        }
    }

    /**
     * Checks if a player is vanished (invisible to other players).
     */
    private static boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
