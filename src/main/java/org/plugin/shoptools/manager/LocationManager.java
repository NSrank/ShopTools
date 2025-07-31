package org.plugin.shoptools.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.plugin.shoptools.ShopTools;
import org.plugin.shoptools.config.ConfigManager;
import org.plugin.shoptools.data.LocationPoint;
import org.plugin.shoptools.spatial.LocationSpatialIndex;
import org.plugin.shoptools.spatial.OctreeStats;
import org.plugin.shoptools.util.MessageUtil;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 位置点管理器
 * 负责位置点的创建、存储、查询和管理
 * 使用八叉树空间索引提供高效的空间查询功能
 */
public class LocationManager {

    private final ShopTools plugin;
    private final ConfigManager configManager;
    private final File dataFile;
    private final Gson gson;
    private final Map<String, LocationPoint> locationPoints;
    private final LocationSpatialIndex spatialIndex;
    
    public LocationManager(ShopTools plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataFile = new File(plugin.getDataFolder(), "locations.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.locationPoints = new HashMap<>();
        this.spatialIndex = new LocationSpatialIndex();

        loadLocationPoints();
    }
    
    /**
     * 创建新的位置点
     * 
     * @param sender 命令发送者
     * @param location 位置
     * @param name 点位名称
     * @param keyword 关键字
     * @return 是否创建成功
     */
    public boolean createLocationPoint(CommandSender sender, Location location, String name, String keyword) {
        if (location == null || location.getWorld() == null) {
            MessageUtil.sendMessage(sender, "&c无效的位置信息！");
            return false;
        }
        
        // 检查名称是否已存在
        if (isNameExists(name, keyword)) {
            MessageUtil.sendMessage(sender, "&c在关键字 '" + keyword + "' 下已存在名为 '" + name + "' 的点位！");
            return false;
        }
        
        // 生成唯一ID
        String id = LocationPoint.generateId();
        while (locationPoints.containsKey(id)) {
            id = LocationPoint.generateId();
        }
        
        // 创建位置点
        LocationPoint point = new LocationPoint(
            id, name, keyword,
            location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(),
            sender.getName()
        );

        locationPoints.put(id, point);
        spatialIndex.addLocation(point);
        saveLocationPoints();
        
        MessageUtil.sendMessage(sender, String.format(
            "&a成功创建位置点！\n" +
            "&7ID: &f%s\n" +
            "&7名称: &f%s\n" +
            "&7关键字: &f%s\n" +
            "&7位置: &f%s",
            id, name, keyword, point.getFormattedLocation()
        ));
        
        return true;
    }
    
    /**
     * 根据关键字查找位置点
     *
     * @param keyword 关键字
     * @param playerLocation 玩家位置（用于距离排序）
     * @return 位置点列表，按距离排序，同世界的在前，其他世界的在后
     */
    public List<LocationPoint> findLocationsByKeyword(String keyword, Location playerLocation) {
        List<LocationPoint> allMatches = locationPoints.values().stream()
            .filter(point -> point.getKeyword().equalsIgnoreCase(keyword))
            .collect(Collectors.toList());

        if (playerLocation == null) {
            // 没有玩家位置信息，按名称排序
            return allMatches.stream()
                .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                .collect(Collectors.toList());
        }

        String playerWorldName = playerLocation.getWorld().getName();

        // 分离同世界和其他世界的位置点
        List<LocationPoint> sameWorldPoints = new ArrayList<>();
        List<LocationPoint> otherWorldPoints = new ArrayList<>();

        for (LocationPoint point : allMatches) {
            if (point.getWorldName().equals(playerWorldName)) {
                sameWorldPoints.add(point);
            } else {
                otherWorldPoints.add(point);
            }
        }

        // 同世界的按距离排序
        sameWorldPoints.sort((p1, p2) -> {
            double dist1 = p1.getDistance(playerLocation);
            double dist2 = p2.getDistance(playerLocation);
            return Double.compare(dist1, dist2);
        });

        // 其他世界的按名称排序
        otherWorldPoints.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

        // 合并结果：同世界的在前，其他世界的在后
        List<LocationPoint> result = new ArrayList<>();
        result.addAll(sameWorldPoints);
        result.addAll(otherWorldPoints);

        return result;
    }
    
    /**
     * 获取所有关键字
     * 
     * @return 关键字列表
     */
    public List<String> getAllKeywords() {
        return locationPoints.values().stream()
            .map(LocationPoint::getKeyword)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.toList());
    }
    
    /**
     * 删除位置点
     *
     * @param id 位置点ID
     * @return 是否删除成功
     */
    public boolean removeLocationPoint(String id) {
        LocationPoint removed = locationPoints.remove(id);
        if (removed != null) {
            spatialIndex.removeLocation(removed);
            saveLocationPoints();
            return true;
        }
        return false;
    }
    
    /**
     * 根据名称和关键字删除位置点
     * 
     * @param name 点位名称
     * @param keyword 关键字
     * @return 是否删除成功
     */
    public boolean removeLocationPointByName(String name, String keyword) {
        LocationPoint toRemove = locationPoints.values().stream()
            .filter(point -> point.getName().equalsIgnoreCase(name) && 
                           point.getKeyword().equalsIgnoreCase(keyword))
            .findFirst()
            .orElse(null);
            
        if (toRemove != null) {
            locationPoints.remove(toRemove.getId());
            spatialIndex.removeLocation(toRemove);
            saveLocationPoints();
            return true;
        }
        return false;
    }
    
    /**
     * 检查名称是否已存在
     * 
     * @param name 点位名称
     * @param keyword 关键字
     * @return 是否存在
     */
    private boolean isNameExists(String name, String keyword) {
        return locationPoints.values().stream()
            .anyMatch(point -> point.getName().equalsIgnoreCase(name) && 
                             point.getKeyword().equalsIgnoreCase(keyword));
    }
    
    /**
     * 解析坐标字符串
     * 支持绝对坐标和相对坐标（~）
     * 
     * @param coordStr 坐标字符串 "x,y,z" 或 "~,~,~"
     * @param playerLocation 玩家当前位置
     * @return 解析后的Location，失败返回null
     */
    public Location parseCoordinates(String coordStr, Location playerLocation) {
        if (coordStr == null || coordStr.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = coordStr.split(",");
        if (parts.length != 3) {
            return null;
        }
        
        try {
            double x, y, z;
            
            // 解析X坐标
            if (parts[0].trim().equals("~")) {
                if (playerLocation == null) return null;
                x = playerLocation.getX();
            } else {
                x = Double.parseDouble(parts[0].trim());
            }
            
            // 解析Y坐标
            if (parts[1].trim().equals("~")) {
                if (playerLocation == null) return null;
                y = playerLocation.getY();
            } else {
                y = Double.parseDouble(parts[1].trim());
            }
            
            // 解析Z坐标
            if (parts[2].trim().equals("~")) {
                if (playerLocation == null) return null;
                z = playerLocation.getZ();
            } else {
                z = Double.parseDouble(parts[2].trim());
            }
            
            // 使用玩家当前世界
            if (playerLocation != null && playerLocation.getWorld() != null) {
                return new Location(playerLocation.getWorld(), x, y, z);
            }
            
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 加载位置点数据
     */
    private void loadLocationPoints() {
        if (!dataFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<String, LocationPoint>>(){}.getType();
            Map<String, LocationPoint> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                locationPoints.putAll(loaded);

                // 构建空间索引
                for (LocationPoint point : loaded.values()) {
                    spatialIndex.addLocation(point);
                }
            }
            plugin.getLogger().info("已加载 " + locationPoints.size() + " 个位置点到空间索引");
        } catch (IOException e) {
            plugin.getLogger().warning("加载位置点数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存位置点数据
     */
    private void saveLocationPoints() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(locationPoints, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存位置点数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取位置点总数
     * 
     * @return 位置点总数
     */
    public int getLocationPointCount() {
        return locationPoints.size();
    }
    
    /**
     * 获取指定关键字的位置点数量
     *
     * @param keyword 关键字
     * @return 位置点数量
     */
    public int getLocationPointCount(String keyword) {
        return (int) locationPoints.values().stream()
            .filter(point -> point.getKeyword().equalsIgnoreCase(keyword))
            .count();
    }

    /**
     * 查找指定范围内的位置点
     *
     * @param center 中心位置
     * @param radius 搜索半径
     * @return 范围内的位置点列表，按距离排序
     */
    public List<LocationPoint> findNearbyLocations(Location center, double radius) {
        return spatialIndex.findNearbyLocations(center, radius);
    }

    /**
     * 获取所有位置点
     *
     * @return 所有位置点的列表，按关键字和名称排序
     */
    public List<LocationPoint> getAllLocationPoints() {
        return locationPoints.values().stream()
            .sorted((p1, p2) -> {
                int keywordCompare = p1.getKeyword().compareToIgnoreCase(p2.getKeyword());
                if (keywordCompare != 0) {
                    return keywordCompare;
                }
                return p1.getName().compareToIgnoreCase(p2.getName());
            })
            .collect(Collectors.toList());
    }

    /**
     * 根据ID删除位置点
     *
     * @param sender 命令发送者
     * @param id 位置点ID
     * @return 是否删除成功
     */
    public boolean deleteLocationPoint(CommandSender sender, String id) {
        LocationPoint point = locationPoints.get(id);
        if (point == null) {
            MessageUtil.sendMessage(sender, "&c未找到ID为 '" + id + "' 的位置点！");
            return false;
        }

        // 从内存和空间索引中移除
        locationPoints.remove(id);
        spatialIndex.removeLocation(point);
        saveLocationPoints();

        MessageUtil.sendMessage(sender, String.format(
            "&a成功删除位置点！\n" +
            "&7ID: &f%s\n" +
            "&7名称: &f%s\n" +
            "&7关键字: &f%s\n" +
            "&7位置: &f%s",
            id, point.getName(), point.getKeyword(), point.getFormattedLocation()
        ));

        return true;
    }

    /**
     * 根据ID获取位置点
     *
     * @param id 位置点ID
     * @return 位置点，如果不存在则返回null
     */
    public LocationPoint getLocationPoint(String id) {
        return locationPoints.get(id);
    }

    /**
     * 获取空间索引统计信息
     *
     * @return 统计信息映射（世界名 -> 统计信息）
     */
    public Map<String, OctreeStats> getSpatialIndexStats() {
        return spatialIndex.getIndexStats();
    }

    /**
     * 清理资源
     */
    public void shutdown() {
        if (spatialIndex != null) {
            spatialIndex.close();
        }
    }
}
