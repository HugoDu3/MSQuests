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
import com.github.ibanetchep.msquests.database.DbAccess;
import com.github.ibanetchep.msquests.database.repository.ActorSqlRepository;
import com.github.ibanetchep.msquests.database.repository.PoolCooldownSqlRepository;
import com.github.ibanetchep.msquests.database.repository.QuestSqlRepository;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.lang.reflect.Field;
import java.util.*;

@Command("quests")
@CommandPermission("msquests.player")
public final class PlayerQuestCommand {

    private final MSQuestsPlugin plugin;
    private final QuestManager questManager;
    private final DbAccess db;
    private static final Map<UUID, UUID> tracked = new HashMap<>();

    public PlayerQuestCommand(MSQuestsPlugin plugin) {
        this.plugin = plugin;
        this.questManager = plugin.getQuestManager();
        this.db = extractDbAccess(plugin);
    }

    @Subcommand("")
    @Description("Show available pools and chains")
    public void listDefault(CommandSender sender) {
        if (sender instanceof Player p) list(p);
        else sender.sendMessage("Only players can use this command.");
    }

    @Subcommand("list")
    @Description("Show available pools and chains")
    public void list(Player p) {
        p.sendMessage("§eAvailable Quest Pools:");
        plugin.getPoolManager().getPools().values()
                .forEach(pool -> p.sendMessage(" §7- §a" + pool.getId() + " §f: " + pool.getDisplayName()));
        p.sendMessage("§eAvailable Quest Chains:");
        plugin.getChainManager().getChains().values()
                .forEach(chain -> p.sendMessage(" §7- §a" + chain.getId() + " §f: " + chain.getDisplayName()));
    }

    @Subcommand("pools")
    @Description("Show pool cooldowns")
    public void pools(Player p) {
        PoolCooldownSqlRepository repo = new PoolCooldownSqlRepository(db);
        long now = System.currentTimeMillis();
        plugin.getPoolManager().getPools().values().forEach(pool -> {
            long last = repo.getLastUse(p.getUniqueId(), pool.getId()).join().orElse(0L);
            long remaining = pool.getCooldown() == 0 ? 0 : pool.getCooldown() * 1000 - (now - last);
            remaining = Math.max(0, remaining);
            String status = remaining == 0 ? "§aREADY" : "§c" + remaining / 1000 + "s";
            p.sendMessage(" §7- §e" + pool.getId() + " §8» " + status);
        });
    }

    @Subcommand("accept")
    @Description("Accept a quest")
    public void accept(Player p, String questKey) {
        QuestConfig cfg = findQuestConfig(questKey);
        if (cfg == null) {
            p.sendMessage("§cThis quest does not exist.");
            return;
        }
        if (locateQuest(p, questKey).isPresent()) {
            p.sendMessage("§cYou already have this quest.");
            return;
        }
        ensureActorExists(p);
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
                p.getUniqueId(),
                QuestStatus.IN_PROGRESS,
                now,
                0,
                cfg.getDuration() > 0 ? now + cfg.getDuration() * 1000 : 0,
                now,
                now,
                objectives
        );
        new QuestSqlRepository(db).save(dto).join();
        questManager.loadActor("player", p.getUniqueId());
        p.sendMessage("§aQuest accepted!");
    }

    @Subcommand("abandon")
    @Description("Abandon a quest")
    public void abandon(Player p, String questKey) {
        Optional<Quest> opt = locateQuest(p, questKey);
        if (opt.isEmpty()) {
            p.sendMessage("§cThis quest is not active.");
            return;
        }
        Quest quest = opt.get();
        quest.setStatus(QuestStatus.FAILED);
        quest.setCompletedAt(new Date(System.currentTimeMillis()));
        QuestEntryMapper mapper = new QuestEntryMapper(plugin.getObjectiveTypeRegistry());
        new QuestSqlRepository(db).save(mapper.toDto(quest)).join();
        p.sendMessage("§eQuest abandoned.");
    }

    @Subcommand("progress")
    @Description("Show progress of current quests")
    public void progress(Player p) {
        boolean found = false;
        for (Quest q : questManager.getLoadedQuests()) {
            if (!q.getActor().isActor(p.getUniqueId())) continue;
            found = true;
            p.sendMessage("§6" + q.getQuest().getKey() + " §7(" + q.getStatus() + ")");
            for (QuestObjective<?> o : q.getObjectives().values()) {
                int prog = o.getProgress();
                int tgt = o.getObjectiveConfig().getTargetAmount();
                p.sendMessage("  §8• §f" + o.getObjectiveConfig().getKey() + ": §a" + prog + "/" + tgt);
            }
        }
        if (!found) p.sendMessage("§cYou have no active quests.");
    }

    @Subcommand("track")
    @Description("Track a quest")
    public void track(Player p, String questKey) {
        Optional<Quest> opt = locateQuest(p, questKey);
        if (opt.isEmpty()) {
            p.sendMessage("§cQuest not active.");
            return;
        }
        tracked.put(p.getUniqueId(), opt.get().getId());
        p.sendMessage("§aTracking §e" + questKey);
    }

    @Subcommand("untrack")
    @Description("Stop tracking any quest")
    public void untrack(Player p) {
        tracked.remove(p.getUniqueId());
        p.sendMessage("§eTracking cleared.");
    }

    @Subcommand("handle_pool")
    @Description("Draw a quest from a pool you have access to")
    public void handlePool(Player p, String poolId) {
        Optional<String> questKey = plugin.getPoolManager().drawRandomQuest(p.getUniqueId(), poolId);
        if (questKey.isEmpty()) {
            p.sendMessage("§cPool is empty or on cooldown.");
            return;
        }
        accept(p, questKey.get());
    }

    private void ensureActorExists(Player p) {
        ActorSqlRepository repo = new ActorSqlRepository(db);
        repo.get(p.getUniqueId()).thenCompose(dto -> {
            if (dto == null) {
                return repo.add(new QuestActorDTO("player", p.getUniqueId()));
            }
            return repo.add(dto);
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

    private Optional<Quest> locateQuest(Player p, String key) {
        for (Quest q : questManager.getLoadedQuests()) {
            if (q.getActor().isActor(p.getUniqueId()) && q.getQuest().getKey().equals(key)) {
                return Optional.of(q);
            }
        }
        return Optional.empty();
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
