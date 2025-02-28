package me.active.activegps.commands;

import me.active.activegps.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class CommandHandler implements CommandExecutor {
    private final Main plugin;

    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            String gpsName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.gps-name", "&a&lGPS"));
            ItemStack gps = new ItemStack(Material.COMPASS);
            gps.getItemMeta().setDisplayName(gpsName);
            player.getInventory().addItem(gps);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessages().getString("messages.gps_received", "&aYou have received a GPS!")));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("activegps.admin")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessages().getString("messages.no_permission", "&cYou don't have permission to do that!")));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadMessages();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessages().getString("messages.config_reloaded", "&aConfiguration reloaded successfully!")));
            return true;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessages().getString("messages.invalid_command", "&cInvalid command. Use /gps or /gps reload.")));
        return true;
    }
}