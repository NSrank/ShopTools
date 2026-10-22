# 数据库直接删除解决方案

## 🎯 新的解决方案

既然ShopPurger的purge()方法没有解决持久化问题，我们现在实现了**数据库直接删除**的解决方案！

### 🔍 核心思路

直接操作QuickShop的数据库，从根源上删除商店记录：
1. **获取数据库连接**：通过QuickShop的DatabaseManager
2. **探索表结构**：自动发现商店数据表和字段
3. **直接删除记录**：使用SQL语句删除商店数据
4. **多策略尝试**：尝试不同的删除方式确保成功

## 🚀 新增功能

### 1. 数据库直接删除命令
```bash
/st dbban <玩家名>
```

**功能特点：**
- 🔍 **自动探索数据库结构**：发现表名和字段
- 🗃️ **多策略删除**：尝试不同的SQL删除语句
- 💾 **完整备份**：删除前自动备份所有数据
- 📊 **详细反馈**：显示每个步骤的执行结果

### 2. 数据库连接获取
- 通过QuickShop的DatabaseManager获取连接
- 支持多种连接方式：getConnection、getDataSource等
- 自动处理不同类型的数据库对象

### 3. 智能表结构探索
- 自动查找可能的商店表名
- 分析表结构和字段类型
- 适配不同版本的QuickShop数据库结构

## 📋 测试步骤

### 第一步：测试数据库直接删除
```bash
/st dbban XRZQQ_QWQ
```

**预期输出：**
```
开始数据库直接删除操作...
警告: 这是实验性功能，可能有风险！
找到 48 个商店，开始数据库直接删除...

=== 商店 1 数据库删除测试 ===
尝试数据库直接删除商店: Shop world(-2244, 82, 1016)...
内存删除结果: 成功
成功获取数据库连接
数据库类型: SQLite
找到商店表: quickshop_shops
表 quickshop_shops 的结构:
  - id (INTEGER)
  - world (TEXT)
  - x (INTEGER)
  - y (INTEGER)
  - z (INTEGER)
  - owner (TEXT)
  - item (TEXT)
  - price (REAL)
删除策略: DELETE FROM quickshop_shops WHERE world = ? AND x = ? AND y = ? AND z = ?
删除的记录数: 1
数据库直接删除成功！
```

### 第二步：验证持久性（关键测试！）
1. **执行dbban命令**
2. **确认商店立即消失**
3. **重启服务器**
4. **再次检查商店是否恢复**

## 🔍 数据库删除策略

### 策略1：基于位置删除
```sql
DELETE FROM quickshop_shops WHERE world = ? AND x = ? AND y = ? AND z = ?
```

### 策略2：基于玩家删除
```sql
DELETE FROM quickshop_shops WHERE owner = ?
```

### 策略3：适配不同字段名
```sql
-- 适配不同的世界字段名
DELETE FROM quickshop_shops WHERE worldname = ? AND x = ? AND y = ? AND z = ?
DELETE FROM quickshop_shops WHERE world_name = ? AND x = ? AND y = ? AND z = ?
```

### 策略4：适配不同表名
自动尝试这些可能的表名：
- `quickshop_shops`
- `qs_shops`
- `shops`
- `quickshop`
- `shop_data`
- `quickshop_data`

## 🔧 技术实现

### 数据库连接获取
```java
// 1. 获取QuickShop实例
QuickShop quickShop = QuickShop.getInstance();

// 2. 获取DatabaseManager
Object databaseManager = quickShop.getDatabaseManager();

// 3. 尝试多种方式获取连接
String[] methods = {"getConnection", "getDataSource", "getDatabase"};
for (String method : methods) {
    Object result = databaseManager.getClass().getMethod(method).invoke(databaseManager);
    if (result instanceof Connection) {
        return (Connection) result;
    }
}
```

### 智能表结构探索
```java
// 1. 获取数据库元数据
DatabaseMetaData metaData = connection.getMetaData();

// 2. 查找商店表
String[] possibleTables = {"quickshop_shops", "qs_shops", "shops"};
for (String tableName : possibleTables) {
    ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
    if (tables.next()) {
        // 找到表，分析结构
        return tableName;
    }
}
```

### 多策略删除执行
```java
// 尝试不同的删除SQL
String[] deleteStrategies = {
    "DELETE FROM " + tableName + " WHERE world = ? AND x = ? AND y = ? AND z = ?",
    "DELETE FROM " + tableName + " WHERE owner = ?",
    // 更多策略...
};

for (String sql : deleteStrategies) {
    PreparedStatement stmt = connection.prepareStatement(sql);
    // 设置参数并执行
    int deletedRows = stmt.executeUpdate();
    if (deletedRows > 0) {
        return true; // 删除成功
    }
}
```

## 📊 预期结果

### 成功场景
如果数据库直接删除成功：
- ✅ **立即删除**：商店立即从游戏中消失
- ✅ **持久化删除**：重启后商店不会恢复
- ✅ **数据库一致性**：数据库记录被彻底删除
- ✅ **完整备份**：所有数据正确备份

### 可能发现的信息
- **数据库类型**：SQLite、MySQL等
- **表结构**：字段名称和类型
- **删除效果**：每种策略的删除记录数

## ⚠️ 安全注意事项

### 实验性功能警告
- 这是直接操作数据库的实验性功能
- 建议先在测试服务器进行验证
- 确保有完整的数据备份

### 风险评估
- **低风险**：只删除指定玩家的商店
- **中风险**：直接操作数据库可能影响数据完整性
- **高风险**：如果SQL语句错误可能影响其他数据

### 安全措施
- 自动备份所有删除的商店数据
- 只测试前3个商店避免大规模影响
- 详细的日志记录每个操作步骤

## 🎯 成功标准

### 技术标准
- [ ] 成功获取数据库连接
- [ ] 正确识别商店数据表
- [ ] 成功删除数据库记录
- [ ] 重启后商店不会恢复

### 用户体验标准
- [ ] 命令执行流畅
- [ ] 详细的操作反馈
- [ ] 完整的备份机制
- [ ] 清晰的错误处理

## 🚀 立即测试

### 部署和测试
1. **部署新版本**插件
2. **执行数据库删除**：`/st dbban XRZQQ_QWQ`
3. **观察详细输出**：数据库类型、表结构、删除结果
4. **验证立即效果**：商店是否消失
5. **重启服务器**：验证持久性

### 如果成功
- 我们找到了真正的持久化删除解决方案！
- 可以将此方法集成到正式的ban命令中
- 优化性能和用户体验

### 如果失败
- 分析输出日志，了解失败原因
- 可能需要调整SQL语句或表名
- 探索其他数据库操作方式

## 总结

数据库直接删除是我们的**最终解决方案**！

通过直接操作QuickShop的数据库，我们可以从根源上删除商店记录，确保重启后不会恢复。

**立即开始测试：**
```bash
/st dbban XRZQQ_QWQ
```

这次我们直接攻击问题的核心 - 数据库！让我们看看能否彻底解决这个持久化删除问题！🚀
