# 分页系统和用户体验改进说明

## 改进概述

基于用户反馈，我们实现了以下重要改进：

1. **UUID转换为玩家名**: 显示真实玩家名而不是UUID
2. **商店状态显示**: 添加[出售]/[收购]/[双向]状态标识
3. **分页系统**: 避免一次性显示大量商店信息
4. **命令重构**: 优化命令结构和用户体验

## 详细改进内容

### 1. UUID到玩家名转换

**问题**: 原来显示的是玩家UUID，用户体验差
```
钻石 world (100, 64, 200) 50.00 123e4567-e89b-12d3-a456-426614174000
```

**解决方案**: 智能玩家名获取
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
    if (name != null) {
        return name;
    }
    
    // 4. 最后返回UUID简短形式
    return playerId.toString().substring(0, 8) + "...";
}
```

**效果**: 现在显示真实玩家名
```
钻石 world (100, 64, 200) 50.00 PlayerName [出售]
```

### 2. 商店状态显示

**新增功能**: 在每个商店信息后显示状态标识

```java
private String getShopStatusText(ShopData.ShopType shopType) {
    switch (shopType) {
        case SELLING:
            return "&a[出售]";  // 绿色
        case BUYING:
            return "&b[收购]";  // 青色
        case BOTH:
            return "&e[双向]";  // 黄色
        default:
            return "&7[未知]";  // 灰色
    }
}
```

**显示效果**:
- `钻石 world (100, 64, 200) 50.00 PlayerName [出售]`
- `铁锭 world (150, 64, 250) 10.00 PlayerB [收购]`
- `金锭 world (200, 64, 300) 25.00 PlayerC [双向]`

### 3. 分页系统实现

**问题**: `/shoptools list`一次性显示6000+商店，造成：
- 聊天框被刷屏
- 服务器性能压力
- 用户无法有效浏览信息

**解决方案**: 实现分页系统

#### 新命令结构

**旧命令**:
- `/shoptools list` - 显示所有商店（问题）
- `/shoptools list <物品ID>` - 显示特定物品商店

**新命令**:
- `/shoptools page <页码>` - 分页显示所有商店
- `/shoptools list <物品ID>` - 显示特定物品商店（保持不变）

#### 分页功能特性

**每页显示**: 10个商店
**页码计算**: 自动计算总页数
**导航提示**: 显示上一页/下一页命令
**范围显示**: 显示当前页的商店范围

**示例输出**:
```
=== 所有商店 (第1页/共672页) ===
苹果 world (100, 64, 200) 5.00 Alice [出售]
钻石 world (150, 64, 250) 100.00 Bob [出售]
石头 world (200, 64, 300) 1.00 Charlie [收购]
...（共10个商店）
显示第 1-10 个商店，共 6717 个
下一页: /st page 2
```

#### 分页实现细节

```java
private void displayShopListPaged(CommandSender sender, List<ShopData> shops, String title, int page) {
    int pageSize = 10;
    int totalPages = (int) Math.ceil((double) shops.size() / pageSize);
    
    // 页码验证
    if (page > totalPages) {
        MessageUtil.sendMessage(sender, "&c页码超出范围！总共 " + totalPages + " 页。");
        return;
    }
    
    // 计算显示范围
    int start = (page - 1) * pageSize;
    int end = Math.min(start + pageSize, shops.size());
    
    // 显示商店信息...
    
    // 导航提示
    if (page > 1) {
        navigation.append("上一页: &e/st page ").append(page - 1);
    }
    if (page < totalPages) {
        navigation.append("下一页: &e/st page ").append(page + 1);
    }
}
```

### 4. Tab补全增强

**新增补全功能**:

1. **page命令补全**: 自动补全页码（显示前10页）
```java
if ("page".equals(subCommand)) {
    int totalShops = dataManager.getShopCount();
    int totalPages = (int) Math.ceil((double) totalShops / 10);
    for (int i = 1; i <= Math.min(totalPages, 10); i++) {
        completions.add(String.valueOf(i));
    }
}
```

2. **命令顺序优化**: `page`命令排在第一位，因为它是最常用的

### 5. 帮助系统更新

**新的帮助信息**:
```yaml
help-header: "&6=== ShopTools 帮助 ==="
help-page: "&e/shoptools page <页码> &7- 分页显示所有商店"
help-list: "&e/shoptools list <物品ID> &7- 显示指定物品的商店"
help-list-item: "&7别名: &e/st page <页码>&7, &e/st list <物品ID>"
help-who: "&e/shoptools who <玩家名> &7- 显示指定玩家的商店"
help-reload: "&e/shoptools reload &7- 重新加载配置和数据"
```

### 6. 错误处理和用户引导

**智能提示**: 当用户使用旧命令时，提供新命令提示
```java
if (args.length < 2) {
    MessageUtil.sendMessage(sender, "&c用法: /shoptools list <物品ID>");
    MessageUtil.sendMessage(sender, "&e提示: 使用 /shoptools page <页码> 查看所有商店");
    return;
}
```

**页码验证**: 完善的页码范围检查
```java
if (page > totalPages) {
    MessageUtil.sendMessage(sender, "&c页码超出范围！总共 " + totalPages + " 页。");
    return;
}
```

## 性能优化

### 1. 内存使用优化
- 分页显示避免一次性加载大量文本到聊天框
- 减少客户端内存压力

### 2. 服务器性能优化
- 避免一次性发送大量消息包
- 减少网络带宽占用

### 3. 用户体验优化
- 信息分页便于浏览
- 智能玩家名显示提高可读性
- 状态标识一目了然

## 使用示例

### 浏览所有商店
```
玩家: /st page 1
系统: === 所有商店 (第1页/共672页) ===
      苹果 world (100, 64, 200) 5.00 Alice [出售]
      钻石 world (150, 64, 250) 100.00 Bob [出售]
      ...
      下一页: /st page 2

玩家: /st page 2
系统: === 所有商店 (第2页/共672页) ===
      ...
      上一页: /st page 1  下一页: /st page 3
```

### 查找特定物品
```
玩家: /st list DIAMOND
系统: === 物品 DIAMOND 的商店 ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      钻石 world (150, 70, 300) 55.00 Bob [出售]
      钻石 world (200, 65, 400) 60.00 Charlie [收购]
      === 共 3 个商店 ===
```

## 配置更新

**消息格式更新**:
```yaml
shop-list-item: "&e{item} &7{location} &a{price} &b{owner} {status}"
```

新增了`{status}`占位符用于显示商店状态。

## 部署说明

这些改进完全向后兼容：
- ✅ 现有配置文件自动升级
- ✅ 旧命令仍然可用（带提示）
- ✅ 不影响现有数据

用户可以立即享受到：
- 🎯 更清晰的商店信息显示
- 📄 便于浏览的分页系统
- 🚀 更好的服务器性能
- 💡 智能的用户引导
