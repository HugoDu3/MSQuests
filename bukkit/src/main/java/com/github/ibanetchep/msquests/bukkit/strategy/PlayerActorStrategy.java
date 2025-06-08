package com.github.ibanetchep.msquests.bukkit.strategy;

import com.github.ibanetchep.msquests.core.strategy.ActorStrategy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PlayerActorStrategy implements ActorStrategy {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final UUID playerId;

    public PlayerActorStrategy(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public void sendMessage(String message) {
        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) {
            Component c = MM.deserialize(message);
            p.sendMessage(c);
        }
    }
}
