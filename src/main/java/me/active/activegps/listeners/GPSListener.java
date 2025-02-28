package me.active.activegps.listeners;

import me.active.activegps.Main;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GPSListener implements Listener {
    private final Main plugin;
    private Map<Player, Location> markedLocations = new HashMap<>();
    private Map<Player, Integer> playerPages = new HashMap<>();

    public GPSListener(Main plugin) {
        this.plugin = plugin;
    }

    private String getMessage(String key) {
        FileConfiguration messagesConfig = plugin.getMessages();
        return ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(key, "Message not found"));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
            if (event.getAction().name().contains("CLICK")) {
                openGPSMenu(player, 1);
            }
        }
    }

    private void openGPSMenu(Player player, int page) {
        FileConfiguration config = plugin.getConfig();
        String menuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("general.display-menu", "GPS Menu"));
        Inventory menu = Bukkit.createInventory(null, 54, menuTitle); // تعداد اسلات‌ها را 54 در نظر می‌گیریم (9 ردیف 6 تایی)

        int itemsPerPage = 45;
        List<String> keys = config.getConfigurationSection("menu").getKeys(false).stream().collect(Collectors.toList());
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(page * itemsPerPage, keys.size());

        for (int i = startIndex; i < endIndex; i++) {
            String key = keys.get(i);
            Material material = Material.valueOf(config.getString("menu." + key + ".item"));
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("menu." + key + ".display-name")));
            meta.setLore(config.getStringList("menu." + key + ".lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList()));
            item.setItemMeta(meta);
            menu.addItem(item);
        }


        String cancelName = ChatColor.translateAlternateColorCodes('&', config.getString("general.cancel-name"));
        String cancelLore = ChatColor.translateAlternateColorCodes('&', config.getString("general.cancel-lore"));
        Material cancelItemType = Material.valueOf(config.getString("general.cancel-item"));
        int cancelSlot = config.getInt("general.cancel-slot");

        ItemStack cancelItem = new ItemStack(cancelItemType);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(cancelName);
        cancelMeta.setLore(Collections.singletonList(cancelLore));
        cancelItem.setItemMeta(cancelMeta);

        menu.setItem(cancelSlot, cancelItem);


        int nextPageSlot = config.getInt("general.next-page-slot");
        int previousPageSlot = config.getInt("general.previous-page-slot");
        Material nextPageItem = Material.valueOf(config.getString("general.next-page-item"));
        Material previousPageItem = Material.valueOf(config.getString("general.previous-page-item"));
        int maxPage = config.getInt("general.max-page");


        if (page < maxPage) {
            ItemStack nextPage = new ItemStack(nextPageItem);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName(ChatColor.GREEN + "Next Page");
            nextPage.setItemMeta(nextMeta);

            if (nextPageSlot >= 0 && nextPageSlot < 54) {
                menu.setItem(nextPageSlot, nextPage);
            }
        }


        if (page > 0) {
            ItemStack previousPage = new ItemStack(previousPageItem);
            ItemMeta prevMeta = previousPage.getItemMeta();
            prevMeta.setDisplayName(ChatColor.RED + "Previous Page");
            previousPage.setItemMeta(prevMeta);

            if (previousPageSlot >= 0 && previousPageSlot < 54) {
                menu.setItem(previousPageSlot, previousPage);
            }
        }

        player.openInventory(menu);
        playerPages.put(player, page);
    }



    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        FileConfiguration config = plugin.getConfig();
        String menuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("general.display-menu", "GPS Menu"));

        if (event.getView().getTitle().equals(menuTitle)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null) {
                String itemName = clickedItem.getType().name();

                if (itemName.equals(config.getString("general.next-page-item"))) {
                    int currentPage = playerPages.getOrDefault(player, 1);
                    openGPSMenu(player, currentPage + 1);
                }

                if (itemName.equals(config.getString("general.previous-page-item"))) {
                    int currentPage = playerPages.getOrDefault(player, 1);
                    openGPSMenu(player, currentPage - 1);
                }

                if (clickedItem.getType() == Material.valueOf(config.getString("general.cancel-item"))) {
                    markedLocations.remove(player);
                    player.sendMessage(getMessage("messages.gps_cancelled"));
                    return;
                }

                for (String key : config.getConfigurationSection("menu").getKeys(false)) {
                    if (clickedItem.getType() == Material.valueOf(config.getString("menu." + key + ".item"))) {
                        int x = config.getInt("menu." + key + ".location.x");
                        int y = config.getInt("menu." + key + ".location.y");
                        int z = config.getInt("menu." + key + ".location.z");
                        Location location = new Location(player.getWorld(), x, y, z);
                        markedLocations.put(player, location);

                        player.setCompassTarget(location);
                        player.sendMessage(getMessage("messages.gps_updated").replace("{location}", key));
                        return;
                    }
                }
            }
        }
    }

    public void updateActionBar(Player player) {
        if (markedLocations.containsKey(player)) {
            Location markedLocation = markedLocations.get(player);
            double distance = player.getLocation().distance(markedLocation);
            String message = ChatColor.GREEN + "Distance to GPS: " + String.format("%.2f", distance) + " meters";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }
}
