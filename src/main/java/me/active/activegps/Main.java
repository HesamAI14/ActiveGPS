package me.active.activegps;

import me.active.activegps.commands.CommandHandler;
import me.active.activegps.listeners.GPSListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {
    private FileConfiguration messagesConfig;
    private GPSListener gpsListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();

        getCommand("gps").setExecutor(new CommandHandler(this));
        gpsListener = new GPSListener(this);
        Bukkit.getPluginManager().registerEvents(gpsListener, this);


        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                gpsListener.updateActionBar(player);
            }
        }, 0L, 20L);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    public void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }
}
