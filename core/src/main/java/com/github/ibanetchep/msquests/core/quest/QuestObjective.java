package com.github.ibanetchep.msquests.core.quest;

import java.util.Date;
import java.util.UUID;

public abstract class QuestObjective<T extends QuestObjectiveConfig> {

    protected T objectiveConfig;
    protected UUID id;
    protected int progress;
    protected QuestObjectiveStatus status = QuestObjectiveStatus.IN_PROGRESS;
    protected Quest quest;
    protected Date startedAt;
    protected Date completedAt;
    protected Date createdAt = new Date();
    protected Date updatedAt = new Date();

    public QuestObjective(UUID id, Quest quest, int progress, T objectiveDefinition) {
        this.id = id;
        this.quest = quest;
        this.progress = progress;
        this.objectiveConfig = objectiveDefinition;
    }

    public boolean isCompleted() {
        return progress >= objectiveConfig.getTargetAmount();
    }

    public UUID getId() {
        return id;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Quest getQuest() {
        return quest;
    }

    public QuestObjectiveStatus getStatus() {
        return status;
    }

    public void setStatus(QuestObjectiveStatus status) {
        this.status = status;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public T getObjectiveConfig() {
        return objectiveConfig;
    }

    public void callOnProgress() {}
    public void callOnComplete() {}
}
