package org.plugin.shoptools.scan;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.maxgamer.quickshop.api.shop.Shop;
import org.plugin.shoptools.config.ConfigManager;
import org.plugin.shoptools.integration.QuickShopIntegration;
import org.plugin.shoptools.model.ShopData;
import org.plugin.shoptools.storage.ShopDataManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 商店库存异步扫描队列
 * <p>
 * 整合自 FinderTools-Lib 的区块异步加载机制（{@code World.getChunkAtAsync}），
 * 将商店按所在区块分组后逐批调度，在区块加载完成的主线程回调中读取真实库存，
 * 全程不阻塞服务器主线程。
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>启动时将所有待扫描商店按 (世界, 区块X, 区块Z) 分组入队</li>
 *   <li>BukkitRunnable 定时器每 {@code tickDelay} tick 从队列取出最多
 *       {@code chunksPerTick} 个区块组进行处理</li>
 *   <li>已加载区块直接读取；未加载区块调用 {@code getChunkAtAsync(generate=false)}
 *       在主线程回调中读取，读完后卸载以节省内存</li>
 *   <li>全部扫描完成后异步保存 JSON，并输出统计日志</li>
 * </ol>
 *
 * @author NSrank & Augment
 */
public class StockScanQueue {

    /** 待扫描区块组的队列（主线程操作，无需并发集合） */
    private final Deque<ChunkGroup> queue = new ArrayDeque<>();

    /** 已派发但尚未完成的异步区块加载数量 */
    private final AtomicInteger pendingAsync = new AtomicInteger(0);

    private final Plugin plugin;
    private final QuickShopIntegration quickShopIntegration;
    private final ShopDataManager dataManager;
    private final ConfigManager configManager;
    private final Logger logger;

    private BukkitTask tickerTask;
    private Runnable onCompleteCallback;
    private long scanStartTime;
    private final AtomicInteger totalShopsScanned = new AtomicInteger(0);

    /**
     * 一个区块内所有待扫描商店的分组
     *
     * @param world   商店所在世界
     * @param chunkX  区块 X 坐标
     * @param chunkZ  区块 Z 坐标
     * @param shops   该区块内的商店列表
     */
    private record ChunkGroup(World world, int chunkX, int chunkZ, List<ShopData> shops) {}

    /**
     * 构造扫描队列。
     *
     * @param plugin               插件实例（用于调度 Bukkit 任务）
     * @param quickShopIntegration QuickShop 集成（用于按位置查找商店）
     * @param dataManager          数据管理器（扫描完成后保存 JSON）
     * @param configManager        配置管理器
     * @param logger               日志记录器
     */
    public StockScanQueue(Plugin plugin, QuickShopIntegration quickShopIntegration,
                          ShopDataManager dataManager, ConfigManager configManager, Logger logger) {
        this.plugin = plugin;
        this.quickShopIntegration = quickShopIntegration;
        this.dataManager = dataManager;
        this.configManager = configManager;
        this.logger = logger;
    }

    /**
     * 启动库存扫描。
     * <p>
     * 必须从主线程调用。内部将商店按区块分组后启动定时分批处理。
     *
     * @param allShops   需要扫描库存的商店列表（通常为全部缓存商店）
     * @param onComplete 扫描全部完成后的回调（在主线程执行）
     */
    public void start(List<ShopData> allShops, Runnable onComplete) {
        if (!configManager.isStockScanEnabled()) {
            logger.info("库存扫描已在配置中禁用，跳过。");
            return;
        }
        if (allShops == null || allShops.isEmpty()) {
            logger.info("没有需要扫描库存的商店。");
            return;
        }

        this.onCompleteCallback = onComplete;
        this.scanStartTime = System.currentTimeMillis();

        // 按 (世界, 区块X, 区块Z) 分组，跳过无限商店（库存永远充足）
        Map<String, ChunkGroup> groupMap = new LinkedHashMap<>();
        int skippedUnlimited = 0;
        for (ShopData shop : allShops) {
            if (shop.isUnlimited() || shop.getShopType() != ShopData.ShopType.SELLING) {
                skippedUnlimited++;
                continue;
            }
            org.bukkit.Location loc = shop.getLocation();
            if (loc == null || loc.getWorld() == null) continue;

            World world = loc.getWorld();
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            String key = world.getName() + ":" + cx + ":" + cz;

            groupMap.computeIfAbsent(key, k -> new ChunkGroup(world, cx, cz, new ArrayList<>()))
                    .shops().add(shop);
        }

        queue.addAll(groupMap.values());

        logger.info(String.format("库存扫描队列已就绪：共 %d 个区块（%d 家商店），跳过 %d 家无限/收购商店。",
                queue.size(), allShops.size() - skippedUnlimited, skippedUnlimited));

        startTicker();
    }

    /** 启动定时处理器（主线程） */
    private void startTicker() {
        int tickDelay = configManager.getStockScanTickDelay();
        int chunksPerTick = configManager.getStockScanChunksPerTick();

        tickerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isEnabled()) {
                    cancel();
                    return;
                }
                // 队列已空且无待处理的异步操作 → 扫描完成
                if (queue.isEmpty() && pendingAsync.get() == 0) {
                    cancel();
                    onScanComplete();
                    return;
                }
                // 每批次分派最多 chunksPerTick 个区块
                int dispatched = 0;
                while (!queue.isEmpty() && dispatched < chunksPerTick) {
                    processGroup(queue.poll());
                    dispatched++;
                }
            }
        }.runTaskTimer(plugin, 40L, tickDelay); // 延迟 40 tick（2秒）后开始，等待世界加载稳定
    }

    /**
     * 处理单个区块组：已加载则直接读取，未加载则异步加载后在主线程回调中读取。
     *
     * @param group 区块分组
     */
    private void processGroup(ChunkGroup group) {
        World world = group.world();
        int cx = group.chunkX();
        int cz = group.chunkZ();

        // 检查区块对应的 region 文件是否存在，避免对未生成区块发起无效加载请求
        if (!isChunkGenerated(world, cx, cz)) {
            return;
        }

        if (world.isChunkLoaded(cx, cz)) {
            // 区块已在内存中，直接读取（主线程安全）
            readStocks(group, false);
        } else {
            // 区块未加载：使用 Paper 的 getChunkAtAsync(generate=false)
            // 回调在主线程执行，不会阻塞当前 tick
            pendingAsync.incrementAndGet();
            world.getChunkAtAsync(cx, cz, false).thenAccept(chunk -> {
                try {
                    if (chunk != null && chunk.isLoaded()) {
                        readStocks(group, true);
                    }
                } finally {
                    pendingAsync.decrementAndGet();
                }
            }).exceptionally(ex -> {
                logger.warning("区块异步加载失败 [" + cx + "," + cz + "]: " + ex.getMessage());
                pendingAsync.decrementAndGet();
                return null;
            });
        }
    }

    /**
     * 在已加载的区块中为每家商店读取真实库存。
     * <p>
     * 关键修复：通过 {@link org.plugin.shoptools.storage.ShopDataManager#updateStockByLocation}
     * 更新 <em>当前缓存中的对象</em>，而非扫描队列启动时持有的旧引用。
     * 这确保了即使 DataSyncManager 在扫描过程中执行了全量同步（导致缓存被重建），
     * 库存更新也会正确写入现有的缓存对象，不会丢失。
     *
     * @param group      区块分组
     * @param wasNewLoad 本次是否由扫描器主动加载（用于决定是否卸载）
     */
    private void readStocks(ChunkGroup group, boolean wasNewLoad) {
        for (ShopData shopData : group.shops()) {
            try {
                Shop qsShop = quickShopIntegration.getShopAtLocation(shopData.getLocation());
                if (qsShop != null) {
                    int stock = qsShop.getRemainingStock();
                    // 直接更新当前缓存中的对象，避免孤儿引用问题
                    boolean updated = dataManager.updateStockByLocation(shopData.getLocation(), stock);
                    if (!updated) {
                        // 若缓存未命中（极少数情况），退而更新本地引用
                        shopData.setStock(stock);
                    }
                    totalShopsScanned.incrementAndGet();
                }
            } catch (Exception e) {
                logger.warning("读取商店库存失败 " + shopData.getFormattedLocation() + ": " + e.getMessage());
            }
        }
        // 若区块是扫描器主动加载的，读完后立即卸载以节省内存
        if (wasNewLoad && !group.world().isChunkLoaded(group.chunkX(), group.chunkZ())) {
            return; // 已被其他原因卸载，无需处理
        }
        if (wasNewLoad) {
            try {
                group.world().getChunkAt(group.chunkX(), group.chunkZ()).unload(true);
            } catch (Exception ignored) { /* 卸载失败不影响数据 */ }
        }
    }

    /**
     * 扫描全部完成时的处理：输出统计日志，异步保存 JSON，执行回调。
     */
    private void onScanComplete() {
        long elapsed = System.currentTimeMillis() - scanStartTime;
        logger.info(String.format("库存扫描完成！共扫描 %d 家商店，耗时 %dms，正在异步保存数据...",
                totalShopsScanned.get(), elapsed));

        // 异步保存 JSON，避免阻塞主线程
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dataManager.saveDataNow();
            logger.info("库存数据已持久化到 shops.json。");
        });

        if (onCompleteCallback != null) {
            onCompleteCallback.run();
        }
    }

    /**
     * 通过检查 region 文件存在性判断区块是否已生成，避免向未生成区块发起加载。
     * 与 FinderTools 的 {@code ChunkDataAccess.chunkExistsOnDisk} 思路一致。
     *
     * @param world  世界
     * @param chunkX 区块 X
     * @param chunkZ 区块 Z
     * @return 对应 .mca region 文件存在时返回 {@code true}
     */
    private boolean isChunkGenerated(World world, int chunkX, int chunkZ) {
        int rx = chunkX >> 5;
        int rz = chunkZ >> 5;
        File regionFile = new File(new File(world.getWorldFolder(), "region"),
                "r." + rx + "." + rz + ".mca");
        return regionFile.exists();
    }

    /**
     * 停止扫描（插件卸载时调用）。
     */
    public void stop() {
        if (tickerTask != null && !tickerTask.isCancelled()) {
            tickerTask.cancel();
        }
        queue.clear();
    }

    /**
     * 当前扫描是否仍在进行中。
     *
     * @return {@code true} 表示仍有待处理的区块
     */
    public boolean isRunning() {
        return tickerTask != null && !tickerTask.isCancelled()
                && (!queue.isEmpty() || pendingAsync.get() > 0);
    }
}
