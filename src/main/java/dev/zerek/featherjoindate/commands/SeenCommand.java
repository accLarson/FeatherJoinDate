package dev.zerek.featherjoindate.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zerek.featherjoindate.FeatherJoinDate;
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

        switch (args.length) {
            case 0:
                if (!sender.hasPermission("feather.seen")) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-not-player"));
                    return true;
                }
                // Checks passed.
                sender.sendMessage(this.formatJoinDateMessage((OfflinePlayer) sender,true));
                return true;

            case 1:
                if (!sender.hasPermission("feather.seen.others")) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-no-permission"));
                    return true;
                }
                
                // Start asynchronous processing
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(args[0]);

                            if (!plugin.getJoinManager().isPlayerStored(offlinePlayer)) {
                                List<String> uuids = plugin.getJoinManager().getUsernameUUIDs(args[0]);
                                if (uuids.size() == 1) {
                                    String currentUsername = fetchPlayerNameFromUUID(uuids.get(0));
                                    OfflinePlayer offlinePlayer1 = plugin.getServer().getOfflinePlayer(currentUsername);
                                    
                                    // Switch back to main thread to send messages
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            sender.sendMessage(plugin.getJoinDateMessages().get("warning-old-username", Map.of(
                                                    "player", args[0],
                                                    "currentusername", currentUsername
                                            )));
                                            sender.sendMessage(formatJoinDateMessage(offlinePlayer1, false));
                                        }
                                    }.runTask(plugin);
                                } else if (uuids.size() > 1) {
                                    final String usernamesJoined = uuids.stream()
                                        .map(SeenCommand.this::fetchPlayerNameFromUUID)
                                        .collect(Collectors.joining(", "));
                                    
                                    // Switch back to main thread to send message
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player-multiple", Map.of(
                                                    "usernames", usernamesJoined
                                            )));
                                        }
                                    }.runTask(plugin);
                                } else {
                                    // Switch back to main thread to send message
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player", Map.of(
                                                    "player", args[0]
                                            )));
                                        }
                                    }.runTask(plugin);
                                }
                            } else {
                                // Player is stored, send join date message
                                final TextComponent message = formatJoinDateMessage(offlinePlayer, false);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        sender.sendMessage(message);
                                    }
                                }.runTask(plugin);
                            }
                        } finally {
                        }
                    }
                }.runTaskAsynchronously(plugin);
                
                return true;

            default:
                sender.sendMessage(plugin.getJoinDateMessages().get("error-arg-count"));
                return true;
        }
    }

    private TextComponent formatJoinDateMessage(OfflinePlayer offlinePlayer, boolean self) {
        String name = offlinePlayer.getName();

        plugin.getLogger().info("Formatting join date message for player: " + name);

        long joinDateMillis = plugin.getJoinManager().getJoinDate(offlinePlayer);
        String joinDate = "Unknown";
        String joinTime = "Unknown";
        if (joinDateMillis > 0) {
            joinDate = dateFormatter.format(Instant.ofEpochMilli(joinDateMillis));
            joinTime = timeFormatter.format(Instant.ofEpochMilli(joinDateMillis));
        }

        long lastLoginMillis = plugin.getJoinManager().getLastLogin(offlinePlayer);
        String lastLoginDate = "Unknown";
        String lastLoginTime = "Unknown";
        if (lastLoginMillis > 0) {
            lastLoginDate = dateFormatter.format(Instant.ofEpochMilli(lastLoginMillis));
            lastLoginTime = timeFormatter.format(Instant.ofEpochMilli(lastLoginMillis));
        }

        String usernames = String.join(", ", plugin.getJoinManager().GetPreviousUsernames(offlinePlayer, name));
        boolean hasPastUsernames = !usernames.isEmpty();

        plugin.getLogger().info("Join date: " + joinDate + " " + joinTime);
        plugin.getLogger().info("Last login: " + lastLoginDate + " " + lastLoginTime);

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

        return plugin.getJoinDateMessages().get(messageKey, messageParams);
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
        return null;    }


}
