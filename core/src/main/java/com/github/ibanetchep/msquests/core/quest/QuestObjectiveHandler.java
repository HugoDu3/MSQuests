package com.github.ibanetchep.msquests.core.quest;

import com.github.ibanetchep.msquests.core.dto.QuestObjectiveDTO;
import com.github.ibanetchep.msquests.core.manager.QuestManager;
import com.github.ibanetchep.msquests.core.quest.event.QuestObjectiveCompleteEvent;
import com.github.ibanetchep.msquests.core.quest.event.QuestObjectiveProgressEvent;
import com.github.ibanetchep.msquests.core.repository.ObjectiveRepository;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class QuestObjectiveHandler<T extends QuestObjective<?>> {

    private final QuestManager questManager;
    private final ObjectiveRepository objectiveRepository;
    private final Class<T> objectiveClass;

    protected QuestObjectiveHandler(QuestManager questManager, ObjectiveRepository objectiveRepository, Class<T> objectiveClass) {
        this.questManager = questManager;
        this.objectiveRepository = objectiveRepository;
        this.objectiveClass = objectiveClass;
    }

    public List<T> getQuestObjectives() {
        List<T> list = new ArrayList<>();
        for (Quest quest : questManager.getLoadedQuests()) {
            if (quest.getObjectives() == null) continue;
            quest.getObjectives().values().stream()
                    .filter(o -> objectiveClass.isInstance(o) && !o.isCompleted())
                    .forEach(o -> list.add(objectiveClass.cast(o)));
        }
        return list;
    }

    protected void updateProgress(T objective, int amount) {
        if (objective.getStatus() == QuestObjectiveStatus.FAILED || objective.isCompleted()) return;

        if (objective.getStartedAt() == null) objective.setStartedAt(new Date());

        int target = objective.getObjectiveConfig().getTargetAmount();
        int newProgress = Math.min(objective.getProgress() + amount, target);
        objective.setProgress(newProgress);
        objective.setUpdatedAt(new Date());

        if (newProgress >= target) {
            objective.setStatus(QuestObjectiveStatus.COMPLETED);
            objective.setCompletedAt(new Date());
            objective.callOnComplete();
            Bukkit.getPluginManager().callEvent(new QuestObjectiveCompleteEvent(objective));
        } else {
            objective.callOnProgress();
            Bukkit.getPluginManager().callEvent(new QuestObjectiveProgressEvent(objective));
        }

        QuestObjectiveDTO dto = new QuestObjectiveDTO(
                objective.getId(),
                objective.getQuest().getId(),
                objective.getObjectiveConfig().getKey(),
                objective.getProgress(),
                objective.getStatus(),
                objective.getStartedAt() != null ? objective.getStartedAt().getTime() : 0L,
                objective.getCompletedAt() != null ? objective.getCompletedAt().getTime() : 0L,
                objective.getCreatedAt() != null ? objective.getCreatedAt().getTime() : System.currentTimeMillis(),
                objective.getUpdatedAt() != null ? objective.getUpdatedAt().getTime() : System.currentTimeMillis()
        );
        objectiveRepository.save(dto);
    }
}
