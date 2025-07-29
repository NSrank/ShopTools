package org.plugin.shoptools.spatial;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 八叉树空间索引实现
 * 
 * 用于高效的3D空间查询和碰撞检测。
 * 线程安全的实现，支持并发读写操作。
 * 
 * @author ShopTools
 * @since 1.1.1
 */
public class Octree implements AutoCloseable {
    private final Range3D boundary;
    private final int maxDepth;
    private final int maxItems;
    private final int depth;
    
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Range3D, Object> items = new ConcurrentHashMap<>();
    private Octree[] children = null;
    private volatile boolean closed = false;
    
    /**
     * 构造函数
     * 
     * @param boundary 八叉树的边界范围
     * @param maxDepth 最大深度，防止无限递归
     * @param maxItems 每个节点最大项目数，超过时分割
     */
    public Octree(Range3D boundary, int maxDepth, int maxItems) {
        this(boundary, maxDepth, maxItems, 0);
    }
    
    /**
     * 内部构造函数，用于创建子节点
     */
    private Octree(Range3D boundary, int maxDepth, int maxItems, int depth) {
        this.boundary = boundary;
        this.maxDepth = maxDepth;
        this.maxItems = maxItems;
        this.depth = depth;
    }
    
    /**
     * 插入范围到八叉树
     * 
     * @param range 要插入的范围
     * @param data 关联的数据（可选）
     * @return 是否成功插入
     */
    public boolean insert(Range3D range, Object data) {
        if (closed) return false;
        
        lock.writeLock().lock();
        try {
            // 检查范围是否在边界内
            if (!boundary.intersects(range)) {
                return false;
            }
            
            // 如果有子节点，尝试插入到子节点
            if (children != null) {
                for (Octree child : children) {
                    if (child.boundary.intersects(range)) {
                        if (child.insert(range, data)) {
                            return true;
                        }
                    }
                }
                return false;
            }
            
            // 添加到当前节点
            items.put(range, data != null ? data : range);
            
            // 检查是否需要分割
            if (items.size() > maxItems && depth < maxDepth) {
                subdivide();
            }
            
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 移除范围
     * 
     * @param range 要移除的范围
     * @return 是否成功移除
     */
    public boolean remove(Range3D range) {
        if (closed) return false;
        
        lock.writeLock().lock();
        try {
            // 先尝试从当前节点移除
            if (items.remove(range) != null) {
                return true;
            }
            
            // 尝试从子节点移除
            if (children != null) {
                for (Octree child : children) {
                    if (child.boundary.intersects(range)) {
                        if (child.remove(range)) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 查询包含指定点的第一个范围
     * 
     * @param point 查询点
     * @return 包含该点的第一个范围，如果没有则返回null
     */
    public Range3D firstRange(Point3D point) {
        if (closed) return null;
        
        lock.readLock().lock();
        try {
            // 检查点是否在边界内
            if (!boundary.contains(point)) {
                return null;
            }
            
            // 先检查当前节点的项目
            for (Range3D range : items.keySet()) {
                if (range.contains(point)) {
                    return range;
                }
            }
            
            // 检查子节点
            if (children != null) {
                for (Octree child : children) {
                    if (child.boundary.contains(point)) {
                        Range3D result = child.firstRange(point);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
            
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 查询包含指定点的所有范围
     * 
     * @param point 查询点
     * @return 包含该点的所有范围列表
     */
    public List<Range3D> queryRanges(Point3D point) {
        if (closed) return Collections.emptyList();
        
        lock.readLock().lock();
        try {
            List<Range3D> result = new ArrayList<>();
            
            // 检查点是否在边界内
            if (!boundary.contains(point)) {
                return result;
            }
            
            // 检查当前节点的项目
            for (Range3D range : items.keySet()) {
                if (range.contains(point)) {
                    result.add(range);
                }
            }
            
            // 检查子节点
            if (children != null) {
                for (Octree child : children) {
                    if (child.boundary.contains(point)) {
                        result.addAll(child.queryRanges(point));
                    }
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 查询与指定范围相交的所有范围
     * 
     * @param queryRange 查询范围
     * @return 相交的所有范围列表
     */
    public List<Range3D> queryIntersecting(Range3D queryRange) {
        if (closed) return Collections.emptyList();
        
        lock.readLock().lock();
        try {
            List<Range3D> result = new ArrayList<>();
            
            // 检查查询范围是否与边界相交
            if (!boundary.intersects(queryRange)) {
                return result;
            }
            
            // 检查当前节点的项目
            for (Range3D range : items.keySet()) {
                if (range.intersects(queryRange)) {
                    result.add(range);
                }
            }
            
            // 检查子节点
            if (children != null) {
                for (Octree child : children) {
                    if (child.boundary.intersects(queryRange)) {
                        result.addAll(child.queryIntersecting(queryRange));
                    }
                }
            }
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 分割当前节点为8个子节点
     */
    private void subdivide() {
        if (children != null || depth >= maxDepth) return;

        List<Range3D> subRanges = boundary.subdivide();

        // 如果无法分割（范围太小），则不创建子节点
        if (subRanges.size() == 1 && subRanges.get(0).equals(boundary)) {
            return;
        }

        // 创建子节点数组，大小为实际子范围数量
        children = new Octree[subRanges.size()];
        for (int i = 0; i < subRanges.size(); i++) {
            children[i] = new Octree(subRanges.get(i), maxDepth, maxItems, depth + 1);
        }
        
        // 将当前项目重新分配到子节点
        Map<Range3D, Object> currentItems = new HashMap<>(items);
        items.clear();
        
        for (Map.Entry<Range3D, Object> entry : currentItems.entrySet()) {
            Range3D range = entry.getKey();
            Object data = entry.getValue();
            boolean inserted = false;
            
            for (Octree child : children) {
                if (child.boundary.intersects(range)) {
                    if (child.insert(range, data)) {
                        inserted = true;
                        break;
                    }
                }
            }
            
            // 如果无法插入到子节点，保留在当前节点
            if (!inserted) {
                items.put(range, data);
            }
        }
    }
    
    /**
     * 获取统计信息
     * 
     * @return 八叉树统计信息
     */
    public OctreeStats getStats() {
        lock.readLock().lock();
        try {
            int totalNodes = 1;
            int totalItems = items.size();
            int maxDepthReached = depth;
            
            if (children != null) {
                for (Octree child : children) {
                    OctreeStats childStats = child.getStats();
                    totalNodes += childStats.getTotalNodes();
                    totalItems += childStats.getTotalItems();
                    maxDepthReached = Math.max(maxDepthReached, childStats.getMaxDepth());
                }
            }
            
            return new OctreeStats(totalNodes, totalItems, maxDepthReached);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 清空八叉树
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            items.clear();
            if (children != null) {
                for (Octree child : children) {
                    child.close();
                }
                children = null;
            }
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
     * 检查八叉树是否已关闭
     * 
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }
}
