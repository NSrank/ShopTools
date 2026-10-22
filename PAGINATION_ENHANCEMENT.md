# 分页功能全面增强说明

## 功能概述

基于用户反馈，我们为`/shoptools list`和`/shoptools who`命令添加了完整的分页支持，解决了大量商店信息一次性显示造成的性能问题。

## 新增分页功能

### 1. `/shoptools list` 命令分页

**新命令格式**:
```
/shoptools list <物品ID> [页码]
/st list DIAMOND        # 默认显示第1页
/st list DIAMOND 2      # 显示第2页
```

**智能分页逻辑**:
- 当商店数量 ≤ 10个时：直接显示所有商店，无分页
- 当商店数量 > 10个时：自动启用分页，每页显示10个商店
- 未指定页码时：默认显示第1页

**示例输出**:
```
玩家: /st list DIAMOND
系统: === 物品 DIAMOND 的商店 (第1页/共3页) ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      钻石 world (150, 70, 300) 55.00 Bob [出售]
      ...（共10个商店）
      显示第 1-10 个商店，共 25 个
      下一页: /st list DIAMOND 2
```

### 2. `/shoptools who` 命令分页

**新命令格式**:
```
/shoptools who <玩家名/UUID> [页码]
/st who Alice           # 默认显示第1页
/st who Alice 2         # 显示第2页
/st who 123e4567... 1   # UUID查找第1页
```

**智能分页逻辑**:
- 当玩家商店数量 ≤ 10个时：直接显示所有商店，无分页
- 当玩家商店数量 > 10个时：自动启用分页，每页显示10个商店
- 支持所有查找方式的分页：完整名称、部分名称、UUID

**示例输出**:
```
管理员: /st who PlayerName
系统: === 玩家 PlayerName 的商店 (第1页/共5页) ===
      钻石 world (100, 64, 200) 50.00 PlayerName [出售]
      石头 world (120, 64, 220) 1.00 PlayerName [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 47 个
      下一页: /st who PlayerName 2
```

## 技术实现细节

### 1. 智能分页判断

```java
// 检查是否需要分页
int pageSize = 10;
if (shops.size() <= pageSize) {
    // 商店数量少，直接显示所有
    displayShopList(sender, shops, title, false);
} else {
    // 商店数量多，使用分页显示
    displayShopListPaged(sender, shops, title, page);
}
```

### 2. 页码参数解析

```java
// 检查是否指定了页码
if (args.length >= 3) {
    try {
        page = Integer.parseInt(args[2]);
        if (page < 1) {
            MessageUtil.sendMessage(sender, "&c页码必须大于0！");
            return;
        }
    } catch (NumberFormatException e) {
        MessageUtil.sendMessage(sender, "&c页码必须是数字！");
        return;
    }
}
```

### 3. 分页显示逻辑

使用统一的`displayShopListPaged`方法：
- 计算总页数和显示范围
- 验证页码有效性
- 显示当前页商店信息
- 提供导航提示

## Tab补全增强

### 1. 物品查找页码补全

```
/st list DIAMOND <TAB>
```
- 自动计算该物品的总页数
- 只有超过10个商店时才显示页码补全
- 最多显示前10页的补全选项

### 2. 玩家查找页码补全

```
/st who PlayerName <TAB>
```
- 自动计算该玩家的商店总页数
- 只有超过10个商店时才显示页码补全
- 支持智能匹配后的页码补全

### 3. 实现代码

```java
} else if (args.length == 3) {
    String subCommand = args[0].toLowerCase();
    
    if ("list".equals(subCommand)) {
        // 第三个参数：页码补全（针对特定物品）
        String itemId = args[1];
        List<ShopData> shops = dataManager.getShopsByItem(itemId);
        if (shops.size() > 10) { // 只有超过10个商店才需要分页
            int totalPages = (int) Math.ceil((double) shops.size() / 10);
            for (int i = 1; i <= Math.min(totalPages, 10); i++) {
                completions.add(String.valueOf(i));
            }
        }
    }
    // ... who命令类似逻辑
}
```

## 多结果显示优化

### 1. 智能命令建议

当`/st who`匹配到多个玩家时，建议命令会根据商店数量智能提示：

**修改前**:
```
Player1 (15个商店) - /st who Player1
```

**修改后**:
```
Player1 (15个商店) - /st who Player1 [页码]
```

### 2. 实现逻辑

```java
String command = "/st who " + match.getPlayerName();

// 如果商店数量超过10个，提示可能需要分页
if (match.getShopCount() > 10) {
    command += " [页码]";
}
```

## 用户体验优化

### 1. 默认行为

- **无页码参数**: 自动显示第1页
- **商店数量少**: 直接显示所有，无分页提示
- **商店数量多**: 启用分页，显示导航信息

### 2. 错误处理

- **页码验证**: 检查页码是否为正整数
- **范围检查**: 防止超出总页数
- **友好提示**: 提供正确的命令格式

### 3. 导航提示

- **页面信息**: 显示当前页/总页数
- **范围信息**: 显示当前页的商店范围
- **导航命令**: 提供上一页/下一页的具体命令

## 使用示例

### 场景1: 物品商店分页浏览

```
玩家: /st list DIAMOND
系统: === 物品 DIAMOND 的商店 (第1页/共3页) ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      钻石 world (150, 70, 300) 55.00 Bob [出售]
      钻石 world (200, 65, 400) 60.00 Charlie [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 25 个
      下一页: /st list DIAMOND 2

玩家: /st list DIAMOND 2
系统: === 物品 DIAMOND 的商店 (第2页/共3页) ===
      钻石 world (250, 64, 450) 65.00 David [出售]
      ...
      上一页: /st list DIAMOND 1  下一页: /st list DIAMOND 3
```

### 场景2: 玩家商店分页浏览

```
管理员: /st who BigMerchant
系统: === 玩家 BigMerchant 的商店 (第1页/共5页) ===
      钻石 world (100, 64, 200) 50.00 BigMerchant [出售]
      石头 world (120, 64, 220) 1.00 BigMerchant [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 47 个
      下一页: /st who BigMerchant 2

管理员: /st who BigMerchant 3
系统: === 玩家 BigMerchant 的商店 (第3页/共5页) ===
      ...
      上一页: /st who BigMerchant 2  下一页: /st who BigMerchant 4
```

### 场景3: 少量商店直接显示

```
玩家: /st list RARE_ITEM
系统: === 物品 RARE_ITEM 的商店 ===
      稀有物品 world (100, 64, 200) 1000.00 Alice [出售]
      稀有物品 world (200, 64, 300) 950.00 Bob [出售]
      === 共 2 个商店 ===
```

### 场景4: Tab补全示例

```
玩家: /st list DIAMOND <TAB>
系统: [1] [2] [3]

管理员: /st who BigMerchant <TAB>
系统: [1] [2] [3] [4] [5]
```

## 性能优化效果

### 1. 内存使用优化

- **分页显示**: 每次只显示10个商店，减少内存占用
- **智能判断**: 少量商店时避免不必要的分页计算

### 2. 网络优化

- **减少消息包**: 避免一次性发送大量聊天消息
- **分批传输**: 用户按需查看更多页面

### 3. 用户体验优化

- **信息清晰**: 分页显示便于阅读和查找
- **导航便利**: 明确的页面导航提示
- **智能默认**: 合理的默认行为减少用户操作

## 配置更新

### 帮助信息更新

```yaml
help-list: "&e/shoptools list <物品ID> [页码] &7- 显示指定物品的商店"
help-who: "&e/shoptools who <玩家名/UUID> [页码] &7- 显示指定玩家的商店"
```

### 命令描述更新

```yaml
usage: /<command> [page <页码>|list <物品ID> [页码]|who <玩家名> [页码]] [args...]
```

## 部署说明

这个增强功能完全向后兼容：
- ✅ 现有命令格式仍然有效
- ✅ 新增的页码参数为可选参数
- ✅ 智能分页判断，不影响少量数据的显示
- ✅ Tab补全增强，提升用户体验

用户现在可以：
- 📄 高效浏览大量商店信息
- 🎯 精确定位到特定页面
- ⚡ 享受更好的服务器性能
- 💡 使用智能的Tab补全功能

这个功能彻底解决了大量商店信息显示的性能问题，为用户提供了流畅的浏览体验！
