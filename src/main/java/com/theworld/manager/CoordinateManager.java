package com.theworld.manager;

import com.theworld.TheWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CoordinateManager {

    private final TheWorldPlugin plugin;
    private int onlineTaskId = -1;
    private int offlineTaskId = -1;

    public CoordinateManager(TheWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public void startBroadcastTask() {
        int onlineIntervalMin = plugin.getConfig().getInt("broadcasts.online-interval-minutes", 15);
        int offlineIntervalMin = plugin.getConfig().getInt("broadcasts.offline-interval-minutes", 60);

        long onlineTicks = (long) onlineIntervalMin * 60 * 20;
        long offlineTicks = (long) offlineIntervalMin * 60 * 20;

        onlineTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            UUID holderUUID = plugin.getItemManager().getHolderUUID();
            if (holderUUID == null) return;
            Player holder = Bukkit.getPlayer(holderUUID);
            if (holder == null || !holder.isOnline()) return;
            int x = holder.getLocation().getBlockX();
            int y = holder.getLocation().getBlockY();
            int z = holder.getLocation().getBlockZ();
            plugin.getItemManager().saveLastLocation(x, y, z, holder.getWorld().getName());
            broadcastOnline(x, y, z, holder.getWorld().getName());
        }, onlineTicks, onlineTicks).getTaskId();

        offlineTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            UUID holderUUID = plugin.getItemManager().getHolderUUID();
            if (holderUUID == null) return;
            Player holder = Bukkit.getPlayer(holderUUID);
            if (holder != null && holder.isOnline()) return;
            int x = plugin.getItemManager().getLastX();
            int y = plugin.getItemManager().getLastY();
            int z = plugin.getItemManager().getLastZ();
            String world = plugin.getItemManager().getLastWorld();
            if (world == null || world.isEmpty()) return;
            broadcastOffline(x, y, z, world);
        }, offlineTicks, offlineTicks).getTaskId();
    }

    private void broadcastOnline(int x, int y, int z, String world) {
        int[] xRange = getVagueRange(x, 400);
        int[] zRange = getVagueRange(z, 400);
        String template = plugin.getConfig().getString("messages.coordinate-broadcast-online", "");
        String msg = colorize(template)
            .replace("%x_min%", String.valueOf(xRange[0]))
            .replace("%x_max%", String.valueOf(xRange[1]))
            .replace("%z_min%", String.valueOf(zRange[0]))
            .replace("%z_max%", String.valueOf(zRange[1]))
            .replace("%y%", String.valueOf(y))
            .replace("%world%", world);
        Bukkit.broadcastMessage(msg);
    }

    private void broadcastOffline(int x, int y, int z, String world) {
        int[] xRange = getVagueRange(x, 200);
        int[] zRange = getVagueRange(z, 200);
        String template = plugin.getConfig().getString("messages.coordinate-broadcast-offline", "");
        String msg = colorize(template)
            .replace("%x_min%", String.valueOf(xRange[0]))
            .replace("%x_max%", String.valueOf(xRange[1]))
            .replace("%z_min%", String.valueOf(zRange[0]))
            .replace("%z_max%", String.valueOf(zRange[1]))
            .replace("%y%", String.valueOf(y))
            .replace("%world%", world);
        Bukkit.broadcastMessage(msg);
    }

    private int[] getVagueRange(int coord, int step) {
        int low = (int)(Math.floor((double) coord / step) * step);
        int high = low + step;
        return new int[]{low, high};
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}