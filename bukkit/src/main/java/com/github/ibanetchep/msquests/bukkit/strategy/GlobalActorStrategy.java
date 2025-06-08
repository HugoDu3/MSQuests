package com.github.ibanetchep.msquests.bukkit.strategy;

import com.github.ibanetchep.msquests.core.strategy.ActorStrategy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class GlobalActorStrategy implements ActorStrategy {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public void sendMessage(String message) {
        Component c = MM.deserialize(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(c);
        }
        Bukkit.getConsoleSender().sendMessage(c);
    }
}
