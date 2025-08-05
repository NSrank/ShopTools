# ShopTools Ban功能问题分析与解决方案

## 🚨 发现的问题

### 问题描述
当前的`/shoptools ban`功能存在一个严重问题：
- **商店只是被"暂时"删除**：使用QuickShop API的`removeShop`方法只是从内存中移除商店
- **重启后重新加载**：服务器重启时，QuickShop会从数据库/文件重新加载所有商店
- **删除效果失效**：被ban的玩家的商店会重新出现

### 问题根源
QuickShop的`removeShop`方法可能只是：
1. 从内存中的商店列表移除
2. 移除游戏世界中的商店标识
3. **但不删除持久化数据**（数据库记录或配置文件）

## 🔍 可能的解决方案

### 方案1：探索QuickShop的持久化删除API

**思路：** 寻找真正删除商店数据的API方法

**实现步骤：**
1. 扩展API探索，寻找更彻底的删除方法
2. 可能的方法名：
   - `deleteShopPermanently`
   - `removeShopFromDatabase`
   - `purgeShop`
   - `destroyShop`

**代码示例：**
```java
// 在QuickShopIntegration中添加
public boolean permanentlyDeleteShop(Shop shop) {
    String[] permanentDeleteMethods = {
        "deleteShopPermanently", "removeShopFromDatabase", 
        "purgeShop", "destroyShop", "deleteShopData"
    };
    
    for (String methodName : permanentDeleteMethods) {
        try {
            Method method = shopManagerClass.getMethod(methodName, Shop.class);
            method.invoke(shopManager, shop);
            return true;
        } catch (NoSuchMethodException ignored) {}
    }
    return false;
}
```

### 方案2：直接操作QuickShop数据库

**思路：** 绕过API，直接删除数据库中的商店记录

**优点：**
- 彻底删除，重启后不会恢复
- 完全控制删除过程

**缺点：**
- 需要了解QuickShop数据库结构
- 可能与QuickShop版本不兼容
- 风险较高，可能破坏数据完整性

**实现考虑：**
```java
// 需要研究QuickShop的数据库表结构
// 可能的表名：quickshop_shops, qs_shops等
public boolean deleteShopFromDatabase(UUID shopId) {
    // 直接SQL删除
    // DELETE FROM quickshop_shops WHERE shop_id = ?
}
```

### 方案3：文件系统操作（如果QuickShop使用文件存储）

**思路：** 如果QuickShop使用文件存储商店数据，直接删除相关文件

**实现步骤：**
1. 研究QuickShop的数据存储方式
2. 定位商店数据文件
3. 删除对应的商店记录

### 方案4：组合方案 - API删除 + 数据清理

**思路：** 结合多种方法确保彻底删除

**实现流程：**
1. 使用现有API删除（内存清理）
2. 探索并使用持久化删除API
3. 如果API不足，补充数据库/文件操作
4. 验证删除效果

## 🧪 测试方案

### 测试步骤
1. **创建测试商店**：让测试玩家创建几个商店
2. **执行ban命令**：`/st ban TestPlayer`
3. **验证即时效果**：确认商店立即消失
4. **重启服务器**：完全重启Minecraft服务器
5. **检查恢复情况**：查看商店是否重新出现

### 验证方法
```bash
# 重启前检查
/st search TestItem

# 重启服务器
# 重启后再次检查
/st search TestItem
```

## 🔧 推荐的实现策略

### 第一阶段：API探索增强
```java
public boolean supportsShopPermanentDeletion() {
    // 扩展现有的API探索，寻找持久化删除方法
    String[] permanentDeleteMethods = {
        "deleteShopPermanently", "removeShopFromDatabase", 
        "purgeShop", "destroyShop", "deleteShopData",
        "removeShopCompletely", "eraseShop"
    };
    
    for (String methodName : permanentDeleteMethods) {
        // 尝试找到这些方法
    }
}
```

### 第二阶段：数据库研究
如果API探索失败，研究QuickShop的数据存储：
1. 检查QuickShop配置文件
2. 查看数据库连接信息
3. 分析表结构
4. 实现直接数据库操作

### 第三阶段：验证和测试
1. 实现测试环境
2. 验证删除效果
3. 确保数据完整性
4. 性能测试

## ⚠️ 风险评估

### 高风险操作
- **直接数据库操作**：可能破坏数据完整性
- **文件系统操作**：可能影响其他功能
- **未知API调用**：可能导致插件崩溃

### 安全措施
1. **完整备份**：操作前备份所有数据
2. **测试环境**：先在测试服务器验证
3. **回滚机制**：准备数据恢复方案
4. **日志记录**：详细记录所有操作

## 🎯 下一步行动

### 立即行动
1. **测试当前问题**：验证重启后商店确实会恢复
2. **API探索增强**：扩展搜索更多删除方法
3. **QuickShop研究**：查看QuickShop文档和源码

### 中期计划
1. **实现持久化删除**：根据研究结果实现真正的删除
2. **测试验证**：确保删除效果持久
3. **用户反馈**：收集实际使用效果

### 长期考虑
1. **功能完善**：可能需要实现真正的unban功能
2. **兼容性维护**：跟进QuickShop版本更新
3. **性能优化**：优化删除操作的性能

## 总结

当前的ban功能确实存在重启后恢复的问题，这是一个需要立即解决的重要问题。我们需要：

1. **立即测试验证**问题的确切表现
2. **探索更深层的删除API**
3. **研究QuickShop的数据存储机制**
4. **实现真正的持久化删除**

这个问题的解决对于ban功能的实用性至关重要！
