package org.plugin.shoptools.spatial;

import org.bukkit.Location;
import org.bukkit.World;
import org.plugin.shoptools.data.LocationPoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 位置点空间索引管理器
 * 
 * 使用八叉树提供高效的位置点空间查询功能
 * 支持按世界分离的空间索引，线程安全
 * 
 * @author ShopTools
 * @since 1.1.1
 */
public class LocationSpatialIndex implements AutoCloseable {
    
    // 每个世界的八叉树索引
    private final Map<String, Octree> worldOctrees = new ConcurrentHashMap<>();
    
    // 位置点数据存储
    private final Map<String, LocationPoint> locationPoints = new ConcurrentHashMap<>();
    
    // 读写锁
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // 八叉树配置
    private static final int MAX_DEPTH = 10;
    private static final int MAX_ITEMS_PER_NODE = 16;
    private static final int WORLD_SIZE = 60000000; // 世界边界大小
    
    private volatile boolean closed = false;
    
    /**
     * 添加位置点到空间索引
     * 
     * @param locationPoint 位置点
     * @return 是否成功添加
     */
    public boolean addLocation(LocationPoint locationPoint) {
        if (closed) return false;
        
        lock.writeLock().lock();
        try {
            String worldName = locationPoint.getWorldName();
            
            // 获取或创建世界的八叉树
            Octree octree = worldOctrees.computeIfAbsent(worldName, this::createWorldOctree);
            
            // 创建位置点的范围（单点范围）
            Point3D point = new Point3D(
                (int) locationPoint.getX(),
                (int) locationPoint.getY(),
                (int) locationPoint.getZ()
            );
            Range3D range = new Range3D(point, point);
            
            // 插入到八叉树
            boolean inserted = octree.insert(range, locationPoint);
            if (inserted) {
                locationPoints.put(locationPoint.getId(), locationPoint);
            }
            
            return inserted;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 从空间索引中移除位置点
     * 
     * @param locationPoint 位置点
     * @return 是否成功移除
     */
    public boolean removeLocation(LocationPoint locationPoint) {
        if (closed) return false;
        
        lock.writeLock().lock();
        try {
            String worldName = locationPoint.getWorldName();
            Octree octree = worldOctrees.get(worldName);
            
            if (octree == null) return false;
            
            // 创建位置点的范围
            Point3D point = new Point3D(
                (int) locationPoint.getX(),
                (int) locationPoint.getY(),
                (int) locationPoint.getZ()
            );
            Range3D range = new Range3D(point, point);
            
            // 从八叉树移除
            boolean removed = octree.remove(range);
            if (removed) {
                locationPoints.remove(locationPoint.getId());
            }
            
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 查找指定范围内的位置点
     * 
     * @param center 中心位置
     * @param radius 搜索半径
     * @return 范围内的位置点列表，按距离排序
     */
    public List<LocationPoint> findNearbyLocations(Location center, double radius) {
        if (closed) return Collections.emptyList();
        
        lock.readLock().lock();
        try {
            String worldName = center.getWorld().getName();
            Octree octree = worldOctrees.get(worldName);
            
            if (octree == null) return Collections.emptyList();
            
            // 创建搜索范围
            int radiusInt = (int) Math.ceil(radius);
            Point3D centerPoint = new Point3D(center);
            Range3D searchRange = new Range3D(
                centerPoint.getX() - radiusInt,
                centerPoint.getY() - radiusInt,
                centerPoint.getZ() - radiusInt,
                centerPoint.getX() + radiusInt,
                centerPoint.getY() + radiusInt,
                centerPoint.getZ() + radiusInt
            );
            
            // 查询相交的范围
            List<Range3D> intersectingRanges = octree.queryIntersecting(searchRange);
            
            // 收集位置点并计算距离
            List<LocationPoint> result = new ArrayList<>();
            for (Range3D range : intersectingRanges) {
                // 从范围中心点找到对应的位置点
                Point3D rangeCenter = range.getCenter();
                LocationPoint locationPoint = findLocationPointAt(worldName, rangeCenter);
                
                if (locationPoint != null) {
                    double distance = locationPoint.getDistance(center);
                    if (distance <= radius) {
                        result.add(locationPoint);
                    }
                }
            }
            
            // 按距离排序
            result.sort((a, b) -> Double.compare(
                a.getDistance(center),
                b.getDistance(center)
            ));
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 查找指定关键字的位置点（在指定世界内）
     * 
     * @param keyword 关键字
     * @param worldName 世界名称
     * @param playerLocation 玩家位置（用于距离排序）
     * @return 位置点列表，按距离排序
     */
    public List<LocationPoint> findLocationsByKeywordInWorld(String keyword, String worldName, Location playerLocation) {
        if (closed) return Collections.emptyList();
        
        lock.readLock().lock();
        try {
            List<LocationPoint> result = locationPoints.values().stream()
                .filter(point -> point.getKeyword().equalsIgnoreCase(keyword))
                .filter(point -> point.getWorldName().equals(worldName))
                .collect(Collectors.toList());
            
            // 如果有玩家位置，按距离排序
            if (playerLocation != null && playerLocation.getWorld().getName().equals(worldName)) {
                result.sort((a, b) -> Double.compare(
                    a.getDistance(playerLocation),
                    b.getDistance(playerLocation)
                ));
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有位置点
     * 
     * @return 所有位置点的集合
     */
    public Collection<LocationPoint> getAllLocations() {
        if (closed) return Collections.emptyList();
        
        lock.readLock().lock();
        try {
            return new ArrayList<>(locationPoints.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取指定世界的位置点数量
     * 
     * @param worldName 世界名称
     * @return 位置点数量
     */
    public int getLocationCountInWorld(String worldName) {
        if (closed) return 0;
        
        lock.readLock().lock();
        try {
            return (int) locationPoints.values().stream()
                .filter(point -> point.getWorldName().equals(worldName))
                .count();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取空间索引统计信息
     * 
     * @return 统计信息映射（世界名 -> 统计信息）
     */
    public Map<String, OctreeStats> getIndexStats() {
        if (closed) return Collections.emptyMap();
        
        lock.readLock().lock();
        try {
            Map<String, OctreeStats> stats = new HashMap<>();
            for (Map.Entry<String, Octree> entry : worldOctrees.entrySet()) {
                stats.put(entry.getKey(), entry.getValue().getStats());
            }
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 清空所有索引
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            for (Octree octree : worldOctrees.values()) {
                octree.close();
            }
            worldOctrees.clear();
            locationPoints.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void close() {
        if (closed) return;
        
        lock.writeLock().lock();
        try {
            closed = true;
            clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 检查是否已关闭
     * 
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * 为指定世界创建八叉树
     * 
     * @param worldName 世界名称
     * @return 八叉树实例
     */
    private Octree createWorldOctree(String worldName) {
        // 创建覆盖整个世界的边界范围
        Range3D worldBoundary = new Range3D(
            -WORLD_SIZE, -256, -WORLD_SIZE,
            WORLD_SIZE, 320, WORLD_SIZE
        );
        
        return new Octree(worldBoundary, MAX_DEPTH, MAX_ITEMS_PER_NODE);
    }
    
    /**
     * 在指定位置查找位置点
     * 
     * @param worldName 世界名称
     * @param point 点坐标
     * @return 位置点，如果没有则返回null
     */
    private LocationPoint findLocationPointAt(String worldName, Point3D point) {
        return locationPoints.values().stream()
            .filter(locationPoint -> locationPoint.getWorldName().equals(worldName))
            .filter(locationPoint -> 
                (int) locationPoint.getX() == point.getX() &&
                (int) locationPoint.getY() == point.getY() &&
                (int) locationPoint.getZ() == point.getZ()
            )
            .findFirst()
            .orElse(null);
    }
}
