# 智能玩家查找功能说明

## 功能概述

基于用户反馈，我们大幅增强了`/shoptools who`命令的玩家查找功能，实现了智能、灵活的玩家搜索系统。

## 新增功能特性

### 1. 多种搜索方式支持

**UUID搜索**:
```
/st who 123e4567-e89b-12d3-a456-426614174000
```
- 支持完整UUID精确匹配
- 适用于需要精确查找特定玩家的场景

**完整玩家名搜索**:
```
/st who PlayerName
```
- 支持完整玩家名精确匹配（不区分大小写）
- 优先级最高的匹配方式

**部分玩家名搜索**:
```
/st who Player
/st who play
```
- 支持部分玩家名模糊匹配
- 包含关系匹配，如"play"可以匹配"PlayerName"

### 2. 智能匹配算法

#### 匹配优先级

1. **UUID精确匹配** - 最高优先级
2. **完整名称匹配** - 不区分大小写
3. **部分名称匹配** - 包含关系匹配

#### 匹配逻辑实现

```java
private PlayerSearchResult findPlayerShops(String searchTerm) {
    // 1. UUID精确匹配
    if (isUUID(searchTerm)) {
        UUID playerId = UUID.fromString(searchTerm);
        List<ShopData> shops = dataManager.getShopsByOwner(playerId);
        if (!shops.isEmpty()) {
            return new PlayerSearchResult(SINGLE_MATCH, shops, playerName);
        }
    }
    
    // 2. 完整名称匹配
    for (PlayerMatch match : playerMap.values()) {
        if (match.getPlayerName().equalsIgnoreCase(searchTerm)) {
            return new PlayerSearchResult(SINGLE_MATCH, shops, playerName);
        }
    }
    
    // 3. 部分名称匹配
    List<PlayerMatch> partialMatches = findPartialMatches(searchTerm);
    if (partialMatches.size() == 1) {
        return new PlayerSearchResult(SINGLE_MATCH, shops, playerName);
    } else if (partialMatches.size() > 1) {
        return new PlayerSearchResult(MULTIPLE_MATCHES, partialMatches);
    }
    
    return new PlayerSearchResult(NO_MATCH, new ArrayList<>());
}
```

### 3. 多结果处理

当搜索词匹配到多个玩家时，系统会显示选择列表：

**示例输出**:
```
管理员: /st who play
系统: 找到多个匹配 "play" 的玩家:
      请使用更精确的名称重新搜索:
      Player1 (15个商店) - /st who Player1
      Player2 (8个商店) - /st who Player2
      PlayerABC (3个商店) - /st who PlayerABC
      ... 还有 2 个匹配结果
```

**特性**:
- 按商店数量降序排序（商店多的玩家优先显示）
- 最多显示10个匹配结果
- 提供精确的查找命令
- 显示每个玩家的商店数量

### 4. 智能建议系统

当没有找到匹配结果时，系统会提供相似玩家名建议：

**示例输出**:
```
管理员: /st who playeer
系统: 未找到玩家 playeer 的商店。
      你是否想查找: Player1, Player2, PlayerABC
```

#### 相似度算法

实现了多种相似度检测方法：

1. **包含关系**: 目标名称包含搜索词或搜索词包含目标名称
2. **开头匹配**: 目标名称以搜索词开头或搜索词以目标名称开头
3. **编辑距离**: 对于短字符串使用Levenshtein距离算法

```java
private boolean isSimilar(String search, String target) {
    // 包含关系
    if (target.contains(search) || search.contains(target)) {
        return true;
    }
    
    // 开头匹配
    if (target.startsWith(search) || search.startsWith(target)) {
        return true;
    }
    
    // 编辑距离（适用于短字符串）
    if (search.length() <= 3 && target.length() <= 8) {
        return getLevenshteinDistance(search, target) <= 2;
    }
    
    return false;
}
```

### 5. 性能优化

#### 玩家信息缓存

```java
// 构建玩家信息映射，避免重复查询
Map<UUID, PlayerMatch> playerMap = new HashMap<>();
for (ShopData shop : allShops) {
    UUID ownerId = shop.getOwnerId();
    String ownerName = getPlayerName(ownerId, shop.getOwnerName());
    
    PlayerMatch match = playerMap.computeIfAbsent(ownerId, 
        id -> new PlayerMatch(ownerName, id, 0));
    playerMap.put(ownerId, new PlayerMatch(match.getPlayerName(), ownerId, match.getShopCount() + 1));
}
```

#### 智能玩家名获取

```java
private String getPlayerName(UUID playerId, String cachedName) {
    // 1. 优先使用缓存的名称（如果不是UUID格式）
    if (cachedName != null && !isUUID(cachedName)) {
        return cachedName;
    }
    
    // 2. 尝试从在线玩家获取
    Player onlinePlayer = Bukkit.getPlayer(playerId);
    if (onlinePlayer != null) {
        return onlinePlayer.getName();
    }
    
    // 3. 尝试从离线玩家获取
    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
    String name = offlinePlayer.getName();
    if (name != null && !name.trim().isEmpty()) {
        return name;
    }
    
    // 4. 最后返回UUID简短形式
    return playerId.toString().substring(0, 8) + "...";
}
```

## 使用示例

### 场景1: 精确玩家名查找
```
管理员: /st who Alice
系统: === 玩家 Alice 的商店 ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      石头 world (120, 64, 220) 1.00 Alice [收购]
      === 共 2 个商店 ===
```

### 场景2: 部分名称匹配
```
管理员: /st who Ali
系统: === 玩家 Alice 的商店 ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      石头 world (120, 64, 220) 1.00 Alice [收购]
      === 共 2 个商店 ===
```

### 场景3: 多个匹配结果
```
管理员: /st who play
系统: 找到多个匹配 "play" 的玩家:
      请使用更精确的名称重新搜索:
      Player1 (15个商店) - /st who Player1
      Player2 (8个商店) - /st who Player2
      PlayerABC (3个商店) - /st who PlayerABC
```

### 场景4: UUID查找
```
管理员: /st who 123e4567-e89b-12d3-a456-426614174000
系统: === 玩家 Alice 的商店 ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      === 共 1 个商店 ===
```

### 场景5: 智能建议
```
管理员: /st who Alise
系统: 未找到玩家 Alise 的商店。
      你是否想查找: Alice, Alicia
```

## 错误处理和用户引导

### 1. 输入验证
- 检查搜索词是否为空
- 验证UUID格式的正确性
- 处理特殊字符和空格

### 2. 友好的错误提示
- 提供正确的命令格式
- 说明支持的搜索方式
- 给出具体的使用示例

### 3. 智能引导
- 多结果时提供精确命令
- 无结果时提供相似建议
- 显示每个玩家的商店数量帮助选择

## 技术实现细节

### 数据结构

**PlayerSearchResult类**:
```java
private static class PlayerSearchResult {
    public enum ResultType {
        NO_MATCH,           // 没有匹配
        SINGLE_MATCH,       // 单个匹配
        MULTIPLE_MATCHES    // 多个匹配
    }
    
    private final ResultType type;
    private final List<ShopData> shops;
    private final String playerDisplayName;
    private final List<PlayerMatch> playerMatches;
}
```

**PlayerMatch类**:
```java
private static class PlayerMatch {
    private final String playerName;
    private final UUID playerId;
    private final int shopCount;
}
```

### 算法复杂度

- **UUID查找**: O(1) - 哈希表查找
- **精确名称匹配**: O(n) - 线性搜索
- **部分名称匹配**: O(n) - 线性搜索
- **相似度计算**: O(m×n) - 编辑距离算法

其中n为玩家数量，m为字符串长度。

## 配置更新

更新了帮助信息以反映新功能：

```yaml
help-who: "&e/shoptools who <玩家名/UUID> &7- 显示指定玩家的商店"
```

## 部署说明

这个增强功能完全向后兼容：
- ✅ 现有的精确玩家名查找仍然有效
- ✅ 新增的模糊匹配和UUID查找功能
- ✅ 智能建议系统提升用户体验
- ✅ 多结果处理避免混淆

管理员现在可以更轻松地：
- 🔍 通过部分名称快速找到玩家
- 🎯 使用UUID进行精确查找
- 📋 在多个匹配中快速选择
- 💡 获得智能的搜索建议

这个功能大大提升了管理员查找玩家商店的效率和便利性！
