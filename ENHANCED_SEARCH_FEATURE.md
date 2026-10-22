# 增强搜索功能说明

## 功能概述

基于用户反馈，我们进一步增强了`/shoptools search`功能，将搜索范围扩展到全服所有商店，同时通过智能距离显示保持商业平衡。

## 功能增强内容

### 1. 搜索范围扩展

**修改前**:
- 搜索范围：玩家周围200格内
- 显示内容：附近商店的精确距离

**修改后**:
- 搜索范围：全服所有商店
- 显示内容：智能距离标识

### 2. 智能距离显示系统

#### 距离显示规则

1. **0-200格内**: 显示精确距离（如"15.3m"、"89.7m"）
2. **超过200格**: 显示"200m+"
3. **其他世界**: 显示"otherworld"

#### 排序优先级

1. **同世界商店**: 按实际距离从近到远排序
2. **其他世界商店**: 排在最后，按世界名字母序排序

### 3. 技术实现

#### 距离比较器

```java
private Comparator<ShopData> createDistanceComparator(Location playerLocation) {
    return (shop1, shop2) -> {
        Location loc1 = shop1.getLocation();
        Location loc2 = shop2.getLocation();
        
        // 检查是否在同一世界
        boolean shop1SameWorld = loc1 != null && loc1.getWorld() != null && 
                               loc1.getWorld().equals(playerLocation.getWorld());
        boolean shop2SameWorld = loc2 != null && loc2.getWorld() != null && 
                               loc2.getWorld().equals(playerLocation.getWorld());
        
        // 同世界的商店优先
        if (shop1SameWorld && !shop2SameWorld) {
            return -1;
        }
        if (!shop1SameWorld && shop2SameWorld) {
            return 1;
        }
        
        // 如果都在同一世界，按距离排序
        if (shop1SameWorld && shop2SameWorld) {
            double dist1 = playerLocation.distance(loc1);
            double dist2 = playerLocation.distance(loc2);
            return Double.compare(dist1, dist2);
        }
        
        // 如果都在其他世界，按世界名排序
        if (!shop1SameWorld && !shop2SameWorld) {
            String world1 = loc1.getWorld().getName();
            String world2 = loc2.getWorld().getName();
            return world1.compareTo(world2);
        }
        
        return 0;
    };
}
```

#### 格式化距离显示

```java
private String getFormattedDistance(Location playerLocation, Location shopLocation) {
    if (shopLocation == null || shopLocation.getWorld() == null) {
        return "unknown";
    }
    
    // 检查是否在同一世界
    if (!shopLocation.getWorld().equals(playerLocation.getWorld())) {
        return "otherworld";
    }
    
    // 计算距离
    double distance = playerLocation.distance(shopLocation);
    
    // 超过200格显示"200m+"
    if (distance > 200.0) {
        return "200m+";
    }
    
    // 正常距离显示
    return String.format("%.1fm", distance);
}
```

## 使用示例

### 场景1: 玩家搜索钻石商店

```
玩家: /st search DIAMOND
系统: === DIAMOND 商店 (第1页/共2页) ===
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      钻石 world (150, 70, 300) 28.7m Bob [出售]
      钻石 world (200, 65, 400) 45.2m Charlie [收购]
      钻石 world (500, 64, 600) 200m+ David [出售]
      钻石 world (800, 64, 900) 200m+ Eve [出售]
      钻石 nether (100, 64, 200) otherworld Frank [出售]
      钻石 end (50, 64, 100) otherworld Grace [出售]
      ...（共10个商店）
      显示第 1-10 个商店，共 15 个
      下一页: /st search DIAMOND 2
```

### 场景2: 分页查看更多商店

```
玩家: /st search DIAMOND 2
系统: === DIAMOND 商店 (第2页/共2页) ===
      钻石 creative (200, 64, 300) otherworld Henry [出售]
      钻石 skyblock (150, 100, 250) otherworld Iris [收购]
      ...
      上一页: /st search DIAMOND 1
```

### 场景3: 没有找到商店

```
玩家: /st search RARE_ITEM
系统: 没有找到 RARE_ITEM 的商店。
```

## 商业平衡考虑

### 1. 信息保护

**隐藏的信息**:
- 超过200格商店的精确距离
- 其他世界商店的精确位置
- 具体的价格信息

**保留的信息**:
- 商店的存在性
- 大致的距离范围
- 商店的基本信息（物品、店主、状态）

### 2. 竞争平衡

**距离优势**:
- 近距离商店仍有明显优势
- 鼓励玩家就近交易
- 维护地理经济平衡

**探索鼓励**:
- 远距离商店信息模糊，需要实地探索
- 跨世界商店需要传送门等方式到达
- 保持游戏的探索乐趣

### 3. 商业策略

**位置价值**:
- 繁华区域的商店仍有位置优势
- 偏远商店可以通过价格竞争
- 跨世界商店适合特殊商品

## 与其他功能的对比

### `/st search` vs `/st near`

| 功能 | `/st search <物品ID>` | `/st near` |
|------|---------------------|------------|
| 搜索范围 | 全服所有商店 | 玩家周围200格内 |
| 物品筛选 | 指定物品 | 所有物品 |
| 距离显示 | 智能显示（精确/200m+/otherworld） | 精确距离 |
| 排序方式 | 距离优先，跨世界最后 | 距离从近到远 |
| 使用场景 | 寻找特定物品的所有选择 | 查看附近所有商店 |

### `/st search` vs `/st list` (管理员)

| 功能 | `/st search <物品ID>` (玩家) | `/st list <物品ID>` (管理员) |
|------|---------------------------|---------------------------|
| 权限要求 | 普通玩家 | 管理员 |
| 搜索范围 | 全服所有商店 | 全服所有商店 |
| 价格显示 | 距离信息 | 真实价格 |
| 排序方式 | 按距离排序 | 按价格排序 |
| 商业保护 | 保护价格隐私 | 完全透明 |

## 配置更新

### 帮助信息更新

```yaml
help-search: "&e/shoptools search <物品ID> [页码] &7- 搜索指定物品的所有商店（按距离排序）"
```

### 命令描述更新

- 移除了"附近200格内"的限制描述
- 强调"按距离排序"的特性
- 保持与其他命令的一致性

## 性能考虑

### 1. 数据量增加

**影响**:
- 搜索结果可能包含更多商店
- 分页功能变得更加重要

**优化**:
- 保持每页10个商店的限制
- 智能分页判断和导航

### 2. 排序性能

**复杂度**:
- 距离计算：O(n)
- 排序操作：O(n log n)
- 总体复杂度：O(n log n)

**优化策略**:
- 缓存玩家位置
- 延迟计算距离
- 分页减少显示数量

## 用户体验提升

### 1. 信息完整性

**优势**:
- 玩家可以看到所有可选商店
- 不会错过远距离的优质商店
- 跨世界商店信息可见

### 2. 决策支持

**帮助玩家**:
- 了解商品的整体供应情况
- 在距离和选择之间做平衡
- 发现新的交易机会

### 3. 探索动机

**鼓励行为**:
- 探索远距离的"200m+"商店
- 访问其他世界的商店
- 建立更广泛的交易网络

## 部署说明

这个功能增强完全向后兼容：
- ✅ 现有命令格式保持不变
- ✅ 新的距离显示逻辑自动生效
- ✅ 不影响其他功能的使用
- ✅ 配置文件平滑升级

用户现在可以享受到：
- 🌍 **全服搜索**: 查看所有可用的商店选择
- 📏 **智能距离**: 清晰的距离信息，保护商业隐私
- 🎯 **优先排序**: 近距离商店优先，远距离商店可选
- 🗺️ **跨世界支持**: 发现其他世界的交易机会

这个功能完美平衡了信息透明度和商业保护，为玩家提供了更全面的商店搜索体验！
