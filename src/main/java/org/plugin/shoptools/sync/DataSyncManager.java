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
import java.util.logging.Level;
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

    /** 连续同步失败次数（用于限频告警） */
    private int consecutiveSyncFailures = 0;
    /** 上次输出失败告警的时间戳 */
    private long lastFailureLogTime = 0;
    /** 失败告警限频间隔：10 分钟 */
    private static final long FAILURE_LOG_INTERVAL_MS = 10 * 60 * 1000;
    /** 当前限频窗口内是否已经输出过完整堆栈 */
    private boolean failureStackLoggedInWindow = false;
    /** 是否已经输出过"无商店数据"告警（数据出现后会重置） */
    private boolean emptyDataWarned = false;
    
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
     * 执行初始同步（不触发库存扫描，启动时已由 loadDataAsync 回调启动扫描）
     */
    private void performInitialSync() {
        logger.info("开始执行初始数据同步...");

        if (!quickShopIntegration.isQuickShopAvailable()) {
            logger.warning("QuickShop不可用，跳过初始同步。");
            return;
        }

        // 三步走：主线程取数 → 异步重建+落盘 → 完成回调标记初始同步结束
        syncShopData(false, () -> {
            isInitialSyncCompleted = true;
            logger.info("初始数据同步完成。");
        });
    }

    /**
     * 启动定期同步任务（每次同步完成后同时触发库存重新扫描）
     */
    private void startPeriodicSync() {
        long interval = configManager.getSyncInterval() / 1000 * 20; // 转换为tick

        logger.info("启动定期数据同步，间隔: " + (interval / 20) + " 秒");

        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isSyncing && quickShopIntegration.isQuickShopAvailable()) {
                    // 异步执行：取数→重建→落盘都不占主线程（2026-10-21 从主线程迁移）
                    syncShopData(true); // 周期同步完成后触发库存重新扫描
                }
            }
        }.runTaskTimerAsynchronously(plugin, interval, interval);
    }
    
    /**
     * 手动执行数据同步（同时触发库存重新扫描，适用于 reload 指令）。
     * <p>
     * 注意：同步本身异步执行，本方法返回仅代表"已受理"，完成/失败通过日志输出。
     *
     * @return 是否成功受理同步请求（同步进行中或 QuickShop 不可用时为 false）
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
        syncShopData(true, () -> logger.info("手动数据同步完成。"));
        return true;
    }

    /**
     * 周期同步成功：重置失败计数与限频状态
     */
    private void onSyncSuccess() {
        if (consecutiveSyncFailures > 0) {
            logger.info("定期数据同步已恢复，此前连续失败 " + consecutiveSyncFailures + " 次。");
        }
        consecutiveSyncFailures = 0;
        lastFailureLogTime = 0;
        failureStackLoggedInWindow = false;
    }

    /**
     * 周期同步失败：告警限频——同一故障每 10 分钟只输出一次告警与堆栈，
     * 窗口内静默累计失败次数，避免日志刷屏与 IO 抖动。
     *
     * @param e 同步异常
     */
    private void onSyncFailure(Exception e) {
        consecutiveSyncFailures++;

        long now = System.currentTimeMillis();
        if (now - lastFailureLogTime < FAILURE_LOG_INTERVAL_MS) {
            // 限频窗口内：仅 debug 输出堆栈，不刷控制台
            if (configManager.isDebugEnabled()) {
                logger.log(Level.WARNING, "[debug] 定期数据同步失败（已连续 " + consecutiveSyncFailures + " 次）: " + e.getMessage(), e);
            }
            return;
        }

        lastFailureLogTime = now;
        if (!failureStackLoggedInWindow) {
            failureStackLoggedInWindow = true;
            logger.log(Level.WARNING, "定期数据同步失败（已连续 " + consecutiveSyncFailures + " 次，后续 10 分钟内仅静默计数）: " + e.getMessage(), e);
        } else {
            logger.warning("定期数据同步失败（已连续 " + consecutiveSyncFailures + " 次）: " + e.getMessage());
        }
    }

    /**
     * 执行商店数据同步（三步走，均可从任意线程发起）：
     * <ol>
     *   <li>主线程：从 QuickShop 读取商店数据（QuickShop API 要求主线程）</li>
     *   <li>异步线程：重建缓存 + 落盘（原在主线程执行，是周期性卡顿的主要来源）</li>
     *   <li>主线程：按需触发库存重新扫描（StockScanQueue 要求主线程）</li>
     * </ol>
     * 失败统一走 {@link #onSyncFailure(Exception)}（10 分钟限频），成功走
     * {@link #onSyncSuccess()} 后执行完成回调。
     *
     * @param triggerStockScan 同步成功后是否触发库存重新扫描
     */
    private void syncShopData(boolean triggerStockScan) {
        syncShopData(triggerStockScan, null);
    }

    /**
     * 执行商店数据同步，完成后执行回调。
     *
     * @param triggerStockScan 同步成功后是否触发库存重新扫描
     * @param onComplete       同步成功后的回调（可为 null）
     */
    private void syncShopData(boolean triggerStockScan, Runnable onComplete) {
        if (isSyncing) {
            return;
        }

        isSyncing = true;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.isEnabled()) {
                isSyncing = false;
                return;
            }

            final List<ShopData> shopDataList;
            try {
                shopDataList = quickShopIntegration.getAllShops();
            } catch (Exception e) {
                isSyncing = false;
                onSyncFailure(e);
                return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // "无商店数据"限频：只在状态变化（有数据→无数据）时告警一次
                    if (shopDataList == null || shopDataList.isEmpty()) {
                        if (!emptyDataWarned) {
                            emptyDataWarned = true;
                            logger.warning("未获取到任何商店数据。");
                        } else if (configManager.isDebugEnabled()) {
                            logger.info("[debug] 本轮同步仍未获取到商店数据。");
                        }
                        return;
                    }
                    emptyDataWarned = false;

                    dataManager.updateShopData(shopDataList);
                    onSyncSuccess();
                    if (onComplete != null) {
                        onComplete.run();
                    }

                    if (triggerStockScan && plugin.isEnabled()) {
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.triggerStockScan());
                    }
                } catch (Exception e) {
                    onSyncFailure(e);
                } finally {
                    isSyncing = false;
                }
            });
        });
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
