package dev.zerek.featherjoindate.commands;

import dev.zerek.featherjoindate.FeatherJoinDate;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class JoinDateCommand implements CommandExecutor {

    private final FeatherJoinDate plugin;

    public JoinDateCommand(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            //add arg count logic... if 0 args, self. if 1 arg + permission, others.
            DateTimeFormatter formatter  = DateTimeFormatter.ofPattern("EEEE, MMM dd, uuuu").withZone(ZoneId.of("America/Toronto"));
            String joinDate = formatter.format(Instant.ofEpochMilli(plugin.getJoinManager().getJoinDate((OfflinePlayer) sender)));
            sender.sendMessage(plugin.getJoinDateMessages().get("joindate-self", Map.of("joindate", joinDate)));
        }
        return true;
    }
}
