package org.plugin.shoptools.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.plugin.shoptools.config.ConfigManager;
import org.plugin.shoptools.model.ShopData;
import org.plugin.shoptools.spatial.LocationSpatialIndex;
import org.plugin.shoptools.spatial.OctreeStats;
import org.plugin.shoptools.data.LocationPoint;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

        // 加载现有数据
        loadData();
    }
    
    /**
     * 更新商店数据
     * 
     * @param shopDataList 新的商店数据列表
     */
    public void updateShopData(List<ShopData> shopDataList) {
        if (shopDataList == null) {
            logger.warning("尝试更新空的商店数据列表！");
            return;
        }
        
        logger.info("开始更新商店数据，共 " + shopDataList.size() + " 个商店...");
        
        // 清空缓存
        clearCache();
        
        // 更新缓存和空间索引
        for (ShopData shopData : shopDataList) {
            if (shopData != null) {
                // 添加到主缓存
                shopCache.put(shopData.getShopId(), shopData);

                // 添加到物品缓存
                String itemId = shopData.getItemId().toLowerCase();
                itemCache.computeIfAbsent(itemId, k -> new ArrayList<>()).add(shopData);

                // 添加到店主缓存
                ownerCache.computeIfAbsent(shopData.getOwnerId(), k -> new ArrayList<>()).add(shopData);

                // 添加到空间索引
                Location shopLocation = shopData.getLocation();
                if (shopLocation != null && shopLocation.getWorld() != null) {
                    // 创建一个临时的LocationPoint来利用空间索引
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
        }
        
        // 保存到文件
        saveData();
        
        this.lastUpdateTime = System.currentTimeMillis();
        this.isDataLoaded = true;
        
        logger.info("商店数据更新完成！缓存了 " + shopCache.size() + " 个商店。");
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

        // 清空空间索引
        spatialIndex.clear();
    }
    
    /**
     * 保存数据到文件
     */
    private void saveData() {
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
     * 从文件加载数据
     */
    private void loadData() {
        if (!dataFile.exists()) {
            logger.info("商店数据文件不存在，将在首次同步后创建。");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type listType = new TypeToken<List<SimpleShopData>>(){}.getType();
            List<SimpleShopData> simpleShopList = gson.fromJson(reader, listType);

            if (simpleShopList != null && !simpleShopList.isEmpty()) {
                List<ShopData> shopList = new ArrayList<>();

                // 转换为ShopData对象
                for (SimpleShopData simpleShop : simpleShopList) {
                    try {
                        ShopData shopData = simpleShop.toShopData();
                        shopList.add(shopData);
                    } catch (Exception e) {
                        logger.warning("转换商店数据时发生错误: " + e.getMessage());
                    }
                }

                updateShopData(shopList);
                logger.info("从文件加载了 " + shopList.size() + " 个商店数据。");
            } else {
                logger.info("商店数据文件为空。");
            }
        } catch (IOException e) {
            logger.severe("加载商店数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.severe("解析商店数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
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
        }

        public ShopData toShopData() {
            UUID shopUUID = UUID.fromString(shopId);
            UUID ownerUUID = UUID.fromString(ownerId);

            Location location = null;
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    location = new Location(world, x, y, z);
                }
            }

            ShopData.ShopType type = ShopData.ShopType.valueOf(shopType);

            return new ShopData(
                shopUUID,
                itemId,
                itemDisplayName,
                location,
                price,
                ownerUUID,
                ownerName,
                type,
                stock,
                null // ItemStack设为null，避免序列化问题
            );
        }
    }


}
