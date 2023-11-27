package dev.zerek.featherjoindate.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.zerek.featherjoindate.FeatherJoinDate;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
import java.util.UUID;
import java.util.stream.Collectors;

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
                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(args[0]);

                if (!plugin.getJoinManager().isPlayerStored(offlinePlayer)) {
                    List<String> uuids = plugin.getJoinManager().getUsernameUUIDs(args[0]);
                    if (uuids.size() == 1) {
                        OfflinePlayer offlinePlayer1 = plugin.getServer().getOfflinePlayer(fetchPlayerNameFromUUID(uuids.get(0)));
                        sender.sendMessage(plugin.getJoinDateMessages().get("warning-old-username",Map.of(
                                "player", args[0],
                                "currentusername", fetchPlayerNameFromUUID(uuids.get(0))
                        )));
                        sender.sendMessage(this.formatJoinDateMessage(offlinePlayer1, false));
                    } else if (uuids.size() > 1) {
                        sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player-multiple",Map.of(
                                "usernames", uuids.stream().map(this::fetchPlayerNameFromUUID).collect(Collectors.joining(", "))
                        )));
                    } else {
                        sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player",Map.of(
                                "player", args[0]
                        )));
                    }
                    return true;
                }

                sender.sendMessage(this.formatJoinDateMessage(offlinePlayer, false));
                return true;

            default:
                sender.sendMessage(plugin.getJoinDateMessages().get("error-arg-count"));
                return true;
        }
    }

    private TextComponent formatJoinDateMessage(OfflinePlayer offlinePlayer, boolean self) {
        String name = offlinePlayer.getName();

        String joinDate = dateFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getJoinDate((offlinePlayer))));
        String joinTime = timeFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getJoinDate((offlinePlayer))));

        String lastLoginDate = dateFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getLastLogin(offlinePlayer)));
        String lastLoginTime = timeFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getLastLogin(offlinePlayer)));

        String usernames = String.join(", ", plugin.getJoinManager().GetPreviousUsernames(offlinePlayer, name));
        boolean hasPastUsernames = !usernames.isEmpty();

        Map<String, String> messageParams = new HashMap<>();
        messageParams.put("player", name);
        messageParams.put("joindate", joinDate);
        messageParams.put("jointime", joinTime);
        messageParams.put("lastlogindate", lastLoginDate);
        messageParams.put("lastlogintime", lastLoginTime);
        messageParams.put("usernames", usernames);

        // Set 'timeonline' only if player is online
        if (offlinePlayer.isOnline()) {
            Duration duration = Duration.between(Instant.ofEpochMilli(plugin.getJoinManager().getLastLogin(offlinePlayer)), Instant.now());
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
            InputStreamReader reader = new InputStreamReader(url.openStream());
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                return jsonObject.get("name").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;    }


}
