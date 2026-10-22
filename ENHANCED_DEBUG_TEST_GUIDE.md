# 增强调试测试指南

## 🎯 基于发现的关键信息

您的探索发现了QuickShop-Reremake的重要组件：

### 🔍 已发现的管理器
- ✅ **ShopManager** - 商店管理器
- ✅ **DatabaseManager** - 数据库管理器  
- ✅ **ShopPurger** - 商店清理器（重点！）
- ✅ **ShopLoader** - 商店加载器
- ✅ **ShopCache** - 商店缓存

### 🎯 重点关注：ShopPurger

`getShopPurger()` 这个方法名暗示它可能是专门用于**清理/删除**商店的组件！

## 🚀 增强的测试功能

我已经大幅增强了 `/st debug delete` 命令，现在它会：

### 1. 深度探索ShopPurger
- 获取ShopPurger实例
- 列出所有包含"purge"、"delete"、"remove"的方法
- 自动尝试调用合适的删除方法

### 2. 探索DatabaseManager
- 获取DatabaseManager实例
- 查找所有数据库删除相关方法
- 分析方法参数类型

### 3. 深度分析ShopManager
- 探索所有删除、移除、卸载、清理方法
- 自动尝试调用合适的方法

## 📋 立即测试

### 第一步：测试增强的持久化删除
```bash
/st debug delete <玩家名>
```

**预期发现（基于ShopPurger）：**
```
尝试持久化删除商店: Shop{...}
内存删除结果: 成功
成功获取ShopPurger: org.maxgamer.quickshop.shop.ShopPurger
ShopPurger方法: purgeShop(Shop)
ShopPurger方法: deleteShopPermanently(Shop)
ShopPurger删除成功，使用方法: purgeShop
成功获取DatabaseManager: org.maxgamer.quickshop.database.DatabaseManager
DatabaseManager方法: deleteShopData(Shop)
DatabaseManager方法: removeShopRecord(Location)
```

### 第二步：验证持久性
1. 执行debug delete命令
2. 重启服务器
3. 检查商店是否真正被删除

## 🔍 可能的发现结果

### 情况A：ShopPurger提供完整解决方案 ✅
如果发现类似方法：
- `ShopPurger.purgeShop(Shop shop)`
- `ShopPurger.deleteShopPermanently(Shop shop)`
- `ShopPurger.removeShopCompletely(Shop shop)`

**→ 这将是最理想的解决方案！**

### 情况B：DatabaseManager提供数据库操作 🔧
如果发现：
- `DatabaseManager.deleteShopData(Shop shop)`
- `DatabaseManager.removeShopRecord(Location location)`
- `DatabaseManager.purgeShopFromDatabase(Shop shop)`

**→ 我们可以实现数据库级别的删除**

### 情况C：ShopManager提供扩展方法 📊
如果发现：
- `ShopManager.removeShop(Shop shop, boolean permanent)`
- `ShopManager.unloadShop(Shop shop, boolean delete)`
- `ShopManager.deleteShop(Shop shop, boolean fromDatabase)`

**→ 我们可以使用带参数的删除方法**

## 🚀 实现策略

### 基于ShopPurger的实现
```java
public boolean permanentlyDeleteShop(Shop shop) {
    try {
        // 1. 获取QuickShop实例
        QuickShop quickShop = QuickShop.getInstance();
        
        // 2. 使用ShopPurger进行彻底删除
        Object shopPurger = quickShop.getShopPurger();
        Method purgeMethod = shopPurger.getClass().getMethod("purgeShop", Shop.class);
        purgeMethod.invoke(shopPurger, shop);
        
        logger.info("使用ShopPurger成功删除商店");
        return true;
        
    } catch (Exception e) {
        logger.warning("ShopPurger删除失败: " + e.getMessage());
        return false;
    }
}
```

### 基于DatabaseManager的实现
```java
public boolean deleteShopFromDatabase(Shop shop) {
    try {
        // 1. 获取DatabaseManager
        QuickShop quickShop = QuickShop.getInstance();
        Object databaseManager = quickShop.getDatabaseManager();
        
        // 2. 调用数据库删除方法
        Method deleteMethod = databaseManager.getClass().getMethod("deleteShopData", Shop.class);
        deleteMethod.invoke(databaseManager, shop);
        
        logger.info("使用DatabaseManager成功删除商店数据");
        return true;
        
    } catch (Exception e) {
        logger.warning("DatabaseManager删除失败: " + e.getMessage());
        return false;
    }
}
```

## 📊 测试计划

### 立即测试（现在）
1. **部署新版本**插件
2. **执行增强测试**：
   ```bash
   /st debug delete TestPlayer
   ```
3. **分析输出结果**，寻找：
   - ShopPurger的删除方法
   - DatabaseManager的数据库操作
   - ShopManager的扩展删除选项

### 短期验证（今天内）
1. **根据发现实现持久化删除**
2. **集成到正式ban命令**
3. **重启测试验证持久性**

## 🎯 成功指标

### 技术指标
- [ ] 找到ShopPurger的删除方法
- [ ] 实现真正的持久化删除
- [ ] 重启后商店不会恢复
- [ ] 其他商店不受影响

### 用户体验指标
- [ ] ban命令执行流畅
- [ ] 删除效果立即可见
- [ ] 重启后效果持续
- [ ] 备份功能正常

## 🔧 预期的完整解决方案

基于发现的组件，最终的ban命令可能会是：

```java
public boolean banPlayerShops(UUID playerUUID) {
    try {
        // 1. 获取玩家商店
        List<Shop> playerShops = getPlayerShops(playerUUID);
        
        // 2. 备份商店数据
        backupPlayerShops(playerUUID, playerShops);
        
        // 3. 使用ShopPurger彻底删除
        QuickShop quickShop = QuickShop.getInstance();
        Object shopPurger = quickShop.getShopPurger();
        
        for (Shop shop : playerShops) {
            // 内存删除
            quickShop.getShopManager().removeShop(shop);
            
            // 持久化删除
            shopPurger.purgeShop(shop);
        }
        
        return true;
    } catch (Exception e) {
        logger.severe("Ban操作失败: " + e.getMessage());
        return false;
    }
}
```

## 总结

基于您发现的QuickShop-Reremake组件，特别是**ShopPurger**，我们很有希望找到完美的持久化删除解决方案！

**立即行动：**
```bash
/st debug delete <玩家名>
```

让我们看看ShopPurger为我们提供了什么强大的删除功能！🚀

这次我们很可能会找到真正的持久化删除方法，彻底解决重启后商店恢复的问题！
