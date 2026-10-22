package org.plugin.shoptools.spatial;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * 3D点坐标
 * 
 * 表示三维空间中的一个点，用于空间索引和碰撞检测
 * 
 * @author ShopTools
 * @since 1.1.1
 */
public class Point3D {
    private final int x;
    private final int y;
    private final int z;
    
    /**
     * 构造函数
     * 
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     */
    public Point3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * 从Bukkit Location创建Point3D
     * 
     * @param location Bukkit位置
     */
    public Point3D(Location location) {
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }
    
    /**
     * 转换为Bukkit Location
     * 
     * @param world 世界
     * @return Bukkit位置
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }
    
    /**
     * 计算到另一个点的距离平方
     * 
     * @param other 另一个点
     * @return 距离平方
     */
    public double distanceSquared(Point3D other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    /**
     * 计算到另一个点的距离
     * 
     * @param other 另一个点
     * @return 距离
     */
    public double distance(Point3D other) {
        return Math.sqrt(distanceSquared(other));
    }
    
    /**
     * 检查点是否在指定范围内
     * 
     * @param range 范围
     * @return 是否在范围内
     */
    public boolean isInRange(Range3D range) {
        return x >= range.getMinX() && x <= range.getMaxX() &&
               y >= range.getMinY() && y <= range.getMaxY() &&
               z >= range.getMinZ() && z <= range.getMaxZ();
    }
    
    /**
     * 偏移点坐标
     * 
     * @param dx X偏移
     * @param dy Y偏移
     * @param dz Z偏移
     * @return 新的点
     */
    public Point3D offset(int dx, int dy, int dz) {
        return new Point3D(x + dx, y + dy, z + dz);
    }
    
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point3D point3D = (Point3D) obj;
        return x == point3D.x && y == point3D.y && z == point3D.z;
    }
    
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("Point3D(%d, %d, %d)", x, y, z);
    }
}
