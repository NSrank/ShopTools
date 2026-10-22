# 附近商店搜索功能说明

## 功能概述

基于用户反馈，我们重新设计了权限系统并添加了两个新的玩家友好功能，既保护了商业信息隐私，又为玩家提供了实用的附近商店查找功能。

## 权限系统重构

### 管理员专用功能
以下功能现在仅限管理员使用（需要`shoptools.admin`权限）：
- `/shoptools page <页码>` - 分页显示所有商店
- `/shoptools list <物品ID> [页码]` - 显示指定物品的所有商店
- `/shoptools who <玩家名/UUID> [页码]` - 查看指定玩家的商店
- `/shoptools reload` - 重新加载配置

### 玩家可用功能
新增的玩家友好功能（所有玩家都可使用）：
- `/shoptools search <物品ID> [页码]` - 搜索附近200格内的指定物品商店
- `/shoptools near [页码]` - 查看附近200格内的所有商店
- `/shoptools help` - 显示帮助信息

## 新功能详细说明

### 1. `/shoptools search` - 附近物品搜索

**命令格式**:
```
/shoptools search <物品ID> [页码]
/st search DIAMOND        # 搜索附近的钻石商店
/st search DIAMOND 2      # 搜索附近的钻石商店第2页
```

**功能特性**:
- **搜索范围**: 以玩家为中心200格半径内
- **世界限制**: 只搜索玩家当前世界的商店
- **距离显示**: 价格位置显示为玩家与商店的距离（如"15.3m"）
- **排序方式**: 按距离从近到远排序
- **分页支持**: 超过10个商店时自动分页
- **智能补全**: 支持物品ID和页码的Tab补全

**示例输出**:
```
玩家: /st search DIAMOND
系统: === 附近的 DIAMOND 商店 ===
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      钻石 world (150, 70, 300) 28.7m Bob [出售]
      钻石 world (200, 65, 400) 45.2m Charlie [收购]
      === 共 3 个商店 ===
```

### 2. `/shoptools near` - 附近所有商店

**命令格式**:
```
/shoptools near [页码]
/st near                  # 查看附近所有商店
/st near 2                # 查看附近所有商店第2页
```

**功能特性**:
- **搜索范围**: 以玩家为中心200格半径内
- **世界限制**: 只搜索玩家当前世界的商店
- **距离显示**: 价格位置显示为玩家与商店的距离
- **排序方式**: 按距离从近到远排序
- **分页支持**: 超过10个商店时自动分页
- **智能补全**: 支持页码的Tab补全

**示例输出**:
```
玩家: /st near
系统: === 附近的商店 (第1页/共3页) ===
      苹果 world (105, 64, 205) 8.2m Alice [出售]
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      石头 world (120, 64, 220) 22.1m Bob [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 25 个
      下一页: /st near 2
```

## 技术实现细节

### 1. 距离计算算法

```java
private double getDistance(Location loc1, Location loc2) {
    if (loc1 == null || loc2 == null || !loc1.getWorld().equals(loc2.getWorld())) {
        return Double.MAX_VALUE;
    }
    
    return loc1.distance(loc2);
}
```

### 2. 附近商店搜索

```java
private List<ShopData> getNearbyShopsByItem(Player player, String itemId, double radius) {
    List<ShopData> allShops = dataManager.getShopsByItem(itemId);
    List<ShopData> nearbyShops = new ArrayList<>();
    
    Location playerLocation = player.getLocation();
    
    for (ShopData shop : allShops) {
        Location shopLocation = shop.getLocation();
        if (shopLocation != null && shopLocation.getWorld() != null && 
            shopLocation.getWorld().equals(playerLocation.getWorld())) {
            
            double distance = getDistance(playerLocation, shopLocation);
            if (distance <= radius) {
                nearbyShops.add(shop);
            }
        }
    }
    
    return nearbyShops;
}
```

### 3. 距离显示格式化

```java
double distance = getDistance(playerLocation, shop.getLocation());
String distanceText = String.format("%.1fm", distance);

String message = configManager.getMessage("shop-list-item")
        .replace("{item}", shop.getItemDisplayName())
        .replace("{location}", shop.getFormattedLocation())
        .replace("{price}", distanceText)  // 价格位置显示距离
        .replace("{owner}", ownerName)
        .replace("{status}", shopStatus);
```

### 4. 智能分页判断

```java
int pageSize = 10;
if (nearbyShops.size() <= pageSize) {
    // 商店数量少，直接显示所有
    displayNearbyShopList(sender, nearbyShops, title, false, player.getLocation());
} else {
    // 商店数量多，使用分页显示
    displayNearbyShopListPaged(sender, nearbyShops, title, page, player.getLocation());
}
```

## 权限保护机制

### 1. 商业信息保护

**隐藏的信息**:
- 全服商店的完整价格信息
- 其他玩家的商店详细信息
- 跨世界的商店信息

**保留的信息**:
- 附近商店的基本信息（物品、位置、店主、状态）
- 玩家与商店的距离信息
- 商店的可用性状态

### 2. 搜索范围限制

**200格半径限制**:
- 防止玩家获取过多商业信息
- 鼓励玩家探索和移动
- 保持游戏的地理平衡

**同世界限制**:
- 只显示当前世界的商店
- 防止跨世界商业信息泄露

## Tab补全增强

### 1. 物品ID补全

```
/st search <TAB>
```
- 显示所有可用的物品ID
- 基于现有商店的物品类型

### 2. 页码补全

```
/st search DIAMOND <TAB>
/st near <TAB>
```
- 根据实际搜索结果动态计算页码
- 只有超过10个商店时才显示页码选项

### 3. 实现代码

```java
if ("search".equals(subCommand)) {
    // 物品ID补全（玩家命令）
    Set<String> itemIds = dataManager.getAllShops().stream()
            .map(ShopData::getItemId)
            .collect(Collectors.toSet());
    completions.addAll(itemIds);
} else if ("near".equals(subCommand)) {
    // 页码补全（玩家命令）
    if (sender instanceof Player) {
        Player player = (Player) sender;
        List<ShopData> nearbyShops = getNearbyShops(player, 200.0);
        if (nearbyShops.size() > 10) {
            int totalPages = (int) Math.ceil((double) nearbyShops.size() / 10);
            for (int i = 1; i <= Math.min(totalPages, 10); i++) {
                completions.add(String.valueOf(i));
            }
        }
    }
}
```

## 使用示例

### 场景1: 玩家寻找附近的钻石商店

```
玩家: /st search DIAMOND
系统: === 附近的 DIAMOND 商店 ===
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      钻石 world (150, 70, 300) 28.7m Bob [出售]
      钻石 world (200, 65, 400) 45.2m Charlie [收购]
      === 共 3 个商店 ===
```

### 场景2: 玩家查看附近所有商店

```
玩家: /st near
系统: === 附近的商店 (第1页/共2页) ===
      苹果 world (105, 64, 205) 8.2m Alice [出售]
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      石头 world (120, 64, 220) 22.1m Bob [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 18 个
      下一页: /st near 2
```

### 场景3: 没有找到附近商店

```
玩家: /st search RARE_ITEM
系统: 附近200格内没有找到 RARE_ITEM 的商店。

玩家: /st near
系统: 附近200格内没有找到任何商店。
```

### 场景4: 管理员尝试使用受限功能

```
普通玩家: /st list DIAMOND
系统: 你没有权限执行此命令。

普通玩家: /st page 1
系统: 你没有权限执行此命令。
```

## 配置更新

### 新增消息配置

```yaml
# 玩家可用命令
help-search: "&e/shoptools search <物品ID> [页码] &7- 搜索附近200格内的指定物品商店"
help-near: "&e/shoptools near [页码] &7- 查看附近200格内的所有商店"

# 管理员命令标识
help-page: "&e/shoptools page <页码> &7- 分页显示所有商店 &c(管理员)"
help-list: "&e/shoptools list <物品ID> [页码] &7- 显示指定物品的商店 &c(管理员)"

# 错误信息
player-only: "&c此命令只能由玩家执行！"
```

## 部署说明

这个功能重构完全向后兼容：
- ✅ 现有管理员权限保持不变
- ✅ 新增玩家功能不影响现有系统
- ✅ 配置文件自动升级
- ✅ 权限系统平滑过渡

### 权限配置建议

```yaml
permissions:
  shoptools.use:
    description: 允许使用基础商店查询功能
    default: true
  shoptools.admin:
    description: 允许使用管理员商店功能
    default: op
```

## 平衡性考虑

### 1. 商业保护
- **价格隐私**: 不显示具体价格，避免价格战
- **范围限制**: 200格限制防止全服商业信息泄露
- **距离优先**: 鼓励就近交易，维护地理经济平衡

### 2. 游戏体验
- **探索鼓励**: 需要移动到不同区域发现更多商店
- **社交促进**: 玩家需要与附近商店主交流
- **竞争平衡**: 避免大商户垄断信息优势

这个功能完美平衡了信息透明度和商业隐私保护，为玩家提供了实用的附近商店查找功能，同时保护了服务器的商业生态平衡！
