# ShopTools 持久化删除调试指南

## 🎯 目标

解决ban功能的持久性问题：商店被删除后，服务器重启时会重新加载。我们需要实现真正的持久化删除。

## 🔧 新增的调试功能

### Debug命令概览
```bash
/shoptools debug storage    # 探索QuickShop数据存储方式
/shoptools debug delete <玩家名>  # 测试持久化删除方法
```

### 权限要求
- 需要 `shoptools.admin` 权限
- 仅管理员可使用

## 📋 测试步骤

### 第一步：探索数据存储
```bash
/st debug storage
```

**这个命令会探索：**
- QuickShop的ShopManager类信息
- 数据库相关方法（如果存在）
- QuickShop插件版本和数据文件夹
- 数据文件夹内容（配置文件、数据库文件等）
- 可能的持久化删除API方法

**预期输出示例：**
```
=== QuickShop数据存储探索 ===
ShopManager类: com.ghostchu.quickshop.shop.ShopManager
QuickShop插件版本: 5.1.2.0
QuickShop数据文件夹: /plugins/QuickShop
数据文件夹内容:
  - config.yml (文件, 15234 bytes)
  - database.db (文件, 2048576 bytes)
  - shops/ (文件夹)
找到数据库方法: getDatabase
找到数据库方法: saveShop
```

### 第二步：测试持久化删除
```bash
/st debug delete <玩家名>
```

**这个命令会：**
1. 找到指定玩家的商店（最多测试3个）
2. 尝试使用现有的`removeShop`方法（内存删除）
3. 尝试寻找并调用持久化删除方法
4. 报告每个步骤的结果

**预期输出示例：**
```
正在测试玩家 TestPlayer 的商店持久化删除...
找到 5 个商店，开始测试删除...

=== 商店 1 删除测试 ===
尝试持久化删除商店: Shop{location=world(100,64,200), item=DIAMOND}
内存删除结果: 成功
持久化删除成功，使用方法: deleteShopPermanently

=== 商店 2 删除测试 ===
尝试持久化删除商店: Shop{location=world(150,64,250), item=IRON_INGOT}
内存删除结果: 成功
未找到可用的持久化删除方法

持久化删除测试完成！
请重启服务器后检查商店是否真正被删除。
```

## 🔍 可能的发现结果

### 情况A：找到持久化删除API ✅
如果输出显示找到了类似这些方法：
- `deleteShopPermanently`
- `removeShopFromDatabase`
- `purgeShop`
- `destroyShop`

**下一步：** 我们将实现这些方法到正式的ban命令中

### 情况B：找到数据库访问方法 🔧
如果输出显示找到了：
- `getDatabase`
- `getConnection`
- `saveShop`

**下一步：** 我们可以研究直接数据库操作

### 情况C：发现文件存储 📁
如果数据文件夹包含：
- `shops.yml`
- `shops/` 文件夹
- 其他配置文件

**下一步：** 我们可以研究文件系统操作

### 情况D：没有找到明显的持久化方法 ❌
**下一步：** 需要更深入的研究或考虑替代方案

## 🧪 验证测试

### 完整的验证流程
1. **创建测试商店**：
   ```bash
   # 让测试玩家创建几个商店
   ```

2. **执行调试测试**：
   ```bash
   /st debug delete TestPlayer
   ```

3. **重启服务器**：
   ```bash
   # 完全重启Minecraft服务器
   ```

4. **验证删除效果**：
   ```bash
   /st search TestItem
   /st near TestItem
   ```

5. **检查结果**：
   - 如果商店不再出现 → 持久化删除成功 ✅
   - 如果商店重新出现 → 仍需改进 ❌

## 🔧 基于结果的实现策略

### 策略1：API增强（如果找到持久化API）
```java
// 在ban命令中添加持久化删除调用
public boolean banPlayerShops(UUID playerUUID) {
    List<Shop> shops = getPlayerShops(playerUUID);
    for (Shop shop : shops) {
        // 1. 内存删除
        removeShop(shop);
        // 2. 持久化删除
        deleteShopPermanently(shop); // 使用找到的方法
    }
}
```

### 策略2：数据库直接操作（如果找到数据库访问）
```java
// 直接操作QuickShop数据库
public boolean deleteShopFromDatabase(Shop shop) {
    Connection conn = quickShopAPI.getDatabase().getConnection();
    PreparedStatement stmt = conn.prepareStatement(
        "DELETE FROM quickshop_shops WHERE shop_id = ?"
    );
    stmt.setString(1, shop.getShopId());
    return stmt.executeUpdate() > 0;
}
```

### 策略3：文件系统操作（如果使用文件存储）
```java
// 直接删除商店配置文件
public boolean deleteShopFromFile(Shop shop) {
    File shopFile = new File(quickShopDataFolder, 
        "shops/" + shop.getShopId() + ".yml");
    return shopFile.delete();
}
```

## ⚠️ 安全注意事项

### 测试环境
- **强烈建议**先在测试服务器进行调试
- 确保有完整的数据备份
- 测试前记录当前商店状态

### 风险评估
- **低风险**：使用官方API方法
- **中风险**：直接数据库操作
- **高风险**：文件系统操作

### 回滚准备
- 备份QuickShop数据文件夹
- 备份数据库（如果使用）
- 记录测试操作步骤

## 📊 预期时间线

### 立即行动（今天）
1. 执行 `/st debug storage` 探索数据存储
2. 执行 `/st debug delete TestPlayer` 测试删除
3. 分析输出结果

### 短期目标（1-2天）
1. 根据调试结果实现持久化删除
2. 集成到正式ban命令
3. 测试验证效果

### 中期目标（1周内）
1. 完善错误处理
2. 优化性能
3. 用户文档更新

## 🎯 成功标准

### 技术标准
- [ ] 商店删除后重启不会恢复
- [ ] 删除操作不影响其他商店
- [ ] 备份功能正常工作
- [ ] 无数据完整性问题

### 用户体验标准
- [ ] 删除操作响应及时
- [ ] 错误信息清晰明确
- [ ] 操作结果准确反馈
- [ ] 管理员易于使用

## 总结

通过新增的debug功能，我们现在有了系统性的方法来探索和解决QuickShop的持久化删除问题。请按照上述步骤进行测试，我们将根据结果制定具体的实现方案！

让我们开始探索QuickShop的数据存储奥秘！🚀
