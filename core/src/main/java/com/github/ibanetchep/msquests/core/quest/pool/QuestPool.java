package com.github.ibanetchep.msquests.core.quest.pool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class QuestPool {

    private final String id;
    private String displayName;
    private long cooldown;
    private final Map<String, PoolEntry> entries = new LinkedHashMap<>();

    public QuestPool(String id) {
        this(id, id, 0L);
    }

    public QuestPool(String id, String displayName, long cooldown) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.cooldown = cooldown;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    public long getCooldown() {
        return cooldown;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public Map<String, PoolEntry> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public void addEntry(PoolEntry entry) {
        Objects.requireNonNull(entry, "entry");
        entries.put(entry.questKey(), entry);
    }

    public void removeEntry(String questKey) {
        entries.remove(questKey);
    }

    @Override
    public String toString() {
        return "QuestPool{id='%s', entries=%d}".formatted(id, entries.size());
    }

    public record PoolEntry(
            String questKey,
            int weight,
            String cinematic,
            String dialogue,
            List<String> rewardsOverride
    ) {
        public PoolEntry {
            Objects.requireNonNull(questKey, "questKey");
            if (weight <= 0) {
                throw new IllegalArgumentException("weight must be > 0");
            }
        }
    }
}
