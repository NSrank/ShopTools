# QuickShop-Reremake 调试指南

## 🎯 目标

基于您发现的QuickShop-Reremake官方仓库信息，我们现在有了更精确的调试方向：
- **QuickShop API类**：`org.maxgamer.quickshop.QuickShop`
- **官方仓库**：https://github.com/PotatoCraft-Studio/QuickShop-Reremake

## 🔧 增强的调试功能

### 新增的探索能力

我已经增强了debug功能，现在可以：

1. **探索QuickShop主类**：
   - 检查`org.maxgamer.quickshop.QuickShop`类的所有方法
   - 查找与删除、数据库相关的方法
   - 获取QuickShop实例并探索实例方法

2. **深度API分析**：
   - 扫描所有包含"delete"、"remove"、"database"、"shop"关键词的方法
   - 分析方法参数类型
   - 探索QuickShop实例的管理器方法

3. **数据库直接操作**：
   - 尝试获取数据库连接
   - 探索数据库表结构
   - 实现直接SQL删除（如果可行）

## 📋 测试步骤

### 第一步：探索QuickShop-Reremake的内部结构
```bash
/st debug storage
```

**新的探索内容：**
- QuickShop主类的所有相关方法
- QuickShop实例的管理器方法
- 数据库连接和表结构信息

**预期发现（基于QuickShop-Reremake）：**
```
=== QuickShop数据存储探索 ===
ShopManager类: org.maxgamer.quickshop.shop.ShopManager
QuickShop主类: org.maxgamer.quickshop.QuickShop
QuickShop主类方法数量: 45
相关方法: getShopManager()
相关方法: getDatabaseManager()
相关方法: deleteShop(Shop)
成功获取QuickShop实例
实例方法: getDatabaseManager
实例方法: getShopManager
QuickShop API类: org.maxgamer.quickshop.QuickShopAPI
找到持久化删除方法: deleteShop
```

### 第二步：测试持久化删除方法
```bash
/st debug delete <玩家名>
```

**增强的测试功能：**
- 尝试调用找到的所有删除方法
- 测试数据库直接操作
- 详细报告每种方法的效果

### 第三步：验证持久性
1. 重启服务器
2. 检查商店是否真正被删除

## 🔍 基于QuickShop-Reremake的预期发现

### 可能的API方法

根据QuickShop-Reremake的架构，我们可能会发现：

#### 1. 主类方法
```java
// QuickShop主类可能的方法
org.maxgamer.quickshop.QuickShop.getInstance()
org.maxgamer.quickshop.QuickShop.getShopManager()
org.maxgamer.quickshop.QuickShop.getDatabaseManager()
```

#### 2. ShopManager方法
```java
// ShopManager可能的删除方法
ShopManager.deleteShop(Shop shop)
ShopManager.removeShop(Shop shop, boolean permanent)
ShopManager.unloadShop(Shop shop)
```

#### 3. DatabaseManager方法
```java
// DatabaseManager可能的方法
DatabaseManager.getConnection()
DatabaseManager.deleteShopData(Shop shop)
DatabaseManager.removeShopRecord(Location location)
```

### 数据库表结构

QuickShop-Reremake可能使用的表结构：
```sql
-- 可能的表名和结构
CREATE TABLE quickshop_shops (
    id INTEGER PRIMARY KEY,
    world VARCHAR(255),
    x INTEGER,
    y INTEGER, 
    z INTEGER,
    owner VARCHAR(36),
    item TEXT,
    price DECIMAL,
    type INTEGER,
    -- 其他字段...
);
```

## 🚀 实现策略

### 策略1：使用发现的API方法

如果找到了持久化删除方法：
```java
// 在ban命令中实现
public boolean permanentlyDeleteShop(Shop shop) {
    try {
        // 方法1：使用QuickShop主类
        QuickShop quickShop = QuickShop.getInstance();
        quickShop.getShopManager().deleteShop(shop);
        
        // 方法2：使用数据库管理器
        quickShop.getDatabaseManager().deleteShopData(shop);
        
        return true;
    } catch (Exception e) {
        logger.warning("持久化删除失败: " + e.getMessage());
        return false;
    }
}
```

### 策略2：数据库直接操作

如果API方法不足，使用数据库直接删除：
```java
public boolean deleteShopFromDatabase(Shop shop) {
    try {
        Connection conn = getDatabaseConnection();
        String sql = "DELETE FROM quickshop_shops WHERE world = ? AND x = ? AND y = ? AND z = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        
        Location loc = shop.getLocation();
        stmt.setString(1, loc.getWorld().getName());
        stmt.setInt(2, loc.getBlockX());
        stmt.setInt(3, loc.getBlockY());
        stmt.setInt(4, loc.getBlockZ());
        
        return stmt.executeUpdate() > 0;
    } catch (Exception e) {
        logger.warning("数据库删除失败: " + e.getMessage());
        return false;
    }
}
```

## 📊 测试计划

### 立即测试（今天）

1. **部署新版本**插件
2. **执行存储探索**：
   ```bash
   /st debug storage
   ```
3. **分析输出结果**，寻找：
   - QuickShop主类的删除方法
   - 数据库管理器的访问方法
   - 可用的持久化删除API

4. **测试删除方法**：
   ```bash
   /st debug delete TestPlayer
   ```

### 短期目标（1-2天）

1. **根据发现实现持久化删除**
2. **集成到正式ban命令**
3. **验证重启后的持久性**

### 验证标准

- [ ] 商店删除后立即消失
- [ ] 重启服务器后商店不会重新出现
- [ ] 其他玩家的商店不受影响
- [ ] 备份功能正常工作

## 🎯 成功指标

### 技术指标
- 找到QuickShop-Reremake的持久化删除API
- 实现真正的持久化删除
- 重启测试通过

### 用户体验指标
- ban命令执行流畅
- 删除效果立即可见
- 重启后效果持续

## 🔧 基于GitHub仓库的额外研究

### 可以研究的方向

1. **源码分析**：
   - 查看ShopManager的实现
   - 了解数据库操作的具体方法
   - 找到删除商店的完整流程

2. **API文档**：
   - 查找官方API文档
   - 了解推荐的删除方法
   - 学习最佳实践

3. **Issues和PR**：
   - 查看相关的问题报告
   - 了解其他开发者的解决方案
   - 学习社区的经验

## 总结

基于您发现的QuickShop-Reremake信息，我们现在有了更精确的调试方向。新增的调试功能将帮助我们：

1. **精确定位**QuickShop-Reremake的内部API
2. **发现持久化删除**的正确方法
3. **实现真正的商店删除**功能

让我们开始测试，探索QuickShop-Reremake的内部结构，找到解决持久化删除问题的最佳方案！🚀

**立即行动：**
```bash
/st debug storage
```

让我们看看QuickShop-Reremake为我们提供了什么强大的API！
