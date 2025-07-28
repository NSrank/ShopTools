package org.plugin.shoptools;

import org.bukkit.plugin.java.JavaPlugin;
import org.plugin.shoptools.command.ShopToolsCommand;
import org.plugin.shoptools.config.ConfigManager;
import org.plugin.shoptools.integration.QuickShopIntegration;
import org.plugin.shoptools.manager.LocationManager;
import org.plugin.shoptools.storage.ShopDataManager;
import org.plugin.shoptools.sync.DataSyncManager;

/**
 * ShopTools主类
 * 一个帮助玩家更好使用QuickShop-Reremake插件的工具
 *
 * @author NSrank & Augment
 */
public final class ShopTools extends JavaPlugin {

    private ConfigManager configManager;
    private QuickShopIntegration quickShopIntegration;
    private LocationManager locationManager;
    private ShopDataManager dataManager;
    private DataSyncManager syncManager;
    private ShopToolsCommand commandHandler;

    @Override
    public void onEnable() {
        getLogger().info("===================================");
        getLogger().info("ShopTools v1.1.1 正在启动...");
        getLogger().info("作者：NSrank & Augment");
        getLogger().info("===================================");

        try {
            // 初始化配置管理器
            initializeConfig();

            // 注册命令（先注册命令，即使QuickShop未就绪也能响应）
            registerCommands();

            // 延迟初始化QuickShop相关功能，确保QuickShop完全启动
            getServer().getScheduler().runTaskLater(this, this::initializeQuickShopIntegration, 60L); // 3秒延迟

            getLogger().info("ShopTools v1.1 基础功能启动完成！");
            getLogger().info("正在等待QuickShop插件完全启动...");

        } catch (Exception e) {
            getLogger().severe("ShopTools启动失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * 延迟初始化QuickShop集成功能
     */
    private void initializeQuickShopIntegration() {
        try {
            getLogger().info("开始初始化QuickShop集成...");

            // 初始化QuickShop集成
            initializeQuickShop();

            // 初始化数据管理器
            initializeDataManager();

            // 初始化同步管理器
            initializeSyncManager();

            // 更新命令处理器
            updateCommandHandler();

            getLogger().info("ShopTools v1.1 完全启动完成！");

        } catch (Exception e) {
            getLogger().severe("QuickShop集成初始化失败: " + e.getMessage());
            e.printStackTrace();

            // 重试机制：如果失败，30秒后再次尝试
            getLogger().warning("将在30秒后重试初始化...");
            getServer().getScheduler().runTaskLater(this, this::initializeQuickShopIntegration, 600L);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ShopTools 正在关闭...");

        try {
            // 停止同步管理器
            if (syncManager != null) {
                syncManager.stopSync();
            }

            getLogger().info("ShopTools 已安全关闭。");

        } catch (Exception e) {
            getLogger().severe("ShopTools关闭时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化配置管理器
     */
    private void initializeConfig() {
        getLogger().info("初始化配置管理器...");
        configManager = new ConfigManager(this);
        getLogger().info("配置管理器初始化完成。");

        getLogger().info("初始化位置管理器...");
        locationManager = new LocationManager(this, configManager);
        getLogger().info("位置管理器初始化完成。");
    }

    /**
     * 初始化QuickShop集成
     */
    private void initializeQuickShop() {
        getLogger().info("初始化QuickShop集成...");
        quickShopIntegration = new QuickShopIntegration(getLogger());

        if (!quickShopIntegration.isQuickShopAvailable()) {
            getLogger().warning("QuickShop不可用！插件功能将受限。");
        } else {
            getLogger().info("QuickShop集成初始化完成。");
        }
    }

    /**
     * 初始化数据管理器
     */
    private void initializeDataManager() {
        getLogger().info("初始化数据管理器...");
        dataManager = new ShopDataManager(getDataFolder(), configManager, getLogger());
        getLogger().info("数据管理器初始化完成。");
    }

    /**
     * 初始化同步管理器
     */
    private void initializeSyncManager() {
        getLogger().info("初始化同步管理器...");
        syncManager = new DataSyncManager(this, configManager, quickShopIntegration, dataManager);
        syncManager.startSync();
        getLogger().info("同步管理器初始化完成。");
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        getLogger().info("注册命令处理器...");
        commandHandler = new ShopToolsCommand(this, configManager, null, locationManager); // 初始时dataManager为null

        getCommand("shoptools").setExecutor(commandHandler);
        getCommand("shoptools").setTabCompleter(commandHandler);
        getCommand("st").setExecutor(commandHandler);
        getCommand("st").setTabCompleter(commandHandler);

        getLogger().info("命令处理器注册完成。");
    }

    /**
     * 更新命令处理器的数据管理器
     */
    private void updateCommandHandler() {
        if (commandHandler != null && dataManager != null) {
            commandHandler.setDataManager(dataManager);
            getLogger().info("命令处理器数据管理器已更新。");
        }
    }

    /**
     * 手动同步商店数据
     *
     * @return 同步是否成功
     */
    public boolean syncShopData() {
        if (syncManager != null) {
            return syncManager.performManualSync();
        }
        return false;
    }

    /**
     * 获取配置管理器
     *
     * @return 配置管理器实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取数据管理器
     *
     * @return 数据管理器实例
     */
    public ShopDataManager getDataManager() {
        return dataManager;
    }

    /**
     * 获取同步管理器
     *
     * @return 同步管理器实例
     */
    public DataSyncManager getSyncManager() {
        return syncManager;
    }
}
