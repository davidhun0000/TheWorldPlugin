package com.theworld.listener;

import com.theworld.TheWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PlayerListener implements Listener {

    private final TheWorldPlugin plugin;

    public PlayerListener(TheWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!plugin.getItemManager().isHolder(victim)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        if (attacker == null) return;
        plugin.getCombatManager().enterCombat(victim, attacker);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!plugin.getItemManager().isHolder(victim)) return;

        Player killer = victim.getKiller();

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        ItemStack theWorldItem = null;
        for (ItemStack drop : drops) {
            if (plugin.getItemManager().isTheWorldItem(drop)) {
                theWorldItem = drop;
                break;
            }
        }
        if (theWorldItem != null) {
            event.getDrops().remove(theWorldItem);
        }

        plugin.getCombatManager().exitCombat(victim.getUniqueId());
        plugin.getItemManager().setHolder(null);

        if (killer != null && killer instanceof Player) {
            final Player finalKiller = killer;
            final ItemStack finalItem = theWorldItem != null ? theWorldItem : plugin.getItemManager().createTheWorldItem();
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getItemManager().setHolder(finalKiller.getUniqueId());
                if (finalItem != null) {
                    finalKiller.getInventory().addItem(finalItem);
                }
                plugin.getItemManager().saveData();
                plugin.getItemManager().sendTitle(finalKiller);
                String broadcast = colorize(plugin.getConfig().getString("messages.holder-died-broadcast",
                    "&c&l%killer% &7has slain &e&l%holder% &7and claimed &e&lThe World&7!"));
                broadcast = broadcast.replace("%killer%", finalKiller.getName()).replace("%holder%", victim.getName());
                Bukkit.broadcastMessage(broadcast);
            });
        } else {
            plugin.getItemManager().setHolder(null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getItemManager().isHolder(player)) return;
        plugin.getItemManager().saveLastLocation(
            player.getLocation().getBlockX(),
            player.getLocation().getBlockY(),
            player.getLocation().getBlockZ(),
            player.getWorld().getName()
        );
        if (plugin.getCombatManager().isInCombat(player.getUniqueId())) {
            plugin.getCombatManager().handleQuit(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getItemManager().isHolder(player)) {
            boolean hasItem = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (plugin.getItemManager().isTheWorldItem(item)) {
                    hasItem = true;
                    break;
                }
            }
            if (!hasItem) {
                ItemStack item = plugin.getItemManager().createTheWorldItem();
                if (item != null) player.getInventory().addItem(item);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getItemManager().sendTitle(player), 20L);
        }
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}