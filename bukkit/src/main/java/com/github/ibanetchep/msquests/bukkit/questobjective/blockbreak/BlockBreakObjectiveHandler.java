package com.github.ibanetchep.msquests.bukkit.questobjective.blockbreak;

import com.github.ibanetchep.msquests.bukkit.MSQuestsPlugin;
import com.github.ibanetchep.msquests.core.quest.QuestObjectiveHandler;
import com.github.ibanetchep.msquests.core.quest.QuestObjectiveStatus;
import com.github.ibanetchep.msquests.core.quest.actor.QuestActor;
import com.github.ibanetchep.msquests.database.repository.ObjectiveSqlRepository;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakObjectiveHandler extends QuestObjectiveHandler<BlockBreakObjective> implements Listener {

    public BlockBreakObjectiveHandler(MSQuestsPlugin plugin) {
        super(plugin.getQuestManager(), new ObjectiveSqlRepository(plugin.getDbAccess()), BlockBreakObjective.class);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        for (BlockBreakObjective objective : getQuestObjectives()) {
            QuestActor actor = objective.getQuest().getActor();
            if (!actor.isActor(player.getUniqueId())) continue;

            Material material = event.getBlock().getType();
            if (material == objective.getObjectiveConfig().getBlockType() && objective.getStatus() != QuestObjectiveStatus.COMPLETED) {
                updateProgress(objective, 1);
            }
        }
    }
}
