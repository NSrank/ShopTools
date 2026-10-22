package org.plugin.shoptools.data;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.plugin.shoptools.util.DirectionUtil;

import java.util.UUID;

/**
 * 位置点数据类
 * 用于存储管理员创建的位置标记点
 */
public class LocationPoint {
    
    private final String id;
    private final String name;
    private final String keyword;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final long createdTime;
    private final String createdBy;
    
    /**
     * 构造函数
     * 
     * @param id 唯一ID
     * @param name 点位名称
     * @param keyword 关键字
     * @param worldName 世界名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param createdBy 创建者
     */
    public LocationPoint(String id, String name, String keyword, String worldName, 
                        double x, double y, double z, String createdBy) {
        this.id = id;
        this.name = name;
        this.keyword = keyword;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdTime = System.currentTimeMillis();
        this.createdBy = createdBy;
    }
    
    /**
     * 从现有数据构造（用于反序列化）
     */
    public LocationPoint(String id, String name, String keyword, String worldName, 
                        double x, double y, double z, long createdTime, String createdBy) {
        this.id = id;
        this.name = name;
        this.keyword = keyword;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdTime = createdTime;
        this.createdBy = createdBy;
    }
    
    /**
     * 生成随机ID
     * 
     * @return 随机ID字符串
     */
    public static String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * 获取Bukkit Location对象
     * 
     * @return Location对象，如果世界不存在则返回null
     */
    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }
    
    /**
     * 计算与指定位置的距离
     * 
     * @param location 目标位置
     * @return 距离，如果不在同一世界返回Double.MAX_VALUE
     */
    public double getDistance(Location location) {
        if (location == null || location.getWorld() == null || 
            !location.getWorld().getName().equals(worldName)) {
            return Double.MAX_VALUE;
        }
        
        Location pointLocation = getLocation();
        if (pointLocation == null) {
            return Double.MAX_VALUE;
        }
        
        return location.distance(pointLocation);
    }
    
    /**
     * 获取格式化的距离文本
     *
     * @param playerLocation 玩家位置
     * @return 格式化的距离文本
     */
    public String getFormattedDistance(Location playerLocation) {
        if (playerLocation == null || playerLocation.getWorld() == null ||
            !playerLocation.getWorld().getName().equals(worldName)) {
            return "otherworld";
        }

        double distance = getDistance(playerLocation);
        if (distance == Double.MAX_VALUE) {
            return "otherworld";
        }

        Location targetLocation = getLocation();
        if (targetLocation == null) {
            return "unknown";
        }

        // 使用DirectionUtil格式化带方向的距离
        return DirectionUtil.formatDistanceWithDirection(playerLocation, targetLocation, distance);
    }
    
    /**
     * 获取格式化的坐标文本
     * 
     * @return 格式化的坐标
     */
    public String getFormattedLocation() {
        return String.format("%s (%.0f, %.0f, %.0f)", worldName, x, y, z);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getKeyword() { return keyword; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public long getCreatedTime() { return createdTime; }
    public String getCreatedBy() { return createdBy; }
    
    @Override
    public String toString() {
        return String.format("LocationPoint{id='%s', name='%s', keyword='%s', location='%s'}", 
                           id, name, keyword, getFormattedLocation());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LocationPoint that = (LocationPoint) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
