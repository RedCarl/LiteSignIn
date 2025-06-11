package studio.trc.bukkit.litesignin.database.util;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import studio.trc.bukkit.litesignin.configuration.ConfigurationType;
import studio.trc.bukkit.litesignin.configuration.ConfigurationUtil;
import studio.trc.bukkit.litesignin.message.MessageUtil;
import studio.trc.bukkit.litesignin.database.storage.YamlStorage;
import studio.trc.bukkit.litesignin.database.storage.MySQLStorage;
import studio.trc.bukkit.litesignin.database.storage.SQLiteStorage;
import studio.trc.bukkit.litesignin.event.Menu;
import studio.trc.bukkit.litesignin.util.PluginControl;
import studio.trc.bukkit.litesignin.util.SignInDate;

public class BackupUtil
{
    private static CommandSender[] backupUsers = {};
    private static boolean backingup = false;
    
    public static boolean isBackingUp() {
        return backingup;
    }
    
    public static Runnable backupMethod = () -> {
        Bukkit.getOnlinePlayers().stream().filter(ps -> Menu.menuOpening.containsKey(ps.getUniqueId())).forEachOrdered(Player::closeInventory);
        try {
            if (!ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getBoolean("Database-Management.Backup.Enabled")) {
                return;
            }
            backingup = true;
            String fileName = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Database-Management.Backup.Backup-File") + ".db";
            Map<String, String> time = new HashMap();
            time.put("{time}", SignInDate.getInstance(new Date()).getName(ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Database-Management.Backup.Time-Format")));
            fileName = MessageUtil.replacePlaceholders(Bukkit.getConsoleSender(), fileName, time);
            String fileFolder = ConfigurationUtil.getConfig(ConfigurationType.CONFIG).getString("Database-Management.Backup.Backup-Folder-Path");
            File folder = new File(fileFolder);
            if (!folder.exists()) folder.mkdir();
            File file = new File(folder, fileName);
            if (!file.exists()) {
                file.createNewFile();
            } else {
                file.delete();
                file.createNewFile();
            }
            if (PluginControl.useMySQLStorage()) {
                MySQLStorage.backup(fileFolder + fileName);
            } else if (PluginControl.useSQLiteStorage()) {
                SQLiteStorage.backup(fileFolder + fileName);
            } else {
                YamlStorage.backup(fileFolder + fileName);
            }
            for (CommandSender sender : BackupUtil.backupUsers) {
                if (sender != null) {
                    Map<String, String> placeholders = MessageUtil.getDefaultPlaceholders();
                    placeholders.put("{file}",  fileName);
                    MessageUtil.sendMessage(sender, ConfigurationUtil.getConfig(ConfigurationType.MESSAGES), "Database-Management.Backup.Successfully", placeholders);
                }
            }
            backingup = false;
        } catch (Throwable t) {
            for (CommandSender sender : BackupUtil.backupUsers) {
                if (sender != null) {
                    Map<String, String> placeholders = MessageUtil.getDefaultPlaceholders();
                    placeholders.put("{error}",  t.getLocalizedMessage() != null ? t.getLocalizedMessage() : "null");
                    MessageUtil.sendMessage(sender, ConfigurationUtil.getConfig(ConfigurationType.MESSAGES), "Database-Management.Backup.Failed", placeholders);
                }
            }
            t.printStackTrace();
            backingup = false;
        }
    };
    
    public static Thread startBackup(CommandSender... users) {
        backupUsers = users;
        Thread thread = new Thread(backupMethod);
        thread.start();
        return thread;
    }
    
    public static void startSyncBackup(CommandSender... users) {
        backupUsers = users;
        backupMethod.run();
    }
}
