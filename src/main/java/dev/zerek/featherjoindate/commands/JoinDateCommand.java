package dev.zerek.featherjoindate.commands;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.managers.JoinManager;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

public class JoinDateCommand implements CommandExecutor {

    private final FeatherJoinDate plugin;

    public JoinDateCommand(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        switch (args.length) {
            case 0:
                if (!sender.hasPermission("feather.joindate")) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-no-permission", null));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-not-player", null));
                    return true;
                }
                // Checks passed.
                sender.sendMessage(this.formatJoinDateMessage((OfflinePlayer) sender,true));
                return true;

            case 1:
                if (!sender.hasPermission("feather.joindate.others")) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-no-permission", null));
                    return true;
                }
                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(args[0]);
                if (!plugin.getJoinManager().isPlayerStored(offlinePlayer)) {
                    sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player", null));
                    return true;
                }
                // Checks passed.
                sender.sendMessage(this.formatJoinDateMessage(offlinePlayer, false));

            default:
                sender.sendMessage(plugin.getJoinDateMessages().get("error-arg-count", null));
                return true;
        }
    }

    private TextComponent formatJoinDateMessage(OfflinePlayer offlinePlayer, boolean self) {
        DateTimeFormatter dateFormatter  = DateTimeFormatter.ofPattern("MMM dd, uuuu").withZone(ZoneId.of("America/Toronto"));
        DateTimeFormatter timeFormatter  = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.of("America/Toronto"));

        String joinDate = dateFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getJoinDate((offlinePlayer))));
        String joinTime = timeFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getJoinDate((offlinePlayer))));

        String lastLoginDate = dateFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getLastLogin(offlinePlayer)));
        String lastLoginTime = timeFormatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getLastLogin(offlinePlayer)));

        String usernames = String.join(", ", plugin.getJoinManager().getUsernames(offlinePlayer));

        if (offlinePlayer.isOnline()) {
            Duration duration = Duration.between(Instant.ofEpochMilli(plugin.getJoinManager().getLastLogin(offlinePlayer)), Instant.now());
            String timeOnline = formatDuration(duration);

            if (self) return plugin.getJoinDateMessages().get("joindate-self", Map.of(
                    "joindate", joinDate,
                    "jointime", joinTime,
                    "lastlogindate", lastLoginDate,
                    "lastLoginTime", lastLoginTime,
                    "timeonline", timeOnline,
                    "usernames", usernames
            ));
            else return plugin.getJoinDateMessages().get("joindate-other", Map.of(
                    "joindate", joinDate,
                    "jointime", joinTime,
                    "lastlogindate", lastLoginDate,
                    "lastLoginTime", lastLoginTime,
                    "timeonline", timeOnline,
                    "player", offlinePlayer.getName(),
                    "usernames", usernames
            ));
        } else {
            return plugin.getJoinDateMessages().get("joindate-other-offline", Map.of(
                    "joindate", joinDate,
                    "jointime", joinTime,
                    "lastlogindate", lastLoginDate,
                    "lastLoginTime", lastLoginTime,
                    "player", offlinePlayer.getName(),
                    "usernames", usernames
            ));

        }
    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() - (hours * 60);
        return String.format("%d hours, %d minutes", hours, minutes);
    }
}
