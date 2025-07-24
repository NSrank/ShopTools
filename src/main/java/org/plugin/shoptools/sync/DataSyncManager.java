package org.plugin.shoptools.sync;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugin.shoptools.ShopTools;
import org.plugin.shoptools.config.ConfigManager;
import org.plugin.shoptools.integration.QuickShopIntegration;
import org.plugin.shoptools.model.ShopData;
import org.plugin.shoptools.storage.ShopDataManager;

import java.util.List;
import java.util.logging.Logger;

/**
 * 数据同步管理器
 * 负责定期从QuickShop同步商店数据
 * 
 * @author NSrank & Augment
 */
public class DataSyncManager {
    
    private final ShopTools plugin;
    private final Logger logger;
    private final ConfigManager configManager;
    private final QuickShopIntegration quickShopIntegration;
    private final ShopDataManager dataManager;
    
    private BukkitTask syncTask;
    private boolean isInitialSyncCompleted = false;
    private boolean isSyncing = false;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param quickShopIntegration QuickShop集成
     * @param dataManager 数据管理器
     */
    public DataSyncManager(ShopTools plugin, ConfigManager configManager, 
                          QuickShopIntegration quickShopIntegration, ShopDataManager dataManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configManager = configManager;
        this.quickShopIntegration = quickShopIntegration;
        this.dataManager = dataManager;
    }
    
    /**
     * 启动数据同步
     */
    public void startSync() {
        logger.info("启动数据同步管理器...");
        
        // 延迟执行初始同步，确保服务器完全启动
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            performInitialSync();
            
            // 如果启用了自动同步，启动定期同步任务
            if (configManager.isAutoSyncEnabled()) {
                startPeriodicSync();
            }
        }, 100L); // 5秒延迟
    }
    
    /**
     * 停止数据同步
     */
    public void stopSync() {
        logger.info("停止数据同步管理器...");
        
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
            syncTask = null;
        }
    }
    
    /**
     * 执行初始同步
     */
    private void performInitialSync() {
        logger.info("开始执行初始数据同步...");

        if (!quickShopIntegration.isQuickShopAvailable()) {
            logger.warning("QuickShop不可用，跳过初始同步。");
            return;
        }

        // 在主线程执行同步操作
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                syncShopData();
                isInitialSyncCompleted = true;
                logger.info("初始数据同步完成。");
            } catch (Exception e) {
                logger.severe("初始数据同步失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 启动定期同步任务
     */
    private void startPeriodicSync() {
        long interval = configManager.getSyncInterval() / 1000 * 20; // 转换为tick
        
        logger.info("启动定期数据同步，间隔: " + (interval / 20) + " 秒");
        
        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSyncing && quickShopIntegration.isQuickShopAvailable()) {
                    // 在主线程执行同步
                    try {
                        syncShopData();

                        if (configManager.isDebugEnabled()) {
                            logger.info("定期数据同步完成。");
                        }
                    } catch (Exception e) {
                        logger.warning("定期数据同步失败: " + e.getMessage());
                        if (configManager.isDebugEnabled()) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }
    
    /**
     * 手动执行数据同步
     *
     * @return 同步是否成功
     */
    public boolean performManualSync() {
        if (isSyncing) {
            logger.warning("数据同步正在进行中，请稍后再试。");
            return false;
        }

        if (!quickShopIntegration.isQuickShopAvailable()) {
            logger.warning("QuickShop不可用，无法执行同步。");
            return false;
        }

        logger.info("开始手动数据同步...");

        // 检查是否在主线程
        if (Bukkit.isPrimaryThread()) {
            // 已经在主线程，直接执行
            try {
                syncShopData();
                logger.info("手动数据同步完成。");
                return true;
            } catch (Exception e) {
                logger.severe("手动数据同步失败: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // 不在主线程，切换到主线程执行
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    syncShopData();
                    logger.info("手动数据同步完成。");
                } catch (Exception e) {
                    logger.severe("手动数据同步失败: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return true; // 异步执行，假设成功
        }
    }
    
    /**
     * 执行商店数据同步
     */
    private void syncShopData() {
        if (isSyncing) {
            return;
        }
        
        isSyncing = true;
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 从QuickShop获取所有商店数据
            List<ShopData> shopDataList = quickShopIntegration.getAllShops();
            
            if (shopDataList == null || shopDataList.isEmpty()) {
                logger.warning("未获取到任何商店数据。");
                return;
            }
            
            // 更新数据管理器中的数据
            dataManager.updateShopData(shopDataList);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            logger.info("数据同步完成，耗时: " + duration + "ms，同步了 " + shopDataList.size() + " 个商店。");
            
        } finally {
            isSyncing = false;
        }
    }
    
    /**
     * 检查初始同步是否完成
     * 
     * @return 如果初始同步已完成返回true
     */
    public boolean isInitialSyncCompleted() {
        return isInitialSyncCompleted;
    }
    
    /**
     * 检查是否正在同步
     * 
     * @return 如果正在同步返回true
     */
    public boolean isSyncing() {
        return isSyncing;
    }
    
    /**
     * 获取最后同步时间
     * 
     * @return 最后同步时间戳
     */
    public long getLastSyncTime() {
        return dataManager.getLastUpdateTime();
    }
    
    /**
     * 重新启动同步任务
     */
    public void restartSync() {
        logger.info("重新启动数据同步...");
        
        stopSync();
        
        // 重新初始化QuickShop连接
        quickShopIntegration.reinitialize();
        
        // 重新启动同步
        startSync();
    }
}
