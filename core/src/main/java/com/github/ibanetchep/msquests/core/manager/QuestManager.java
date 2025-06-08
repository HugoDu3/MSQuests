package com.github.ibanetchep.msquests.core.manager;

import com.github.ibanetchep.msquests.bukkit.strategy.GlobalActorStrategy;
import com.github.ibanetchep.msquests.bukkit.strategy.PlayerActorStrategy;
import com.github.ibanetchep.msquests.core.dto.QuestActorDTO;
import com.github.ibanetchep.msquests.core.dto.QuestConfigDTO;
import com.github.ibanetchep.msquests.core.dto.QuestDTO;
import com.github.ibanetchep.msquests.core.mapper.QuestConfigMapper;
import com.github.ibanetchep.msquests.core.mapper.QuestEntryMapper;
import com.github.ibanetchep.msquests.core.quest.Quest;
import com.github.ibanetchep.msquests.core.quest.QuestConfig;
import com.github.ibanetchep.msquests.core.quest.actor.QuestActor;
import com.github.ibanetchep.msquests.core.quest.actor.QuestGlobalActor;
import com.github.ibanetchep.msquests.core.quest.actor.QuestPlayerActor;
import com.github.ibanetchep.msquests.core.registry.ActorTypeRegistry;
import com.github.ibanetchep.msquests.core.registry.ObjectiveTypeRegistry;
import com.github.ibanetchep.msquests.core.repository.ActorRepository;
import com.github.ibanetchep.msquests.core.repository.QuestConfigRepository;
import com.github.ibanetchep.msquests.core.repository.QuestRepository;
import com.github.ibanetchep.msquests.core.strategy.ActorStrategy;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class QuestManager {

    private final Logger logger;
    private final Map<String, QuestConfig> questConfigs = new ConcurrentHashMap<>();
    private final Map<UUID, QuestActor> actors = new ConcurrentHashMap<>();
    private final Map<UUID, Quest> quests = new ConcurrentHashMap<>();
    private final Map<UUID, ActorStrategy> actorStrategies = new ConcurrentHashMap<>();

    private final QuestConfigRepository questConfigRepository;
    private final ActorRepository actorRepository;
    private final QuestRepository questRepository;
    private final QuestConfigMapper questConfigMapper;
    private final QuestEntryMapper questEntryMapper;
    private final ActorTypeRegistry actorTypeRegistry;

    public QuestManager(
            Logger logger,
            QuestConfigRepository questConfigRepository,
            ActorRepository actorRepository,
            QuestRepository questRepository,
            QuestConfigMapper questConfigMapper,
            QuestEntryMapper questEntryMapper,
            ActorTypeRegistry actorTypeRegistry,
            ObjectiveTypeRegistry objectiveTypeRegistry
    ) {
        this.logger = logger;
        this.questConfigRepository = questConfigRepository;
        this.actorRepository = actorRepository;
        this.questRepository = questRepository;
        this.questConfigMapper = questConfigMapper;
        this.questEntryMapper = questEntryMapper;
        this.actorTypeRegistry = actorTypeRegistry;
        loadQuestConfigs();
    }

    public void loadQuestConfigs() {
        questConfigs.clear();
        questConfigRepository.getAll().thenAccept(map -> {
            map.values().forEach(dto -> questConfigs.put(dto.key(), questConfigMapper.toEntity(dto)));
            logger.info("Loaded " + questConfigs.size() + " quest configs");
        });
    }

    public void loadActor(String type, UUID uuid) {
        actorRepository.get(uuid).thenAccept(dto -> {
            if (dto == null) {
                dto = new QuestActorDTO(type, uuid);
                actorRepository.add(dto);
            }
            QuestActor actor = actorTypeRegistry.createActor(dto);
            if (actor == null) return;
            actors.put(actor.getId(), actor);

            if (actor instanceof QuestPlayerActor) {
                actorStrategies.put(actor.getId(), new PlayerActorStrategy(uuid));
            } else if (actor instanceof QuestGlobalActor) {
                actorStrategies.put(actor.getId(), new GlobalActorStrategy());
            }
            loadQuests(actor);
        });
    }

    private void loadQuests(QuestActor actor) {
        questRepository.getAllByActor(actor.getId()).thenAccept(map -> {
            map.values().forEach(dto -> {
                QuestConfig cfg = questConfigs.get(dto.questKey());
                if (cfg != null) {
                    quests.put(dto.id(), questEntryMapper.toEntity(dto, actor, cfg));
                }
            });
        });
    }

    public ActorStrategy getActorStrategy(UUID actorId) {
        return actorStrategies.get(actorId);
    }

    public void saveQuestConfig(QuestConfig cfg) {
        questConfigs.put(cfg.getKey(), cfg);
        questConfigRepository.upsert(questConfigMapper.toDto(cfg));
    }

    public Iterable<Quest> getLoadedQuests() {
        return quests.values();
    }

    public Map<String, QuestConfig> getQuestConfigs() {
        return Collections.unmodifiableMap(questConfigs);
    }

    public void removeQuest(UUID questId) {
        quests.remove(questId);
    }
}
