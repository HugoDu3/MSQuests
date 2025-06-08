package com.github.ibanetchep.msquests.core.quest.pool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class QuestChain {

    private final String id;
    private String displayName;
    private final List<ChainEntry> steps = new ArrayList<>();

    public QuestChain(String id) {
        this(id, id);
    }

    public QuestChain(String id, String displayName) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
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

    public List<ChainEntry> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public void addStep(ChainEntry entry) {
        Objects.requireNonNull(entry, "entry");
        steps.add(entry);
    }

    @Override
    public String toString() {
        return "QuestChain{id='%s', steps=%d}".formatted(id, steps.size());
    }

    public record ChainEntry(
            String questKey,
            int weight,
            String cinematic,
            String dialogue,
            List<String> rewardsOverride,
            long cooldownAfter
    ) {
        public ChainEntry {
            Objects.requireNonNull(questKey, "questKey");
            if (weight <= 0) {
                throw new IllegalArgumentException("weight must be > 0");
            }
            if (cooldownAfter < 0) {
                throw new IllegalArgumentException("cooldownAfter cannot be negative");
            }
        }
    }
}
