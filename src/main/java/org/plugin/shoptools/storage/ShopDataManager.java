package org.plugin.shoptools.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.plugin.shoptools.config.ConfigManager;
import org.plugin.shoptools.model.ShopData;
import org.plugin.shoptools.spatial.LocationSpatialIndex;
import org.plugin.shoptools.data.LocationPoint;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 商店数据管理器
 * 负责商店数据的存储、缓存和检索
 * 
 * @author NSrank & Augment
 */
public class ShopDataManager {
    
    private final Logger logger;
    private final ConfigManager configManager;
    private final File dataFile;
    private final Gson gson;
    
    // 缓存系统
    private final Map<UUID, ShopData> shopCache = new ConcurrentHashMap<>();
    private final Map<String, List<ShopData>> itemCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<ShopData>> ownerCache = new ConcurrentHashMap<>();
    /** 按 "world:blockX:blockY:blockZ" 快速定位 ShopData，供 StockScanQueue 原地更新库存 */
    private final Map<String, ShopData> locationIndex = new ConcurrentHashMap<>();

    // 空间索引系统
    private final LocationSpatialIndex spatialIndex;

    private long lastUpdateTime = 0;
    private boolean isDataLoaded = false;
    
    /**
     * 构造函数
     * 
     * @param dataFolder 数据文件夹
     * @param configManager 配置管理器
     * @param logger 日志记录器
     */
    public ShopDataManager(File dataFolder, ConfigManager configManager, Logger logger) {
        this.logger = logger;
        this.configManager = configManager;
        this.dataFile = new File(dataFolder, "shops.json");

        // 初始化空间索引
        this.spatialIndex = new LocationSpatialIndex();

        // 创建Gson实例，使用简化的序列化策略
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        // 确保数据文件夹存在
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        // 注意：数据加载通过 loadDataAsync() 异步完成，不在构造函数中执行
    }
    
    /**
     * 更新商店数据（来自 DataSyncManager 的全量同步）。
     * <p>
     * 在清空缓存前，会先对已扫描的库存信息做快照；重建完成后自动恢复，
     * 确保 {@link org.plugin.shoptools.scan.StockScanQueue} 写入的库存数据
     * 不会被每次同步冲掉。
     *
     * @param shopDataList 从 QuickShop 获取的最新商店数据列表
     */
    public void updateShopData(List<ShopData> shopDataList) {
        if (shopDataList == null) {
            logger.warning("尝试更新空的商店数据列表！");
            return;
        }

        logger.info("开始更新商店数据，共 " + shopDataList.size() + " 个商店...");

        // ── 第一步：快照已扫描的库存数据，key = "world:x:y:z" ──────────────────
        // 由于 convertShopToShopData 无法在同步时读取库存（需加载区块），
        // 必须在替换缓存前将现有的已确认库存保存下来，供后续恢复。
        Map<String, Integer> stockSnapshot = new HashMap<>();
        for (ShopData existing : shopCache.values()) {
            if (existing.isStockKnown()) {
                String key = locationKey(existing.getLocation());
                if (key != null) {
                    stockSnapshot.put(key, existing.getStock());
                }
            }
        }
        if (!stockSnapshot.isEmpty()) {
            logger.info("已快照 " + stockSnapshot.size() + " 家商店的库存数据，将在同步后恢复。");
        }

        // ── 第二步：清空并重建缓存 ───────────────────────────────────────────────
        clearCache();

        for (ShopData shopData : shopDataList) {
            if (shopData != null) {
                // 主缓存
                shopCache.put(shopData.getShopId(), shopData);

                // 物品缓存
                String itemId = shopData.getItemId().toLowerCase();
                itemCache.computeIfAbsent(itemId, k -> new ArrayList<>()).add(shopData);

                // 店主缓存
                ownerCache.computeIfAbsent(shopData.getOwnerId(), k -> new ArrayList<>()).add(shopData);

                // 位置索引（供 StockScanQueue 原地更新）
                String locKey = locationKey(shopData.getLocation());
                if (locKey != null) {
                    locationIndex.put(locKey, shopData);
                }

                // 空间索引
                Location shopLocation = shopData.getLocation();
                if (shopLocation != null && shopLocation.getWorld() != null) {
                    LocationPoint tempPoint = new LocationPoint(
                        shopData.getShopId().toString(),
                        "shop_" + shopData.getItemId(),
                        shopData.getItemId(),
                        shopLocation.getWorld().getName(),
                        shopLocation.getX(),
                        shopLocation.getY(),
                        shopLocation.getZ(),
                        "system"
                    );
                    spatialIndex.addLocation(tempPoint);
                }

                // ── 第三步：恢复该商店的库存快照（如有）────────────────────────────
                if (locKey != null && stockSnapshot.containsKey(locKey)) {
                    shopData.setStock(stockSnapshot.get(locKey)); // 同时将 stockKnown 置为 true
                }
            }
        }

        // 保存到文件
        saveDataNow();

        this.lastUpdateTime = System.currentTimeMillis();
        this.isDataLoaded = true;

        logger.info("商店数据更新完成！缓存了 " + shopCache.size() + " 个商店（已恢复 "
                + stockSnapshot.size() + " 家已扫描库存）。");
    }
    
    /**
     * 获取所有商店数据
     *
     * @return 商店数据列表
     */
    public List<ShopData> getAllShops() {
        return new ArrayList<>(shopCache.values());
    }

    /**
     * 查找指定范围内的商店
     *
     * @param center 中心位置
     * @param radius 搜索半径
     * @return 范围内的商店列表，按距离排序
     */
    public List<ShopData> findNearbyShops(Location center, double radius) {
        if (center == null || center.getWorld() == null) {
            return new ArrayList<>();
        }

        // 使用空间索引查找附近的位置点
        List<LocationPoint> nearbyPoints = spatialIndex.findNearbyLocations(center, radius);

        // 将LocationPoint转换为ShopData
        List<ShopData> nearbyShops = new ArrayList<>();
        for (LocationPoint point : nearbyPoints) {
            try {
                UUID shopId = UUID.fromString(point.getId());
                ShopData shop = shopCache.get(shopId);
                if (shop != null) {
                    nearbyShops.add(shop);
                }
            } catch (IllegalArgumentException e) {
                // 忽略无效的UUID，可能是位置点而非商店
            }
        }

        return nearbyShops;
    }

    /**
     * 查找指定物品在指定范围内的商店
     *
     * @param itemId 物品ID
     * @param center 中心位置
     * @param radius 搜索半径
     * @return 范围内的商店列表，按距离排序
     */
    public List<ShopData> findNearbyShopsByItem(String itemId, Location center, double radius) {
        if (itemId == null || itemId.trim().isEmpty() || center == null || center.getWorld() == null) {
            return new ArrayList<>();
        }

        // 先获取所有该物品的商店
        List<ShopData> itemShops = getShopsByItem(itemId);

        // 过滤出在范围内的商店
        List<ShopData> nearbyShops = new ArrayList<>();
        for (ShopData shop : itemShops) {
            Location shopLocation = shop.getLocation();
            if (shopLocation != null && shopLocation.getWorld() != null &&
                shopLocation.getWorld().equals(center.getWorld())) {

                double distance = center.distance(shopLocation);
                if (distance <= radius) {
                    nearbyShops.add(shop);
                }
            }
        }

        // 按距离排序
        nearbyShops.sort((a, b) -> {
            Location locA = a.getLocation();
            Location locB = b.getLocation();
            if (locA == null || locB == null) return 0;

            double distA = center.distance(locA);
            double distB = center.distance(locB);
            return Double.compare(distA, distB);
        });

        return nearbyShops;
    }
    
    /**
     * 根据物品ID获取商店数据
     * 
     * @param itemId 物品ID
     * @return 商店数据列表
     */
    public List<ShopData> getShopsByItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedItemId = itemId.toLowerCase().trim();
        List<ShopData> shops = itemCache.get(normalizedItemId);
        
        if (shops == null) {
            // 尝试模糊匹配
            shops = itemCache.entrySet().stream()
                    .filter(entry -> entry.getKey().contains(normalizedItemId))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toList());
        }
        
        return shops != null ? new ArrayList<>(shops) : new ArrayList<>();
    }
    
    /**
     * 根据店主UUID获取商店数据
     * 
     * @param ownerId 店主UUID
     * @return 商店数据列表
     */
    public List<ShopData> getShopsByOwner(UUID ownerId) {
        if (ownerId == null) {
            return new ArrayList<>();
        }
        
        List<ShopData> shops = ownerCache.get(ownerId);
        return shops != null ? new ArrayList<>(shops) : new ArrayList<>();
    }
    
    /**
     * 根据店主名称获取商店数据
     * 
     * @param ownerName 店主名称
     * @return 商店数据列表
     */
    public List<ShopData> getShopsByOwnerName(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedName = ownerName.toLowerCase().trim();
        
        return shopCache.values().stream()
                .filter(shop -> shop.getOwnerName() != null && 
                               shop.getOwnerName().toLowerCase().contains(normalizedName))
                .collect(Collectors.toList());
    }
    
    /**
     * 清空所有缓存
     */
    private void clearCache() {
        shopCache.clear();
        itemCache.clear();
        ownerCache.clear();
        locationIndex.clear();

        // 清空空间索引
        spatialIndex.clear();
    }

    /**
     * 将 Location 转换为用于 locationIndex 的字符串键。
     * 使用方块坐标（整数），忽略 yaw/pitch，与 QuickShop 内部行为一致。
     *
     * @param loc 位置对象
     * @return 格式为 "world:blockX:blockY:blockZ" 的键；loc 无效时返回 {@code null}
     */
    private String locationKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
    
    /**
     * 将当前缓存的所有商店数据立即保存到 shops.json。
     * <p>
     * 可在主线程或异步线程调用，但建议在异步线程中调用以避免 I/O 阻塞主线程。
     * {@link org.plugin.shoptools.scan.StockScanQueue} 在扫描完成后会通过异步任务调用此方法。
     */
    public void saveDataNow() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            List<ShopData> shopList = new ArrayList<>(shopCache.values());
            List<SimpleShopData> simpleShopList = new ArrayList<>();

            // 转换为简化的数据对象
            for (ShopData shop : shopList) {
                simpleShopList.add(new SimpleShopData(shop));
            }

            gson.toJson(simpleShopList, writer);

            if (configManager.isDebugEnabled()) {
                logger.info("商店数据已保存到文件: " + dataFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.severe("保存商店数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 异步从文件加载商店数据（多线程加速，避免阻塞主线程）
     * <p>
     * 必须从主线程调用，内部会自动调度异步任务：
     * <ol>
     *   <li>主线程：捕获世界快照（避免在异步线程调用 Bukkit API）</li>
     *   <li>异步线程：文件 I/O + JSON 解析 + ForkJoinPool 并行对象转换</li>
     *   <li>主线程：重建内存缓存和空间索引，然后调用 {@code onLoaded} 回调</li>
     * </ol>
     *
     * @param plugin   插件实例，用于调度 Bukkit 任务
     * @param onLoaded 数据加载并缓存完成后在主线程执行的回调；为 {@code null} 时忽略
     */
    public void loadDataAsync(Plugin plugin, Runnable onLoaded) {
        if (!dataFile.exists()) {
            logger.info("商店数据文件不存在，将在首次同步后创建。");
            return;
        }

        // 在主线程捕获世界快照，避免在异步线程中调用 Bukkit API
        Map<String, World> worldSnapshot = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            worldSnapshot.put(world.getName(), world);
        }

        // 确定并行线程数
        int configThreads = configManager.getLoadThreads();
        int threadCount = (configThreads <= 0)
                ? Math.max(1, Runtime.getRuntime().availableProcessors() - 2)
                : configThreads;

        logger.info("开始异步加载商店数据，使用 " + threadCount + " 个线程...");
        final long startTime = System.currentTimeMillis();

        // 切换到异步线程执行文件 I/O 和数据解析
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (FileReader reader = new FileReader(dataFile)) {
                Type listType = new TypeToken<List<SimpleShopData>>(){}.getType();
                List<SimpleShopData> simpleShopList = gson.fromJson(reader, listType);

                if (simpleShopList == null || simpleShopList.isEmpty()) {
                    logger.info("商店数据文件为空。");
                    return;
                }

                logger.info("已解析 " + simpleShopList.size() + " 条记录，开始并行转换（" + threadCount + " 线程）...");

                // 使用 ForkJoinPool 并行将 SimpleShopData 转换为 ShopData
                ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);
                List<ShopData> shopList;
                try {
                    shopList = forkJoinPool.submit(() ->
                        simpleShopList.parallelStream()
                            .map(simpleShop -> {
                                try {
                                    // 使用世界快照，避免在异步线程调用 Bukkit API
                                    return simpleShop.toShopData(worldSnapshot);
                                } catch (Exception e) {
                                    logger.warning("转换商店数据时发生错误: " + e.getMessage());
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
                    ).get();
                } finally {
                    forkJoinPool.shutdown();
                }

                long parseTime = System.currentTimeMillis() - startTime;
                final List<ShopData> finalShopList = shopList;
                logger.info("并行转换完成，共 " + finalShopList.size() + " 个商店，耗时 " + parseTime + "ms，切回主线程重建缓存...");

                // 切回主线程重建内存缓存，保证线程安全
                Bukkit.getScheduler().runTask(plugin, () -> {
                    rebuildCachesOnly(finalShopList);
                    long totalTime = System.currentTimeMillis() - startTime;
                    logger.info("商店数据加载完成！共加载 " + shopCache.size() + " 个商店，总耗时 " + totalTime + "ms。");
                    // 通知调用方加载已完成（用于触发库存扫描等后续操作）
                    if (onLoaded != null) {
                        onLoaded.run();
                    }
                });

            } catch (IOException e) {
                logger.severe("加载商店数据时发生 I/O 错误: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                logger.severe("解析商店数据时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 仅从商店数据列表重建内存缓存，不写入文件
     * <p>
     * 用于从磁盘加载后填充缓存，避免不必要的写回操作。
     * 必须在主线程调用。
     *
     * @param shopList 商店数据列表
     */
    private void rebuildCachesOnly(List<ShopData> shopList) {
        // 清空旧缓存和空间索引
        clearCache();

        for (ShopData shopData : shopList) {
            if (shopData == null) continue;

            // 添加到主缓存
            shopCache.put(shopData.getShopId(), shopData);

            // 添加到物品缓存
            String itemId = shopData.getItemId();
            if (itemId != null) {
                itemCache.computeIfAbsent(itemId.toLowerCase(), k -> new ArrayList<>()).add(shopData);
            }

            // 添加到店主缓存
            ownerCache.computeIfAbsent(shopData.getOwnerId(), k -> new ArrayList<>()).add(shopData);

            // 添加到位置索引（供 StockScanQueue 原地更新库存）
            String locKey = locationKey(shopData.getLocation());
            if (locKey != null) {
                locationIndex.put(locKey, shopData);
            }

            // 添加到空间索引
            Location shopLocation = shopData.getLocation();
            if (shopLocation != null && shopLocation.getWorld() != null) {
                LocationPoint tempPoint = new LocationPoint(
                    shopData.getShopId().toString(),
                    "shop_" + shopData.getItemId(),
                    shopData.getItemId(),
                    shopLocation.getWorld().getName(),
                    shopLocation.getX(),
                    shopLocation.getY(),
                    shopLocation.getZ(),
                    "system"
                );
                spatialIndex.addLocation(tempPoint);
            }
        }

        this.lastUpdateTime = System.currentTimeMillis();
        this.isDataLoaded = true;
    }
    
    /**
     * 根据位置直接更新当前缓存中的商店库存。
     * <p>
     * 由 {@link org.plugin.shoptools.scan.StockScanQueue} 在区块加载后调用，确保更新的是
     * 当前缓存中的实际对象（而非扫描队列启动时持有的旧引用），彻底避免"孤儿对象"问题。
     * 必须在主线程调用（由 Paper 的 getChunkAtAsync 回调保证）。
     *
     * @param location 商店位置
     * @param stock    从 QuickShop API 读取到的实际库存数量
     * @return 如果找到并更新了缓存对象返回 {@code true}；位置未命中返回 {@code false}
     */
    public boolean updateStockByLocation(Location location, int stock) {
        String key = locationKey(location);
        if (key == null) return false;
        ShopData shopData = locationIndex.get(key);
        if (shopData != null) {
            shopData.setStock(stock); // 同时将 stockKnown 置为 true
            return true;
        }
        return false;
    }

    /**
     * 检查数据是否已加载
     *
     * @return 如果数据已加载返回true
     */
    public boolean isDataLoaded() {
        return isDataLoaded;
    }
    
    /**
     * 获取最后更新时间
     * 
     * @return 最后更新时间戳
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * 获取缓存的商店数量
     *
     * @return 商店数量
     */
    public int getShopCount() {
        return shopCache.size();
    }

    /**
     * 简化的商店数据类，用于JSON序列化
     */
    private static class SimpleShopData {
        public String shopId;
        public String itemId;
        public String itemDisplayName;
        public String worldName;
        public double x, y, z;
        public double price;
        public String ownerId;
        public String ownerName;
        public String shopType;
        public int stock;
        public boolean isUnlimited;

        public SimpleShopData() {
            // 默认构造函数，用于反序列化
        }

        public SimpleShopData(ShopData shopData) {
            this.shopId = shopData.getShopId().toString();
            this.itemId = shopData.getItemId();
            this.itemDisplayName = shopData.getItemDisplayName();

            Location loc = shopData.getLocation();
            if (loc != null && loc.getWorld() != null) {
                this.worldName = loc.getWorld().getName();
                this.x = loc.getX();
                this.y = loc.getY();
                this.z = loc.getZ();
            }

            this.price = shopData.getPrice();
            this.ownerId = shopData.getOwnerId().toString();
            this.ownerName = shopData.getOwnerName();
            this.shopType = shopData.getShopType().name();
            this.stock = shopData.getStock();
            this.isUnlimited = shopData.isUnlimited();
        }

        /**
         * 将简化数据转换为 ShopData（使用世界快照，适合在异步线程调用）
         *
         * @param worldMap 世界名称到 World 对象的映射快照（主线程提前采集）
         * @return 完整的 ShopData 对象
         */
        public ShopData toShopData(Map<String, World> worldMap) {
            UUID shopUUID = UUID.fromString(shopId);
            UUID ownerUUID = UUID.fromString(ownerId);

            Location location = null;
            if (worldName != null) {
                World world = worldMap.get(worldName);
                if (world != null) {
                    location = new Location(world, x, y, z);
                }
            }

            ShopData.ShopType type = ShopData.ShopType.valueOf(shopType);

            return new ShopData(
                shopUUID, itemId, itemDisplayName, location,
                price, ownerUUID, ownerName, type, stock, isUnlimited, null
            );
        }
    }


}
