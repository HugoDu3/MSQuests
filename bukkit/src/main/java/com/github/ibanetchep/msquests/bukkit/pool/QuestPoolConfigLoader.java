package com.github.ibanetchep.msquests.bukkit.pool;

import com.github.ibanetchep.msquests.bukkit.MSQuestsPlugin;
import com.github.ibanetchep.msquests.core.quest.pool.QuestPool;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class QuestPoolConfigLoader {

    private final MSQuestsPlugin plugin;
    private final Path poolsDir;

    public QuestPoolConfigLoader(MSQuestsPlugin plugin) {
        this.plugin = plugin;
        this.poolsDir = plugin.getDataFolder().toPath().resolve("quests").resolve("pools");
    }

    public Map<String, QuestPool> load() {
        Map<String, QuestPool> pools = new LinkedHashMap<>();
        try {
            if (Files.notExists(poolsDir)) {
                Files.createDirectories(poolsDir);
            }
            try (Stream<Path> stream = Files.walk(poolsDir)) {
                stream
                        .filter(p -> p.getFileName().toString().endsWith(".yml"))
                        .forEach(path -> parseFile(path, pools));
            }
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unable to read quest pools", e);
        }
        return pools;
    }


    private void parseFile(Path path, Map<String, QuestPool> pools) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            QuestPool pool = new QuestPool(
                    id,
                    section.getString("display_name", id),
                    section.getLong("cooldown", 0L)
            );
            ConfigurationSection entries = section.getConfigurationSection("entries");
            if (entries != null) {
                for (String questKey : entries.getKeys(false)) {
                    ConfigurationSection e = entries.getConfigurationSection(questKey);
                    if (e == null) {
                        continue;
                    }
                    int weight = e.getInt("weight", 1);
                    String cinematic = e.getString("cinematic");
                    String dialogue = e.getString("dialogue");
                    List<String> rewards = e.isList("rewards") ? e.getStringList("rewards") : null;
                    try {
                        pool.addEntry(new QuestPool.PoolEntry(questKey, weight, cinematic, dialogue, rewards));
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Invalid entry " + questKey + " in pool " + id + " (" + path.getFileName() + ")");
                    }
                }
            }
            pools.put(pool.getId(), pool);
        }
    }
}
