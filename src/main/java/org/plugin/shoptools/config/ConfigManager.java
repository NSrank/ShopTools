package org.plugin.shoptools.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * 配置文件管理器
 * 负责加载和管理插件的配置文件
 * 
 * @author NSrank & Augment
 */
public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    // 默认配置值
    private static final boolean DEFAULT_DEBUG = false;
    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final long DEFAULT_CACHE_EXPIRE_TIME = 300000L; // 5分钟
    private static final boolean DEFAULT_AUTO_SYNC = true;
    private static final long DEFAULT_SYNC_INTERVAL = 600000L; // 10分钟
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 设置默认值
        setDefaults();
        
        // 保存配置以确保所有默认值都被写入
        saveConfig();
    }
    
    /**
     * 设置默认配置值
     */
    private void setDefaults() {
        config.addDefault("debug", DEFAULT_DEBUG);
        config.addDefault("cache.size", DEFAULT_CACHE_SIZE);
        config.addDefault("cache.expire-time", DEFAULT_CACHE_EXPIRE_TIME);
        config.addDefault("sync.auto", DEFAULT_AUTO_SYNC);
        config.addDefault("sync.interval", DEFAULT_SYNC_INTERVAL);
        
        // 消息配置
        config.addDefault("messages.prefix", "&6[ShopTools] &r");
        config.addDefault("messages.no-permission", "&c你没有权限使用此命令！");
        config.addDefault("messages.player-only", "&c此命令只能由玩家执行！");
        config.addDefault("messages.invalid-command", "&c无效的命令！使用 /shoptools help 查看帮助。");
        config.addDefault("messages.quickshop-not-found", "&c未找到QuickShop插件！请确保已安装QuickShop-Reremake。");
        config.addDefault("messages.data-loading", "&a正在加载商店数据...");
        config.addDefault("messages.data-loaded", "&a商店数据加载完成！共加载 {count} 个商店。");
        config.addDefault("messages.data-load-failed", "&c商店数据加载失败！请检查控制台错误信息。");
        config.addDefault("messages.no-shops-found", "&e未找到任何商店数据。");
        config.addDefault("messages.item-not-found", "&e未找到物品 &6{item}&e 的商店。");
        config.addDefault("messages.player-not-found", "&e未找到玩家 &6{player}&e 的商店。");
        config.addDefault("messages.shop-list-header", "&6=== 商店列表 ===");
        config.addDefault("messages.shop-list-item", "&e{item} &7{location} &a{price} &b{owner}");
        config.addDefault("messages.shop-list-footer", "&6=== 共 {count} 个商店 ===");
        config.addDefault("messages.help-header", "&6=== ShopTools 帮助 ===");
        config.addDefault("messages.help-list", "&e/shoptools list &7- 显示所有商店");
        config.addDefault("messages.help-list-item", "&e/shoptools list <物品ID> &7- 显示指定物品的商店");
        config.addDefault("messages.help-who", "&e/shoptools who <玩家名> &7- 显示指定玩家的商店");
        config.addDefault("messages.help-reload", "&e/shoptools reload &7- 重新加载配置和数据");
        
        config.options().copyDefaults(true);
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存配置文件！", e);
        }
    }
    
    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("配置文件已重新加载。");
    }
    
    // 配置获取方法
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", DEFAULT_DEBUG);
    }
    
    public int getCacheSize() {
        return config.getInt("cache.size", DEFAULT_CACHE_SIZE);
    }
    
    public long getCacheExpireTime() {
        return config.getLong("cache.expire-time", DEFAULT_CACHE_EXPIRE_TIME);
    }
    
    public boolean isAutoSyncEnabled() {
        return config.getBoolean("sync.auto", DEFAULT_AUTO_SYNC);
    }
    
    public long getSyncInterval() {
        return config.getLong("sync.interval", DEFAULT_SYNC_INTERVAL);
    }

    /**
     * 获取ban命令调试模式设置
     *
     * @return 是否启用ban命令调试模式
     */
    public boolean getBanDebug() {
        return config.getBoolean("admin.ban-debug", false);
    }
    
    public String getMessage(String key) {
        return config.getString("messages." + key, "&c消息配置错误: " + key);
    }
    
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }
    
    /**
     * 获取原始配置对象
     * 
     * @return FileConfiguration对象
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
