package com.github.ibanetchep.msquests.bukkit.command;

import com.github.ibanetchep.msquests.bukkit.MSQuestsPlugin;
import com.github.ibanetchep.msquests.core.dto.QuestActorDTO;
import com.github.ibanetchep.msquests.core.dto.QuestDTO;
import com.github.ibanetchep.msquests.core.dto.QuestObjectiveDTO;
import com.github.ibanetchep.msquests.core.manager.QuestManager;
import com.github.ibanetchep.msquests.core.mapper.QuestEntryMapper;
import com.github.ibanetchep.msquests.core.quest.Quest;
import com.github.ibanetchep.msquests.core.quest.QuestConfig;
import com.github.ibanetchep.msquests.core.quest.QuestObjective;
import com.github.ibanetchep.msquests.core.quest.QuestObjectiveConfig;
import com.github.ibanetchep.msquests.core.quest.QuestObjectiveStatus;
import com.github.ibanetchep.msquests.core.quest.QuestStatus;
import com.github.ibanetchep.msquests.core.quest.event.QuestCompleteEvent;
import com.github.ibanetchep.msquests.database.DbAccess;
import com.github.ibanetchep.msquests.database.repository.ActorSqlRepository;
import com.github.ibanetchep.msquests.database.repository.PoolCooldownSqlRepository;
import com.github.ibanetchep.msquests.database.repository.QuestSqlRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.lang.reflect.Field;
import java.util.*;

@Command("msquests")
@CommandPermission("msquests.admin")
public final class QuestAdminCommand {

    private final MSQuestsPlugin plugin;
    private final QuestManager questManager;
    private final DbAccess db;

    public QuestAdminCommand(MSQuestsPlugin plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getQuestManager();
        this.db = extractDbAccess(plugin);
    }

    @Subcommand("reload")
    @Description("Reloads the quest configs and language files")
    public void reload(CommandSender sender) {
        plugin.getLangManager().load();
        questManager.loadQuestConfigs();
        sender.sendMessage("§aMSQuests reloaded");
    }

    @Subcommand("give")
    @Description("Give a quest to a player")
    public void give(CommandSender sender, Player player, String questKey) {
        QuestConfig cfg = findQuestConfig(questKey);
        if (cfg == null) {
            sender.sendMessage("§cUnknown quest: " + questKey);
            return;
        }

        ensureActorExists(player);
        QuestSqlRepository repo = new QuestSqlRepository(db);

        long now = System.currentTimeMillis();
        UUID questId = UUID.randomUUID();
        Map<UUID, QuestObjectiveDTO> objectives = new HashMap<>();
        for (QuestObjectiveConfig oc : cfg.getObjectives().values()) {
            UUID objId = UUID.randomUUID();
            objectives.put(objId, new QuestObjectiveDTO(
                    objId,
                    questId,
                    oc.getKey(),
                    0,
                    QuestObjectiveStatus.IN_PROGRESS,
                    0,
                    0,
                    now,
                    now
            ));
        }

        QuestDTO dto = new QuestDTO(
                questId,
                questKey,
                player.getUniqueId(),
                QuestStatus.IN_PROGRESS,
                now,
                0,
                cfg.getDuration() > 0 ? now + cfg.getDuration() * 1000 : 0,
                now,
                now,
                objectives
        );
        repo.save(dto).join();
        questManager.loadActor("player", player.getUniqueId());
        sender.sendMessage("§aQuest §e" + questKey + "§a given to §b" + player.getName());
    }

    @Subcommand("remove")
    @Description("Force-abandon a quest")
    public void remove(CommandSender sender, Player player, String questKey) {
        Optional<Quest> optQuest = locateQuest(player, questKey);
        if (optQuest.isEmpty()) {
            sender.sendMessage("§cQuest not found for that player");
            return;
        }

        Quest quest = optQuest.get();
        quest.setStatus(QuestStatus.FAILED);
        quest.setCompletedAt(new Date(System.currentTimeMillis()));

        QuestEntryMapper mapper = new QuestEntryMapper(plugin.getObjectiveTypeRegistry());
        new QuestSqlRepository(db).save(mapper.toDto(quest)).join();

        removeFromMemory(quest.getId());
        sender.sendMessage("§aQuest abandoned");
    }

    @Subcommand("finish")
    @Description("Instantly finish a quest")
    public void finish(CommandSender sender, Player player, String questKey) {
        Optional<Quest> optQuest = locateQuest(player, questKey);
        if (optQuest.isEmpty()) {
            sender.sendMessage("§cQuest not found for that player");
            return;
        }

        Quest quest = optQuest.get();
        long now = System.currentTimeMillis();
        quest.setStatus(QuestStatus.COMPLETED);
        quest.setCompletedAt(new Date(now));

        for (QuestObjective<?> o : quest.getObjectives().values()) {
            o.setProgress(o.getObjectiveConfig().getTargetAmount());
            o.setStatus(QuestObjectiveStatus.COMPLETED);
            o.setCompletedAt(new Date(now));
        }

        QuestEntryMapper mapper = new QuestEntryMapper(plugin.getObjectiveTypeRegistry());
        new QuestSqlRepository(db).save(mapper.toDto(quest)).join();

        Bukkit.getPluginManager().callEvent(new QuestCompleteEvent(quest));
        sender.sendMessage("§aQuest completed");
    }

    @Subcommand("handle_pool")
    @Description("Draw a quest from a pool for a player")
    public void handlePool(CommandSender sender, Player player, String poolId) {
        Optional<String> key = plugin.getPoolManager().drawRandomQuest(player.getUniqueId(), poolId);
        if (key.isEmpty()) {
            sender.sendMessage("§cNothing drawn (empty pool or cooldown)");
            return;
        }
        give(sender, player, key.get());
    }

    @Subcommand("pool reset")
    @Description("Reset a pool cooldown")
    public void poolReset(CommandSender sender, Player player, String poolId) {
        new PoolCooldownSqlRepository(db)
                .updateLastUse(player.getUniqueId(), poolId, 0L)
                .join();
        sender.sendMessage("§aCooldown reset");
    }

    private void ensureActorExists(Player player) {
        ActorSqlRepository repo = new ActorSqlRepository(db);
        repo.get(player.getUniqueId()).thenCompose(dto -> {
            return repo.add(Objects.requireNonNullElseGet(dto, () -> new QuestActorDTO("player", player.getUniqueId())));
        }).join();
    }

    @SuppressWarnings("unchecked")
    private QuestConfig findQuestConfig(String key) {
        try {
            Field f = QuestManager.class.getDeclaredField("questConfigs");
            f.setAccessible(true);
            Map<String, QuestConfig> map = (Map<String, QuestConfig>) f.get(questManager);
            return map.get(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Optional<Quest> locateQuest(Player player, String questKey) {
        for (Quest q : questManager.getLoadedQuests()) {
            if (q.getActor().isActor(player.getUniqueId()) && q.getQuest().getKey().equals(questKey)) {
                return Optional.of(q);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private void removeFromMemory(UUID questId) {
        try {
            Field f = QuestManager.class.getDeclaredField("quests");
            f.setAccessible(true);
            Map<UUID, Quest> map = (Map<UUID, Quest>) f.get(questManager);
            map.remove(questId);
        } catch (Exception ignored) {
        }
    }

    private DbAccess extractDbAccess(MSQuestsPlugin pl) {
        try {
            Field f = MSQuestsPlugin.class.getDeclaredField("dbAccess");
            f.setAccessible(true);
            return (DbAccess) f.get(pl);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to access DbAccess", e);
        }
    }
}
