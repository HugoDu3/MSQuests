package com.github.ibanetchep.msquests.bukkit.pool;

import com.github.ibanetchep.msquests.bukkit.MSQuestsPlugin;
import com.github.ibanetchep.msquests.core.quest.pool.QuestChain;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class QuestChainConfigLoader {

    private final MSQuestsPlugin plugin;
    private final Path chainsDir;

    public QuestChainConfigLoader(MSQuestsPlugin plugin) {
        this.plugin = plugin;
        this.chainsDir = plugin.getDataFolder().toPath().resolve("quests").resolve("chains");
    }

    public Map<String, QuestChain> load() {
        Map<String, QuestChain> chains = new LinkedHashMap<>();
        try {
            if (Files.notExists(chainsDir)) {
                Files.createDirectories(chainsDir);
            }
            try (Stream<Path> stream = Files.walk(chainsDir)) {
                stream
                        .filter(p -> p.getFileName().toString().endsWith(".yml"))
                        .forEach(path -> parseFile(path, chains));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to read quest chains");
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error walking quest chains directory", e);
        }
        return chains;
    }

    private void parseFile(Path path, Map<String, QuestChain> chains) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            QuestChain chain = new QuestChain(id, section.getString("display_name", id));
            List<Map<?, ?>> steps = section.getMapList("steps");
            for (Map<?, ?> raw : steps) {
                Object questObj = raw.get("quest_key");
                if (questObj == null) {
                    questObj = raw.get("quest");
                }
                if (questObj == null) {
                    continue;
                }
                String questKey = questObj.toString();
                int weight = raw.containsKey("weight") ? ((Number) raw.get("weight")).intValue() : 1;
                String cinematic = raw.containsKey("cinematic") ? raw.get("cinematic").toString() : null;
                String dialogue = raw.containsKey("dialogue") ? raw.get("dialogue").toString() : null;
                @SuppressWarnings("unchecked")
                List<String> rewards = raw.containsKey("rewards") ? (List<String>) raw.get("rewards") : null;
                long cooldownAfter = raw.containsKey("cooldown_after")
                        ? ((Number) raw.get("cooldown_after")).longValue()
                        : 0L;
                try {
                    chain.addStep(new QuestChain.ChainEntry(
                            questKey, weight, cinematic, dialogue, rewards, cooldownAfter
                    ));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning(
                            "Invalid step for quest " + questKey +
                                    " in chain " + id + " (" + path.getFileName() + ")"
                    );
                }
            }
            chains.put(chain.getId(), chain);
        }
    }
}
