package com.theworld.command;

import com.theworld.TheWorldPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TheWorldCommand implements CommandExecutor {

    private final TheWorldPlugin plugin;

    public TheWorldCommand(TheWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("theworld.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(colorize("&aConfig reloaded."));
                break;

            case "info":
                java.util.UUID holderUUID = plugin.getItemManager().getHolderUUID();
                if (holderUUID == null) {
                    sender.sendMessage(colorize("&7The World has no current holder."));
                } else {
                    org.bukkit.OfflinePlayer holder = Bukkit.getOfflinePlayer(holderUUID);
                    boolean online = holder.isOnline();
                    sender.sendMessage(colorize("&eThe World holder: &6" + (holder.getName() != null ? holder.getName() : "Unknown") +
                        " &7(" + (online ? "&aOnline" : "&cOffline") + "&7)"));
                    sender.sendMessage(colorize("&eDragon defeated before: &6" + plugin.getItemManager().isDragonDefeated()));
                }
                break;

            case "give":
                if (args.length < 2) {
                    sender.sendMessage(colorize("&cUsage: /theworld give <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found."));
                    return true;
                }
                if (plugin.getItemManager().hasHolder()) {
                    plugin.getItemManager().removeItemFromHolder();
                }
                plugin.getItemManager().giveItemToPlayer(target);
                sender.sendMessage(colorize("&aGave The World to &e" + target.getName() + "&a."));
                break;

            case "remove":
                if (!plugin.getItemManager().hasHolder()) {
                    sender.sendMessage(colorize("&cNo one currently holds The World."));
                    return true;
                }
                plugin.getItemManager().removeItemFromHolder();
                sender.sendMessage(colorize("&aThe World has been removed."));
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&e&lThe World Admin Commands:"));
        sender.sendMessage(colorize("&e/theworld reload &7- Reload config"));
        sender.sendMessage(colorize("&e/theworld info &7- Show holder info"));
        sender.sendMessage(colorize("&e/theworld give <player> &7- Give the item to a player"));
        sender.sendMessage(colorize("&e/theworld remove &7- Remove the item from holder"));
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }
}
