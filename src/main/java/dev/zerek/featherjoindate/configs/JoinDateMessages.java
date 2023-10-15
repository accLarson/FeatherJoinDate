package dev.zerek.featherjoindate.configs;

import dev.zerek.featherjoindate.FeatherJoinDate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JoinDateMessages {

    private final FeatherJoinDate plugin;
    private final Map<String,String> messages;
    private final MiniMessage mm = MiniMessage.builder().tags(
            TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.reset())
                    .build()
    ).build();

    public JoinDateMessages(FeatherJoinDate plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.init();
    }

    private void init() {
        File file = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) this.plugin.saveResource("messages.yml", false);
        FileConfiguration messagesConfig = YamlConfiguration.loadConfiguration(file);
        messagesConfig.getKeys(false).forEach(key -> messages.put(key,messagesConfig.getString(key)));
    }

    public TextComponent get(String key){
        if (messages.containsKey(key)) return (TextComponent) mm.deserialize(messages.get(key));
        else return Component.text("");
    }

    public TextComponent get(String key, Map<String, String> placeholders) {
        if (messages.containsKey(key)) {

            List<TagResolver> rs = placeholders
                    .entrySet()
                    .stream()
                    .map(entry -> (TagResolver) Placeholder.parsed(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            return (TextComponent) mm.deserialize(messages.get(key), TagResolver.resolver(rs));

        } else return Component.text("");
    }

}
