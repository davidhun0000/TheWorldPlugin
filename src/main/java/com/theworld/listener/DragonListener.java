package com.theworld.listener;

import com.theworld.TheWorldPlugin;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DragonListener implements Listener {

    private final TheWorldPlugin plugin;

    public DragonListener(TheWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.ENDER_DRAGON) return;
        EnderDragon dragon = (EnderDragon) event.getEntity();
        Player killer = dragon.getKiller();
        if (killer == null) return;

        if (plugin.getItemManager().hasHolder()) {
            String msg = colorize(plugin.getConfig().getString("messages.dragon-already-claimed",
                "&cThe World item has already been claimed by another warrior. Defeat them to take it!"));
            killer.sendMessage(msg);
            return;
        }

        plugin.getItemManager().setDragonDefeated(true);
        plugin.getItemManager().giveItemToPlayer(killer);

        String msg = colorize(plugin.getConfig().getString("messages.dragon-first-kill",
            "&6&lYou have defeated the Ender Dragon and claimed &e&lThe World&6&l!"));
        killer.sendMessage(msg);
        org.bukkit.Bukkit.broadcastMessage(colorize("&6&l" + killer.getName() + " &ehas defeated the Ender Dragon and claimed &6&lThe World&e!"));
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}