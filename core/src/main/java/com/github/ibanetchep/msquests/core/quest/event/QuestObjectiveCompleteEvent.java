package com.github.ibanetchep.msquests.core.quest.event;

import com.github.ibanetchep.msquests.core.quest.QuestObjective;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class QuestObjectiveCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final QuestObjective<?> objective;

    public QuestObjectiveCompleteEvent(QuestObjective<?> objective) {
        this.objective = objective;
    }

    public QuestObjective<?> getObjective() {
        return objective;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
