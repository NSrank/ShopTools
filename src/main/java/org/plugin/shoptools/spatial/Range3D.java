package org.plugin.shoptools.spatial;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 3D范围
 *
 * 表示三维空间中的一个立方体区域，用于空间索引和碰撞检测
 *
 * @author ShopTools
 * @since 1.1.1
 */
public class Range3D {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    
    /**
     * 构造函数
     * 
     * @param minX 最小X坐标
     * @param minY 最小Y坐标
     * @param minZ 最小Z坐标
     * @param maxX 最大X坐标
     * @param maxY 最大Y坐标
     * @param maxZ 最大Z坐标
     */
    public Range3D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (minX > maxX) throw new IllegalArgumentException("minX (" + minX + ") must be <= maxX (" + maxX + ")");
        if (minY > maxY) throw new IllegalArgumentException("minY (" + minY + ") must be <= maxY (" + maxY + ")");
        if (minZ > maxZ) throw new IllegalArgumentException("minZ (" + minZ + ") must be <= maxZ (" + maxZ + ")");
        
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
    
    /**
     * 从两个点创建Range3D
     * 
     * @param point1 第一个点
     * @param point2 第二个点
     */
    public Range3D(Point3D point1, Point3D point2) {
        this(
            Math.min(point1.getX(), point2.getX()),
            Math.min(point1.getY(), point2.getY()),
            Math.min(point1.getZ(), point2.getZ()),
            Math.max(point1.getX(), point2.getX()),
            Math.max(point1.getY(), point2.getY()),
            Math.max(point1.getZ(), point2.getZ())
        );
    }
    
    /**
     * 从两个Location创建Range3D
     * 
     * @param loc1 第一个位置
     * @param loc2 第二个位置
     */
    public Range3D(Location loc1, Location loc2) {
        this(new Point3D(loc1), new Point3D(loc2));
    }
    
    /**
     * 获取范围的中心点
     * 
     * @return 中心点
     */
    public Point3D getCenter() {
        return new Point3D(
            (minX + maxX) / 2,
            (minY + maxY) / 2,
            (minZ + maxZ) / 2
        );
    }
    
    /**
     * 获取范围的体积
     * 
     * @return 体积
     */
    public long getVolume() {
        return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
    
    /**
     * 检查点是否在范围内
     * 
     * @param point 点
     * @return 是否在范围内
     */
    public boolean contains(Point3D point) {
        return point.getX() >= minX && point.getX() <= maxX &&
               point.getY() >= minY && point.getY() <= maxY &&
               point.getZ() >= minZ && point.getZ() <= maxZ;
    }
    
    /**
     * 检查Location是否在范围内
     * 
     * @param location 位置
     * @return 是否在范围内
     */
    public boolean contains(Location location) {
        return contains(new Point3D(location));
    }
    
    /**
     * 检查两个范围是否相交
     * 
     * @param other 另一个范围
     * @return 是否相交
     */
    public boolean intersects(Range3D other) {
        return !(maxX < other.minX || minX > other.maxX ||
                maxY < other.minY || minY > other.maxY ||
                maxZ < other.minZ || minZ > other.maxZ);
    }
    
    /**
     * 检查当前范围是否完全包含另一个范围
     * 
     * @param other 另一个范围
     * @return 是否完全包含
     */
    public boolean contains(Range3D other) {
        return minX <= other.minX && maxX >= other.maxX &&
               minY <= other.minY && maxY >= other.maxY &&
               minZ <= other.minZ && maxZ >= other.maxZ;
    }
    
    /**
     * 扩展范围
     * 
     * @param amount 扩展量
     * @return 扩展后的范围
     */
    public Range3D expand(int amount) {
        return new Range3D(
            minX - amount, minY - amount, minZ - amount,
            maxX + amount, maxY + amount, maxZ + amount
        );
    }
    
    /**
     * 分割为8个子范围（用于八叉树）
     *
     * @return 8个子范围的列表
     */
    public List<Range3D> subdivide() {
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;

        List<Range3D> subRanges = new ArrayList<>();

        // 只有当范围足够大时才进行分割
        if (maxX - minX < 1 || maxY - minY < 1 || maxZ - minZ < 1) {
            // 范围太小，无法分割，返回自身
            subRanges.add(new Range3D(minX, minY, minZ, maxX, maxY, maxZ));
            return subRanges;
        }

        // 计算安全的分割点
        int nextCenterX = Math.min(centerX + 1, maxX);
        int nextCenterY = Math.min(centerY + 1, maxY);
        int nextCenterZ = Math.min(centerZ + 1, maxZ);

        // 创建8个子范围，确保边界有效
        try {
            // 左下前
            if (centerX >= minX && centerY >= minY && centerZ >= minZ) {
                subRanges.add(new Range3D(minX, minY, minZ, centerX, centerY, centerZ));
            }
            // 右下前
            if (nextCenterX <= maxX && centerY >= minY && centerZ >= minZ) {
                subRanges.add(new Range3D(nextCenterX, minY, minZ, maxX, centerY, centerZ));
            }
            // 左上前
            if (centerX >= minX && nextCenterY <= maxY && centerZ >= minZ) {
                subRanges.add(new Range3D(minX, nextCenterY, minZ, centerX, maxY, centerZ));
            }
            // 右上前
            if (nextCenterX <= maxX && nextCenterY <= maxY && centerZ >= minZ) {
                subRanges.add(new Range3D(nextCenterX, nextCenterY, minZ, maxX, maxY, centerZ));
            }
            // 左下后
            if (centerX >= minX && centerY >= minY && nextCenterZ <= maxZ) {
                subRanges.add(new Range3D(minX, minY, nextCenterZ, centerX, centerY, maxZ));
            }
            // 右下后
            if (nextCenterX <= maxX && centerY >= minY && nextCenterZ <= maxZ) {
                subRanges.add(new Range3D(nextCenterX, minY, nextCenterZ, maxX, centerY, maxZ));
            }
            // 左上后
            if (centerX >= minX && nextCenterY <= maxY && nextCenterZ <= maxZ) {
                subRanges.add(new Range3D(minX, nextCenterY, nextCenterZ, centerX, maxY, maxZ));
            }
            // 右上后
            if (nextCenterX <= maxX && nextCenterY <= maxY && nextCenterZ <= maxZ) {
                subRanges.add(new Range3D(nextCenterX, nextCenterY, nextCenterZ, maxX, maxY, maxZ));
            }
        } catch (IllegalArgumentException e) {
            // 如果仍然出现无效范围，返回自身
            subRanges.clear();
            subRanges.add(new Range3D(minX, minY, minZ, maxX, maxY, maxZ));
        }

        // 如果没有有效的子范围，返回自身
        if (subRanges.isEmpty()) {
            subRanges.add(new Range3D(minX, minY, minZ, maxX, maxY, maxZ));
        }

        return subRanges;
    }
    
    // Getters
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Range3D range3D = (Range3D) obj;
        return minX == range3D.minX && minY == range3D.minY && minZ == range3D.minZ &&
               maxX == range3D.maxX && maxY == range3D.maxY && maxZ == range3D.maxZ;
    }
    
    @Override
    public int hashCode() {
        int result = minX;
        result = 31 * result + minY;
        result = 31 * result + minZ;
        result = 31 * result + maxX;
        result = 31 * result + maxY;
        result = 31 * result + maxZ;
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("Range3D((%d,%d,%d) to (%d,%d,%d))", minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * 从两个点创建Range3D的静态方法
     *
     * @param point1 第一个点
     * @param point2 第二个点
     * @return Range3D实例
     */
    public static Range3D fromPoints(Point3D point1, Point3D point2) {
        return new Range3D(point1, point2);
    }

}
