package com.theworld.manager;

import com.theworld.TheWorldPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final TheWorldPlugin plugin;
    private final Map<UUID, Long> combatEndTime = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private int taskId = -1;

    public CombatManager(TheWorldPlugin plugin) {
        this.plugin = plugin;
        startCombatTask();
    }

    private void startCombatTask() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            combatEndTime.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue());
        }, 20L, 20L).getTaskId();
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void enterCombat(Player holder, Player attacker) {
        int durationMinutes = plugin.getConfig().getInt("combat.duration-minutes", 20);
        long end = System.currentTimeMillis() + (long) durationMinutes * 60 * 1000;
        combatEndTime.put(holder.getUniqueId(), end);
        lastAttacker.put(holder.getUniqueId(), attacker.getUniqueId());
        String msg = colorize(plugin.getConfig().getString("messages.combat-started", "&cYou are now in combat!"));
        holder.sendMessage(msg);
    }

    public void exitCombat(UUID uuid) {
        if (combatEndTime.remove(uuid) != null) {
            lastAttacker.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                String msg = colorize(plugin.getConfig().getString("messages.combat-ended", "&aYou are no longer in combat."));
                p.sendMessage(msg);
            }
        }
    }

    public boolean isInCombat(UUID uuid) {
        Long end = combatEndTime.get(uuid);
        if (end == null) return false;
        if (System.currentTimeMillis() > end) {
            combatEndTime.remove(uuid);
            lastAttacker.remove(uuid);
            return false;
        }
        return true;
    }

    public UUID getLastAttacker(UUID uuid) {
        return lastAttacker.get(uuid);
    }

    public void handleQuit(Player holder) {
        if (!isInCombat(holder.getUniqueId())) return;
        UUID attackerUUID = getLastAttacker(holder.getUniqueId());
        combatEndTime.remove(holder.getUniqueId());
        lastAttacker.remove(holder.getUniqueId());

        holder.setHealth(0);

        String broadcast = colorize(plugin.getConfig().getString("messages.combat-death-broadcast",
            "&c&l%player% &7logged out during combat and &cdied&7! &e&l%attacker% &7has claimed &e&lThe World&7!"));
        broadcast = broadcast.replace("%player%", holder.getName());

        Player attacker = attackerUUID != null ? Bukkit.getPlayer(attackerUUID) : null;
        if (attacker != null && attacker.isOnline()) {
            broadcast = broadcast.replace("%attacker%", attacker.getName());
            Bukkit.broadcastMessage(broadcast);
            plugin.getItemManager().transferItemTo(attacker, holder);
            attacker.sendTitle(
                colorize(plugin.getConfig().getString("messages.title-king", "&e&lKing of the World")),
                colorize(plugin.getConfig().getString("messages.title-subtitle", "&7Holder of The World")),
                10, 70, 20
            );
        } else {
            broadcast = broadcast.replace("%attacker%", "Unknown");
            Bukkit.broadcastMessage(broadcast);
            plugin.getItemManager().setHolder(null);
        }
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}