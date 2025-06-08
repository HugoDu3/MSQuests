package com.github.ibanetchep.msquests.bukkit.pool;

import com.github.ibanetchep.msquests.bukkit.MSQuestsPlugin;
import com.github.ibanetchep.msquests.core.repository.ChainProgressRepository;
import com.github.ibanetchep.msquests.core.quest.pool.QuestChain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class QuestChainManager {

    private final MSQuestsPlugin plugin;
    private final ChainProgressRepository progressRepo;
    private final Map<String, QuestChain> chains = new LinkedHashMap<>();

    public QuestChainManager(MSQuestsPlugin plugin, ChainProgressRepository progressRepo) {
        this.plugin = plugin;
        this.progressRepo = progressRepo;
    }

    public void load() {
        chains.clear();
        chains.putAll(new QuestChainConfigLoader(plugin).load());
    }

    public Optional<String> nextInChain(UUID actorId, String chainId) {
        QuestChain chain = chains.get(chainId);
        if (chain == null) return Optional.empty();

        int idx = progressRepo
                .getCurrentIndex(actorId, chainId)
                .join()
                .orElse(0);

        List<QuestChain.ChainEntry> steps = chain.getSteps();
        if (idx >= steps.size()) return Optional.empty();

        String nextKey = steps.get(idx).questKey();
        progressRepo.updateCurrentIndex(actorId, chainId, idx + 1).join();
        return Optional.of(nextKey);
    }

    public Map<String, QuestChain> getChains() {
        return Collections.unmodifiableMap(chains);
    }
}
