package com.github.ibanetchep.msquests.core.quest.event;

import com.github.ibanetchep.msquests.core.quest.Quest;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class QuestCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Quest quest;

    public QuestCompleteEvent(Quest quest) {
        this.quest = quest;
    }

    public Quest getQuest() {
        return quest;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
