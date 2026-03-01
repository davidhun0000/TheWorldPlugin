package com.theworld.placeholder;

import com.theworld.TheWorldPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TheWorldPlaceholder extends PlaceholderExpansion {

    private final TheWorldPlugin plugin;

    public TheWorldPlaceholder(TheWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "theworld";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TheWorldPlugin";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("title")) {
            if (player instanceof Player) {
                Player online = (Player) player;
                if (plugin.getItemManager().isHolder(online)) {
                    return "\u00a7e\u00a7lKing of the World";
                }
            }
            return "";
        }

        if (params.equalsIgnoreCase("holder")) {
            java.util.UUID holderUUID = plugin.getItemManager().getHolderUUID();
            if (holderUUID == null) return "None";
            OfflinePlayer holder = Bukkit.getOfflinePlayer(holderUUID);
            return holder.getName() != null ? holder.getName() : "Unknown";
        }

        if (params.equalsIgnoreCase("has_item")) {
            if (player instanceof Player) {
                return plugin.getItemManager().isHolder((Player) player) ? "true" : "false";
            }
            return "false";
        }

        if (params.equalsIgnoreCase("cooldown")) {
            if (player instanceof Player) {
                Player online = (Player) player;
                long remaining = plugin.getTimeStopManager().getRemainingCooldownSeconds(online);
                if (remaining <= 0) return "Ready";
                long min = remaining / 60;
                long sec = remaining % 60;
                return min + "m " + sec + "s";
            }
            return "N/A";
        }

        return null;
    }
}
