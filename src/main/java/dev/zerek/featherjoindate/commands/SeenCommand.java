package dev.zerek.featherjoindate.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zerek.featherjoindate.FeatherJoinDate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Level;

public class SeenCommand implements CommandExecutor {

    private final FeatherJoinDate plugin;

    private static final DateTimeFormatter dateFormatter  = DateTimeFormatter.ofPattern("MMM dd, uuuu").withZone(ZoneId.of("America/Toronto"));
    private static final DateTimeFormatter timeFormatter  = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.of("America/Toronto"));


    public SeenCommand(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("feather.seen")) {
            sender.sendMessage(plugin.getJoinDateMessages().get("error-no-permission"));
            return true;
        }

        OfflinePlayer offlinePlayer;

        switch (args.length) {
            case 0:
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-not-player"));
                    return true;
                }
                offlinePlayer = (OfflinePlayer) sender;
                break;

            case 1:
                offlinePlayer = plugin.getServer().getOfflinePlayer(args[0]);
                break;

            default:
                sender.sendMessage(plugin.getJoinDateMessages().get("error-arg-count"));
                return true;
        }

        // Create a CompletableFuture to handle the async data gathering
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Create a data container to hold all the information
                    class PlayerData {
                        long joinDate = 0;
                        long lastLogin = 0;
                        List<String> previousUsernames = new ArrayList<>();
                    }
                    
                    PlayerData data = new PlayerData();
                    
                    // First verify the player exists in database
                    boolean isStored = plugin.getJoinManager().isPlayerStored(offlinePlayer);
                    
                    if (!isStored) {
                        handleUnseenPlayer(sender, args[0]);
                        return;
                    }

                    // Gather all required data before proceeding
                    data.joinDate = plugin.getJoinManager().getJoinDate(offlinePlayer);
                    data.lastLogin = plugin.getJoinManager().getLastLogin(offlinePlayer);
                    data.previousUsernames = plugin.getJoinManager().GetPreviousUsernames(
                            offlinePlayer, 
                            offlinePlayer.getName()
                    );

                    // Switch back to main thread to send the message
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sender.sendMessage(formatJoinDateMessage(
                                offlinePlayer, false, 
                                data.joinDate, 
                                data.lastLogin, 
                                data.previousUsernames));
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error processing seen command", e);
                }
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }

    private TextComponent formatJoinDateMessage(OfflinePlayer offlinePlayer, boolean self, 
            long joinDateMillis, long lastLoginMillis, List<String> previousUsernames) {
        String name = offlinePlayer.getName();
        boolean hasUnknownValues = false;


        String joinDate = "Unknown";
        String joinTime = "Unknown";
        if (joinDateMillis > 0) {
            joinDate = dateFormatter.format(Instant.ofEpochMilli(joinDateMillis));
            joinTime = timeFormatter.format(Instant.ofEpochMilli(joinDateMillis));
        } else {
            hasUnknownValues = true;
        }

        String lastLoginDate = "Unknown";
        String lastLoginTime = "Unknown";
        if (lastLoginMillis > 0) {
            lastLoginDate = dateFormatter.format(Instant.ofEpochMilli(lastLoginMillis));
            lastLoginTime = timeFormatter.format(Instant.ofEpochMilli(lastLoginMillis));
        } else {
            hasUnknownValues = true;
        }

        boolean hasPastUsernames = !previousUsernames.isEmpty();
        String usernames = String.join(", ", previousUsernames);


        Map<String, String> messageParams = new HashMap<>();
        messageParams.put("player", name != null ? name : "Unknown");
        messageParams.put("joindate", joinDate);
        messageParams.put("jointime", joinTime);
        messageParams.put("lastlogindate", lastLoginDate);
        messageParams.put("lastlogintime", lastLoginTime);
        messageParams.put("usernames", usernames);

        // Set 'timeonline' only if player is online
        if (offlinePlayer.isOnline() && lastLoginMillis > 0) {
            Duration duration = Duration.between(Instant.ofEpochMilli(lastLoginMillis), Instant.now());
            String timeOnline = formatDuration(duration);
            messageParams.put("timeonline", timeOnline);
        }

        // Select the appropriate message key
        String messageKey;
        if (offlinePlayer.isOnline() && !isVanished(offlinePlayer.getPlayer())) {
            messageKey = hasPastUsernames ? "joindate" : "joindate-no-usernames";
        } else {
            messageKey = hasPastUsernames ? "joindate-offline" : "joindate-offline-no-usernames";
        }

        TextComponent baseMessage = plugin.getJoinDateMessages().get(messageKey, messageParams);
        
        if (hasUnknownValues) return plugin.getJoinDateMessages().get("unknown-value-warning").append(Component.newline()).append(baseMessage);
        else return baseMessage;
    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() - (hours * 60);
        return String.format("%d hours, %d minutes", hours, minutes);
    }

    private boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    private void handleUnseenPlayer(CommandSender sender, String playerName) {
        try {
            List<String> uuids = plugin.getJoinManager().getUsernameUUIDs(playerName);

            if (uuids.size() > 1) {
                // Multiple UUIDs found - collect current usernames for all UUIDs
                List<String> currentUsernames = uuids.stream()
                    .map(this::fetchPlayerNameFromUUID)
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
                
                // Send the multiple users message
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sender.sendMessage(plugin.getJoinDateMessages().get(
                            "error-unseen-player-multiple",
                            Map.of("usernames", String.join(", ", currentUsernames))
                        ));
                    }
                }.runTask(plugin);
            } else if (uuids.size() == 1) {
                String currentUsername = fetchPlayerNameFromUUID(uuids.get(0));
                if (currentUsername == null || currentUsername.equals(playerName)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            sendNeverJoinedMessage(sender, playerName);
                        }
                    }.runTask(plugin);
                    return;
                }
                
                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(currentUsername);
                
                // Gather all data before proceeding
                long joinDate = plugin.getJoinManager().getJoinDate(offlinePlayer);
                long lastLogin = plugin.getJoinManager().getLastLogin(offlinePlayer);
                List<String> previousUsernames = plugin.getJoinManager().GetPreviousUsernames(
                        offlinePlayer, 
                        currentUsername
                );
                
                // Switch back to main thread to send messages
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        sender.sendMessage(plugin.getJoinDateMessages().get("warning-old-username", Map.of(
                                "player", playerName,
                                "currentusername", currentUsername
                        )));
                        sender.sendMessage(formatJoinDateMessage(offlinePlayer, false, joinDate, lastLogin, previousUsernames));
                    }
                }.runTask(plugin);
            } else sendNeverJoinedMessage(sender, playerName);  // No UUIDs found
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling unseen player", e);
        }
    }

    private void sendNeverJoinedMessage(CommandSender sender, String playerName) {
        Map<String, String> params = new HashMap<>();
        params.put("player", playerName);
        sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player", params));
    }

    public String fetchPlayerNameFromUUID(String uuid) {
        try {
            URL url = new URL("https://api.mojang.com/user/profile/" + uuid.replace("-", ""));
            try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element.isJsonObject()) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    return jsonObject.get("name").getAsString();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error fetching player name for UUID " + uuid + ": " + e.getMessage());
        }
        return null;    
    }
}
