package com.github.ibanetchep.msquests.bukkit.pool;

import com.github.ibanetchep.msquests.bukkit.MSQuestsPlugin;
import com.github.ibanetchep.msquests.core.repository.PoolCooldownRepository;
import com.github.ibanetchep.msquests.core.quest.pool.QuestPool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class QuestPoolManager {

    private final MSQuestsPlugin plugin;
    private final PoolCooldownRepository cooldownRepo;
    private final Map<String, QuestPool> pools = new LinkedHashMap<>();

    public QuestPoolManager(MSQuestsPlugin plugin, PoolCooldownRepository cooldownRepo) {
        this.plugin = plugin;
        this.cooldownRepo = cooldownRepo;
    }

    public void load() {
        pools.clear();
        pools.putAll(new QuestPoolConfigLoader(plugin).load());
    }

    public Optional<String> drawRandomQuest(UUID actorId, String poolId) {
        QuestPool pool = pools.get(poolId);
        if (pool == null) return Optional.empty();

        long lastUse = cooldownRepo
                .getLastUse(actorId, poolId)
                .join()
                .orElse(0L);

        long now = System.currentTimeMillis();
        if (now - lastUse < pool.getCooldown() * 1000) return Optional.empty();

        int totalWeight = pool.getEntries().values().stream()
                .mapToInt(QuestPool.PoolEntry::weight)
                .sum();
        int pick = ThreadLocalRandom.current().nextInt(totalWeight) + 1;

        String chosen = null;
        int cumsum = 0;
        for (QuestPool.PoolEntry e : pool.getEntries().values()) {
            cumsum += e.weight();
            if (pick <= cumsum) {
                chosen = e.questKey();
                break;
            }
        }
        if (chosen == null) return Optional.empty();

        cooldownRepo.updateLastUse(actorId, poolId, now).join();
        return Optional.of(chosen);
    }

    public Map<String, QuestPool> getPools() {
        return Collections.unmodifiableMap(pools);
    }
}
