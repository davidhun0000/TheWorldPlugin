package com.theworld.listener;

import com.theworld.TheWorldPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemListener implements Listener {

    private final TheWorldPlugin plugin;

    public ItemListener(TheWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !plugin.getItemManager().isTheWorldItem(item)) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        if (!plugin.getItemManager().isHolder(player)) return;
        plugin.getTimeStopManager().activateTimeStop(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        if (!plugin.getItemManager().isTheWorldItem(item)) return;
        event.setCancelled(true);
        String msg = colorize(plugin.getConfig().getString("messages.cannot-drop", "&cYou cannot drop The World!"));
        player.sendMessage(msg);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (isTheWorld(cursor) || isTheWorld(current)) {
            if (isOtherPlayerInventory(event)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        if (!plugin.getItemManager().isTheWorldItem(item)) return;
        if (plugin.getItemManager().hasHolder() && !plugin.getItemManager().isHolder(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isOtherPlayerInventory(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return false;
        return event.getClickedInventory() != ((Player) event.getWhoClicked()).getInventory();
    }

    private boolean isTheWorld(ItemStack item) {
        return plugin.getItemManager().isTheWorldItem(item);
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}
