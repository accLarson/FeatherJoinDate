package dev.zerek.featherjoindate.commands;

import dev.zerek.featherjoindate.FeatherJoinDate;
import dev.zerek.featherjoindate.utils.MessageFormatterUtility;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SeenCommand implements CommandExecutor {

    private final FeatherJoinDate plugin;

    public SeenCommand(FeatherJoinDate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        // Check permission
        if (!sender.hasPermission("feather.joindate.seen")) {
            sender.sendMessage(plugin.getJoinDateMessages().get("error-no-permission"));
            return true;
        }

        // Validate argument count
        if (args.length > 1) { 
            sender.sendMessage(plugin.getJoinDateMessages().get("error-arg-count"));
            return true;
        }

        // Determine target player
        String targetName;
        if (args.length == 0) {
            // No arguments - use sender's name
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getJoinDateMessages().get("error-not-player"));
                return true;
            }
            targetName = sender.getName();
        } else {
            // Use provided player name
            targetName = args[0];
        }

        // Handle the command logic asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> processSeenCommand(sender, targetName));
        return true;
    }

    /**
     * Process the seen command for the given target player name
     */
    private void processSeenCommand(CommandSender sender, String targetName) {
        // First check if target is the sender
        if (sender instanceof Player && sender.getName().equalsIgnoreCase(targetName)) {
            // Directly use the sender's player object
            Player player = (Player) sender;
            displayPlayerStats(sender, player);
            return;
        }

        // Check if there's an online player with this name
        Player onlinePlayer = Bukkit.getPlayerExact(targetName);
        if (onlinePlayer != null) {
            displayPlayerStats(sender, onlinePlayer);
            return;
        }

        // Get all UUIDs that have used this username
        List<String> uuids = plugin.getJoinManager().getUsernameUUIDs(targetName);

        if (uuids.isEmpty()) {
            // No player with this username has ever joined
            Map<String, String> params = new HashMap<>();
            params.put("player", targetName);
            sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player", params));
            return;
        }

        if (uuids.size() > 1) {
            // Multiple players have used this username - check if any currently has it
            OfflinePlayer currentHolder = null;
            Set<String> currentUsernames = new HashSet<>(); 

            for (String uuid : uuids) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                String currentName = offlinePlayer.getName();

                if (currentName != null) {
                    // Check if this player currently has the searched username
                    if (currentName.equalsIgnoreCase(targetName)) {
                        currentHolder = offlinePlayer;
                        break;
                    }
                    currentUsernames.add(offlinePlayer.getName());
                } else {
                    // Name is null, try to get it from MineTools API directly
                    // We're already in an async task, so we can call this directly
                    String apiUsername = plugin.getMineToolsAPIUtility()
                            .getCurrentUsername(offlinePlayer.getUniqueId())
                            .join(); // This blocks until the API call completes
                            
                    if (apiUsername != null) {
                        // Check if this is the current holder of the name
                        if (apiUsername.equalsIgnoreCase(targetName)) {
                            currentHolder = offlinePlayer;
                            break;
                        }
                        currentUsernames.add(apiUsername);
                    } else {
                        // API lookup failed, fall back to database
                        String databaseName = plugin.getJoinManager()
                                .getMostRecentUsernameForUUID(offlinePlayer.getUniqueId().toString());
                        if (databaseName != null) {
                            // Check if this is the current holder of the name
                            if (databaseName.equalsIgnoreCase(targetName)) {
                                currentHolder = offlinePlayer;
                                break;
                            }
                            currentUsernames.add(databaseName);
                        }
                    }
                }
            }

            // If we found a current holder of the username, use that player
            if (currentHolder != null) {
                // Check if the current holder has a null name
                if (currentHolder.getName() == null) {
                    // Use MineToolsAPI to get the current username
                    String apiUsername = plugin.getMineToolsAPIUtility()
                        .getCurrentUsername(currentHolder.getUniqueId())
                        .join(); // This blocks until the API call completes

                    // Use the target name if API lookup fails
                    String nameToUse = apiUsername != null ? apiUsername : targetName;
                    displayPlayerStatsWithCustomName(sender, currentHolder, nameToUse);
                } else {
                    displayPlayerStats(sender, currentHolder);
                }
            } else {
                // No current holder, show the ambiguity message with all the usernames we found
                if (currentUsernames.isEmpty()) {
                    // If we couldn't find any current usernames, try one more fallback
                    for (String uuid : uuids) {
                        String databaseName = plugin.getJoinManager()
                                .getMostRecentUsernameForUUID(uuid);
                        if (databaseName != null) {
                            currentUsernames.add(databaseName);
                        }
                    }
                }
                
                // Now send the message with all usernames we found
                if (!currentUsernames.isEmpty()) {
                    // Send message on the main thread
                    final List<String> finalUsernames = new ArrayList<>(new HashSet<>(currentUsernames));
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Map<String, String> params = new HashMap<>();
                        params.put("usernames", String.join(", ", finalUsernames));
                        sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player-multiple", params));
                    });
                } else {
                    // We couldn't find any usernames at all - this is an edge case
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Map<String, String> params = new HashMap<>();
                        params.put("usernames", "Unknown players");
                        sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player-multiple", params));
                    });
                }
            }
            return;
        }

        // Single UUID found - get the offline player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuids.get(0)));

        // If the OfflinePlayer has a null name, try to get the name from MineTools API
        if (offlinePlayer.getName() == null) {
            // Start async lookup
            CompletableFuture<String> futureUsername = plugin.getMineToolsAPIUtility() 
                .getCurrentUsername(offlinePlayer.getUniqueId());

            // Handle the result when it's ready
            futureUsername.thenAccept(apiUsername -> {
                if (apiUsername != null) {
                    if (!apiUsername.equalsIgnoreCase(targetName)) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(plugin.getJoinDateMessages().get("warning-old-username",
                                    new HashMap<>() {{
                                        put("player", targetName);
                                        put("currentusername", apiUsername);
                                    }}));
                        });
                    }
                    displayPlayerStatsWithCustomName(sender, offlinePlayer, apiUsername);
                } else {
                    // API lookup failed, fall back to database lookup
                    String databaseName = plugin.getJoinManager().getMostRecentUsernameForUUID(offlinePlayer.getUniqueId().toString());
                    if (!databaseName.equalsIgnoreCase(targetName)) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(plugin.getJoinDateMessages().get("warning-old-username",
                                    new HashMap<>() {{
                                        put("player", targetName);
                                        put("currentusername", databaseName);
                                    }}));
                        });
                    }
                    displayPlayerStatsWithCustomName(sender, offlinePlayer, databaseName);
                }
            });
            return;
        }
        if (!offlinePlayer.getName().equalsIgnoreCase(targetName)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getJoinDateMessages().get("warning-old-username",
                        new HashMap<>() {{
                            put("player", targetName);
                            put("currentusername", offlinePlayer.getName());
                        }}));
            });
        }
        displayPlayerStats(sender, offlinePlayer);
    }

    /**
     * Display the join statistics for a player
     */
    private void displayPlayerStats(CommandSender sender, OfflinePlayer offlinePlayer) {
        // Run the database query asynchronously (we're already in an async task)
        // But send the message on the main thread
        displayPlayerStatsAsync(sender, offlinePlayer);
    }

    private void displayPlayerStatsAsync(CommandSender sender, OfflinePlayer offlinePlayer) {
        // Get player data from database
        Map<String, Object> playerData = plugin.getJoinManager().getPlayerFullData(offlinePlayer);

        // Check if player exists in database
        if (!(boolean) playerData.get("exists")) {
            Map<String, String> params = new HashMap<>();
            params.put("player", offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown");
            // Send message on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player", params));
            });
            return;
        }
        
        // Format and send the message
        TextComponent message = MessageFormatterUtility.formatJoinDateMessage(
                plugin,
                offlinePlayer,
                (long) playerData.get("joinDate"),
                (long) playerData.get("lastLogin"),
                (List<String>) playerData.get("previousUsernames"));
        
        // Send message on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage(message);
        });
    }
    
    /**
     * Display the join statistics for a player using a custom name
     * This is used when the OfflinePlayer has a null name but we found the name elsewhere
     */
    private void displayPlayerStatsWithCustomName(CommandSender sender, OfflinePlayer offlinePlayer, String customName) {
        // Get player data from database
        Map<String, Object> playerData = plugin.getJoinManager().getPlayerFullData(offlinePlayer);

        // Check if player exists in database
        if (!(boolean) playerData.get("exists")) {
            Map<String, String> params = new HashMap<>();
            params.put("player", customName != null ? customName : "Unknown");
            // Send message on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getJoinDateMessages().get("error-unseen-player", params));
            });
            return;
        }
        
        // Create custom parameters with the custom name
        Map<String, String> customParams = new HashMap<>();
        customParams.put("player", customName);
        
        // Format and send the message with custom parameters
        TextComponent message = MessageFormatterUtility.formatJoinDateMessage(
                plugin, offlinePlayer, (long) playerData.get("joinDate"), (long) playerData.get("lastLogin"),
                (List<String>) playerData.get("previousUsernames"), customParams);
        
        // Send message on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            sender.sendMessage(message);
        });
    }
}
