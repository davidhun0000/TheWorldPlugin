package com.theworld.manager;

import com.theworld.TheWorldPlugin;
import com.cryptomorin.xseries.XSound;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TimeStopManager {

    private final TheWorldPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> frozenEntities = new HashSet<>();
    private int freezeTaskId = -1;

    public TimeStopManager(TheWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player) {
        Long end = cooldowns.get(player.getUniqueId());
        if (end == null) return false;
        if (System.currentTimeMillis() > end) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public long getRemainingCooldownSeconds(Player player) {
        Long end = cooldowns.get(player.getUniqueId());
        if (end == null) return 0;
        return Math.max(0, (end - System.currentTimeMillis()) / 1000);
    }

    public void applyRandomCooldown(Player player) {
        int minMin = plugin.getConfig().getInt("cooldown.min-minutes", 10);
        int maxMin = plugin.getConfig().getInt("cooldown.max-minutes", 20);
        int range = maxMin - minMin;
        int chosen = minMin + (range > 0 ? (int)(Math.random() * (range + 1)) : 0);
        long end = System.currentTimeMillis() + (long) chosen * 60 * 1000;
        cooldowns.put(player.getUniqueId(), end);
    }

    public void activateTimeStop(Player holder) {
        if (isOnCooldown(holder)) {
            long remaining = getRemainingCooldownSeconds(holder);
            String msg = colorize(plugin.getConfig().getString("messages.item-on-cooldown", "&cThe World is recharging! Time remaining: &e%time%"));
            msg = msg.replace("%time%", formatTime(remaining));
            holder.sendMessage(msg);
            return;
        }

        applyRandomCooldown(holder);

        String activateMsg = colorize(plugin.getConfig().getString("messages.item-activated", "&e&lZA WARUDO!"));
        holder.sendMessage(activateMsg);
        holder.sendTitle("\u00a7e\u00a7lZA WARUDO!", "\u00a77Time is stopped!", 5, 40, 10);

        XSound.matchXSound("ENTITY_ENDER_DRAGON_GROWL").ifPresent(s -> s.play(holder));

        Location center = holder.getLocation();
        for (Entity entity : center.getWorld().getNearbyEntities(center, 100, 100, 100)) {
            if (entity.getUniqueId().equals(holder.getUniqueId())) continue;
            frozenEntities.add(entity.getUniqueId());
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setAI(false);
            }
            entity.setVelocity(new Vector(0, 0, 0));
        }

        startFreezeTickTask(center);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            unfreezeAll();
            stopFreezeTask();
        }, 60L);
    }

    private void startFreezeTickTask(Location center) {
        stopFreezeTask();
        freezeTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : frozenEntities) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null) {
                    entity.setVelocity(new Vector(0, 0, 0));
                }
            }
        }, 1L, 1L).getTaskId();
    }

    private void stopFreezeTask() {
        if (freezeTaskId != -1) {
            Bukkit.getScheduler().cancelTask(freezeTaskId);
            freezeTaskId = -1;
        }
    }

    private void unfreezeAll() {
        for (UUID uuid : frozenEntities) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setAI(true);
            }
        }
        frozenEntities.clear();
    }

    public void removeCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    private String formatTime(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        return min + "m " + sec + "s";
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}