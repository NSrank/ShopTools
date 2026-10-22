package org.plugin.shoptools.spatial;

/**
 * 八叉树统计信息
 * 
 * @author ShopTools
 * @since 1.1.1
 */
public class OctreeStats {
    private final int totalNodes;
    private final int totalItems;
    private final int maxDepth;
    
    /**
     * 构造函数
     * 
     * @param totalNodes 总节点数
     * @param totalItems 总项目数
     * @param maxDepth 最大深度
     */
    public OctreeStats(int totalNodes, int totalItems, int maxDepth) {
        this.totalNodes = totalNodes;
        this.totalItems = totalItems;
        this.maxDepth = maxDepth;
    }
    
    /**
     * 获取总节点数
     * 
     * @return 总节点数
     */
    public int getTotalNodes() {
        return totalNodes;
    }
    
    /**
     * 获取总项目数
     * 
     * @return 总项目数
     */
    public int getTotalItems() {
        return totalItems;
    }
    
    /**
     * 获取最大深度
     * 
     * @return 最大深度
     */
    public int getMaxDepth() {
        return maxDepth;
    }
    
    @Override
    public String toString() {
        return String.format("OctreeStats{nodes=%d, items=%d, maxDepth=%d}", 
                           totalNodes, totalItems, maxDepth);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OctreeStats that = (OctreeStats) obj;
        return totalNodes == that.totalNodes && 
               totalItems == that.totalItems && 
               maxDepth == that.maxDepth;
    }
    
    @Override
    public int hashCode() {
        int result = totalNodes;
        result = 31 * result + totalItems;
        result = 31 * result + maxDepth;
        return result;
    }
}
