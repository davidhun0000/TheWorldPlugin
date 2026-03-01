package com.theworld.manager;

import com.theworld.TheWorldPlugin;
import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XItemFlag;

import com.cryptomorin.xseries.XMaterial;

import com.cryptomorin.xseries.XAttribute;

import com.cryptomorin.xseries.XPotion;

import lombok.Getter;

import org.bukkit.potion.PotionEffectType;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemManager {

    private final TheWorldPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    @Getter private UUID holderUUID;
    @Getter private boolean dragonDefeated;

    public ItemManager(TheWorldPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        String uuidStr = dataConfig.getString("holder-uuid", "");
        this.dragonDefeated = dataConfig.getBoolean("dragon-defeated", false);
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                this.holderUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                this.holderUUID = null;
            }
        }
    }

    public void saveData() {
        dataConfig.set("holder-uuid", holderUUID != null ? holderUUID.toString() : "");
        dataConfig.set("dragon-defeated", dragonDefeated);
        if (holderUUID != null) {
            Player holder = Bukkit.getPlayer(holderUUID);
            if (holder != null && holder.isOnline()) {
                dataConfig.set("last-location.world", holder.getWorld().getName());
                dataConfig.set("last-location.x", holder.getLocation().getBlockX());
                dataConfig.set("last-location.y", holder.getLocation().getBlockY());
                dataConfig.set("last-location.z", holder.getLocation().getBlockZ());
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDragonDefeated(boolean defeated) {
        this.dragonDefeated = defeated;
        saveData();
    }

    public void setHolder(UUID uuid) {
        this.holderUUID = uuid;
        saveData();
    }

    public boolean hasHolder() {
        return holderUUID != null;
    }

    public boolean isHolder(Player player) {
        return holderUUID != null && holderUUID.equals(player.getUniqueId());
    }

    public Player getHolderPlayer() {
        if (holderUUID == null) return null;
        return Bukkit.getPlayer(holderUUID);
    }

    public ItemStack createTheWorldItem() {
        ItemStack item = XMaterial.matchXMaterial("CLOCK").map(XMaterial::parseItem).orElse(null);
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        meta.setDisplayName("\u00a7e\u00a7lThe World");
        List<String> lore = Arrays.asList(
            "\u00a77\u00a7oZA WARUDO!",
            "\u00a77Right-click to stop time",
            "\u00a77in a \u00a7e100 block radius",
            "\u00a77for \u00a7e3 seconds\u00a77.",
            "",
            "\u00a78Holder: \u00a7eKing of the World"
        );
        meta.setLore(lore);
        XEnchantment.matchXEnchantment("UNBREAKING").ifPresent(e -> meta.addEnchant(e.getEnchant(), 3, true));
        XItemFlag.of("HIDE_ENCHANTS").ifPresent(f -> f.set(meta));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTheWorldItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().equals("\u00a7e\u00a7lThe World");
    }


    public void giveItemToPlayer(Player player) {

        ItemStack item = createTheWorldItem();

        if (item == null) return;

        player.getInventory().addItem(item);

        setHolder(player.getUniqueId());

        sendTitle(player);

        applyBuffs(player);

    }



    public void transferItemTo(Player newHolder, Player oldHolder) {

        if (oldHolder != null) {

            removeBuffs(oldHolder);

        }

        if (oldHolder != null && oldHolder.isOnline()) {

            for (int i = 0; i < oldHolder.getInventory().getSize(); i++) {

                ItemStack slot = oldHolder.getInventory().getItem(i);

                if (isTheWorldItem(slot)) {

                    oldHolder.getInventory().setItem(i, null);

                    break;

                }

            }

        }

        giveItemToPlayer(newHolder);

    }


    public void removeItemFromHolder() {
        if (holderUUID == null) return;
        Player holder = Bukkit.getPlayer(holderUUID);
        if (holder != null && holder.isOnline()) {
            for (int i = 0; i < holder.getInventory().getSize(); i++) {
                ItemStack slot = holder.getInventory().getItem(i);
                if (isTheWorldItem(slot)) {
                    holder.getInventory().setItem(i, null);
                    break;
                }
            }
        }
        setHolder(null);
    }


    public void sendTitle(Player player) {

        String title = plugin.getConfig().getString("messages.title-king", "&e&lKing of the World");

        String subtitle = plugin.getConfig().getString("messages.title-subtitle", "&7Holder of The World");

        player.sendTitle(colorize(title), colorize(subtitle), 10, 70, 20);

    }

// Az applyBuffs-ban a módosító fix névvel: "KingHealth"
public void applyBuffs(Player player) {
    if (!plugin.getConfig().getBoolean("buffs.enabled", true)) return;
    
    double extraHP = plugin.getConfig().getInt("buffs.extra-hearts", 4) * 2.0;

    XAttribute.of("MAX_HEALTH").ifPresent(attr -> {
        var attribute = player.getAttribute(attr.get());
        if (attribute != null) {
            // Törlés, hogy ne halmozódjon
            attribute.getModifiers().forEach(mod -> {
                if (mod.getName().equals("KingHealth")) attribute.removeModifier(mod);
            });
            
            // Hozzáadás
            attribute.addModifier(new org.bukkit.attribute.AttributeModifier(
                UUID.randomUUID(), "KingHealth", extraHP, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER));
        }
    });

    // Erő effekt
    XPotion.matchXPotion("STRENGTH").ifPresent(xp -> 
        player.addPotionEffect(xp.buildPotionEffect(Integer.MAX_VALUE, plugin.getConfig().getInt("buffs.strength-level", 1) - 1))
    );
}

    public void removeBuffs(Player player) {

        if (!plugin.getConfig().getBoolean("buffs.enabled", true)) return;

        int extraHearts = plugin.getConfig().getInt("buffs.extra-hearts", 4);

        XAttribute.of("max_health").ifPresent(attr -> {

            double current = player.getAttribute(attr.get()).getBaseValue();

            player.getAttribute(attr.get()).setBaseValue(Math.max(0, current - extraHearts));

        });

        XPotion.matchXPotion("INCREASE_DAMAGE").ifPresent(xp -> player.removePotionEffect(xp.getPotionEffectType()));

    }


    public void saveLastLocation(int x, int y, int z, String world) {
        dataConfig.set("last-location.world", world);
        dataConfig.set("last-location.x", x);
        dataConfig.set("last-location.y", y);
        dataConfig.set("last-location.z", z);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLastX() { return dataConfig.getInt("last-location.x", 0); }
    public int getLastY() { return dataConfig.getInt("last-location.y", 0); }
    public int getLastZ() { return dataConfig.getInt("last-location.z", 0); }
    public String getLastWorld() { return dataConfig.getString("last-location.world", ""); }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}
