package com.theworld;

import com.theworld.listener.DragonListener;
import com.theworld.listener.ItemListener;
import com.theworld.listener.PlayerListener;
import com.theworld.manager.CombatManager;
import com.theworld.manager.CoordinateManager;
import com.theworld.manager.ItemManager;
import com.theworld.manager.TimeStopManager;
import com.theworld.placeholder.TheWorldPlaceholder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TheWorldPlugin extends JavaPlugin {

    @Getter private ItemManager itemManager;
    @Getter private CombatManager combatManager;
    @Getter private CoordinateManager coordinateManager;
    @Getter private TimeStopManager timeStopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.itemManager = new ItemManager(this);
        this.combatManager = new CombatManager(this);
        this.timeStopManager = new TimeStopManager(this);
        this.coordinateManager = new CoordinateManager(this);

        getServer().getPluginManager().registerEvents(new DragonListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TheWorldPlaceholder(this).register();
        }

        getCommand("theworld").setExecutor(new com.theworld.command.TheWorldCommand(this));

        coordinateManager.startBroadcastTask();
    }

    @Override
    public void onDisable() {
        itemManager.saveData();
        combatManager.shutdown();
    }
}
