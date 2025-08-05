# ShopTools Unban功能实现指南

## 功能概述

`/shoptools unban <玩家名>` 命令的本质是：
1. 读取被ban玩家的商店备份数据
2. 在原位置重新创建相同的商店
3. 恢复所有商店属性（物品、价格、类型等）

## 当前实现状态

### ✅ 已完成的功能

#### 1. API探索机制
```java
public boolean supportsShopCreation() {
    // 检查常见的创建方法名
    String[] createMethodNames = {
        "createShop", "addShop", "create", "add", "newShop", "makeShop"
    };
    
    // 尝试不同的参数组合
    // (Location, ItemStack, double, ShopType, UUID)
    // (Location, ItemStack, double, ShopType, Player)
    // (Location, ItemStack, double, UUID)
}
```

#### 2. 完整的恢复流程
```java
private boolean restoreShop(ShopBackupData shopData, List<String> failureReasons) {
    // 1. 重建世界和位置
    // 2. 检查位置是否可用
    // 3. 重建物品ItemStack
    // 4. 重建商店类型ShopType
    // 5. 获取玩家UUID
    // 6. 调用QuickShop API创建商店
}
```

#### 3. 安全检查机制
- **位置冲突检查**：确保恢复位置没有其他商店
- **世界存在检查**：验证目标世界是否存在
- **数据有效性检查**：验证物品类型、商店类型、UUID等
- **权限验证**：仅管理员可执行

#### 4. 异步处理架构
- 备份数据读取在异步线程
- 商店创建在主线程（API安全）
- 详细的进度反馈

### 🔍 需要测试的API探索

现在您可以测试以下命令来探索QuickShop的创建API：

```bash
# 1. 首先测试API探索
/st unban TestPlayer
```

**预期输出（如果找到API）：**
```
[ShopTools] 找到商店创建方法: createShop(Location, ItemStack, double, ShopType, UUID)
开始恢复玩家 TestPlayer 的商店...
```

**预期输出（如果没找到API）：**
```
抱歉，当前版本的QuickShop不支持商店创建功能！
此功能需要QuickShop提供官方的商店创建API
```

## 测试步骤

### 第一步：API探索测试
```bash
# 测试是否能找到创建API
/st unban XRZQQ_QWQ
```

### 第二步：根据结果进行下一步

#### 情况A：找到了创建API
如果日志显示找到了创建方法，我们需要：
1. 实现具体的API调用
2. 测试商店创建功能
3. 验证恢复的商店是否正确

#### 情况B：没找到创建API
如果没找到API，我们可以：
1. 扩展搜索范围，查找更多可能的方法名
2. 研究QuickShop源码或文档
3. 考虑替代方案（如模拟玩家操作）

## 代码架构亮点

### 🛡️ 安全性设计
```java
// 位置冲突检查
private boolean isLocationOccupied(Location location) {
    List<ShopData> nearbyShops = dataManager.findNearbyShops(location, 1.0);
    // 检查是否有商店在相同位置
}

// 数据验证
Material material = Material.valueOf(shopData.getItemType()); // 可能抛出异常
ShopType shopType = ShopType.valueOf(shopData.getShopType()); // 可能抛出异常
UUID ownerUUID = UUID.fromString(shopData.getOwnerId());      // 可能抛出异常
```

### 📊 详细的错误报告
```java
List<String> failureReasons = new ArrayList<>();

// 收集所有失败原因
if (world == null) {
    failureReasons.add("世界不存在: " + shopData.getWorldName());
}

// 最终报告
MessageUtil.sendMessage(sender, "&c部分商店恢复失败，原因：");
for (String reason : failureReasons) {
    MessageUtil.sendMessage(sender, "&7- " + reason);
}
```

### ⚡ 性能优化
- 异步读取备份文件
- 批量处理商店恢复
- 主线程仅执行API调用
- 智能的位置检查

## 下一步开发计划

### 如果找到创建API
1. **实现API调用**：
```java
private boolean createShopWithAPI(Location location, ItemStack itemStack, 
                                 double price, ShopType shopType, UUID ownerUUID) {
    // 根据找到的API方法实现具体调用
    Object shopManager = quickShopAPI.getShopManager();
    Method createMethod = shopManagerClass.getMethod("createShop", ...);
    createMethod.invoke(shopManager, location, itemStack, price, shopType, ownerUUID);
}
```

2. **测试和验证**：
   - 创建测试商店
   - 验证商店属性
   - 测试边界情况

### 如果没找到创建API
1. **扩展搜索**：
```java
// 添加更多可能的方法名
String[] createMethodNames = {
    "createShop", "addShop", "create", "add", "newShop", "makeShop",
    "buildShop", "setupShop", "registerShop", "installShop"
};
```

2. **研究替代方案**：
   - 模拟玩家右键创建商店
   - 直接操作QuickShop数据库
   - 使用命令执行方式

## 测试建议

### 准备工作
1. 确保有XRZQQ_QWQ的备份文件
2. 准备一个测试区域
3. 确认管理员权限

### 测试命令
```bash
# 基础功能测试
/st unban XRZQQ_QWQ

# 错误处理测试
/st unban NonExistentPlayer

# 权限测试（普通玩家执行）
/st unban TestPlayer
```

### 预期结果验证
- [ ] API探索日志输出
- [ ] 备份文件读取成功
- [ ] 位置冲突检查工作
- [ ] 错误信息清晰明确
- [ ] 权限检查正常

## 总结

unban功能的框架已经完全实现，包括：
- ✅ 完整的恢复流程
- ✅ 安全检查机制  
- ✅ 错误处理和报告
- ✅ 异步处理架构
- ✅ API探索机制

现在只需要：
1. **测试API探索** - 看看能否找到QuickShop的创建方法
2. **实现具体API调用** - 根据找到的方法完成最后一步
3. **验证功能完整性** - 确保恢复的商店完全正确

让我们开始测试，看看QuickShop为我们提供了什么创建API！🚀
