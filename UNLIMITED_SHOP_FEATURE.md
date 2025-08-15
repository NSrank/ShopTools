# 无限商店状态检测功能

## 🎯 功能概述

新增了商店"无限"状态的检测和显示功能，能够自动识别QuickShop中的无限商店（系统商店），并在显示时将其店主名称改为"系统商店"，同时显示商店状态标识。

## 🚀 核心功能

### 1. 无限状态检测

#### 多重检测机制
ShopTools使用多种方法来检测商店是否为无限状态：

1. **API方法检测**：尝试调用 `isUnlimited()` 方法
2. **商店类型检测**：检查商店类型是否包含"UNLIMITED"或"INFINITE"
3. **库存检测**：检查库存是否为-1（通常表示无限）
4. **API方法检测2**：尝试调用 `isUnlimitedShop()` 方法
5. **店主UUID检测**：检查是否为特殊系统UUID

#### 检测代码实现
```java
private boolean checkIfUnlimited(Shop shop) {
    try {
        // 方法1: 尝试调用isUnlimited()方法
        try {
            java.lang.reflect.Method isUnlimitedMethod = shop.getClass().getMethod("isUnlimited");
            Object result = isUnlimitedMethod.invoke(shop);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (Exception ignored) {}
        
        // 方法2: 检查商店类型
        try {
            Object shopType = shop.getShopType();
            if (shopType != null) {
                String shopTypeStr = shopType.toString().toUpperCase();
                if (shopTypeStr.contains("UNLIMITED") || shopTypeStr.contains("INFINITE")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        
        // 方法3: 检查库存是否为-1
        try {
            int stock = shop.getRemainingStock();
            if (stock == -1) {
                return true;
            }
        } catch (Exception ignored) {}
        
        // 更多检测方法...
        
        return false;
    } catch (Exception e) {
        logger.warning("检测商店无限状态时发生错误: " + e.getMessage());
        return false;
    }
}
```

### 2. 数据模型增强

#### ShopData类更新
- **新增字段**：`boolean isUnlimited` - 标识商店是否为无限状态
- **新增方法**：
  - `isUnlimited()` - 获取无限状态
  - `getDisplayOwnerName()` - 获取显示用的店主名称
  - `getStatusDescription()` - 获取商店状态描述

#### 显示逻辑
```java
/**
 * 获取显示用的店主名称
 * 如果是无限商店，返回"系统商店"
 */
public String getDisplayOwnerName() {
    return isUnlimited ? "系统商店" : ownerName;
}

/**
 * 获取商店状态描述
 */
public String getStatusDescription() {
    return isUnlimited ? "无限" : "普通";
}
```

### 3. 显示界面更新

#### 商店信息显示
所有商店列表现在都会显示：
- **店主名称**：无限商店显示为"系统商店"
- **状态标识**：无限商店会在状态后添加"[无限]"标识

#### 显示效果对比

**普通商店**
```
钻石 | 位置: world(100,64,200) | 价格: ¥100.0 | 店主: PlayerName | 状态: 出售
```

**无限商店**
```
钻石 | 位置: world(100,64,200) | 价格: ¥100.0 | 店主: 系统商店 | 状态: 出售 [无限]
```

## 🔧 技术实现

### 1. 数据流程

#### 商店数据读取流程
1. **QuickShop API调用**：从QuickShop获取商店对象
2. **无限状态检测**：使用多重检测机制判断是否为无限商店
3. **店主名称处理**：如果是无限商店，将店主名称设为"系统商店"
4. **ShopData创建**：创建包含无限状态信息的ShopData对象
5. **显示处理**：在界面显示时使用正确的店主名称和状态标识

#### 代码实现
```java
// 在QuickShopIntegration中
private ShopData convertToShopData(Shop shop) {
    // ... 其他数据获取 ...
    
    // 检测商店是否为无限状态
    boolean isUnlimited = checkIfUnlimited(shop);
    
    // 如果是无限商店，将店主名称改为"系统商店"
    String displayOwnerName = isUnlimited ? "系统商店" : ownerName;
    
    // 创建ShopData对象
    return new ShopData(
        shopId, itemId, itemDisplayName, location, price,
        ownerId, displayOwnerName, shopType, stock, isUnlimited, item
    );
}
```

### 2. 兼容性设计

#### 向后兼容
- **数据库兼容**：现有商店数据自动标记为非无限状态
- **API兼容**：使用反射调用，兼容不同版本的QuickShop
- **显示兼容**：普通商店的显示方式保持不变

#### 错误处理
- **API调用失败**：如果检测方法不存在，默认为普通商店
- **数据异常**：完善的异常处理，确保插件稳定运行
- **日志记录**：详细的错误日志，便于问题排查

## 📊 功能特性

### 1. 智能识别
- **多重检测**：使用5种不同的方法检测无限状态
- **容错机制**：单个检测方法失败不影响整体功能
- **版本兼容**：适配不同版本的QuickShop API

### 2. 用户友好
- **清晰标识**：无限商店有明确的"系统商店"标识
- **状态显示**：在商店状态后添加"[无限]"标识
- **一致性**：所有商店列表都使用统一的显示格式

### 3. 性能优化
- **缓存机制**：无限状态信息被缓存在ShopData中
- **高效检测**：检测逻辑优化，避免重复计算
- **内存友好**：只增加一个boolean字段，内存开销极小

## 🎮 使用场景

### 1. 服务器管理
- **系统商店识别**：快速识别哪些是管理员创建的系统商店
- **商店分类**：区分玩家商店和系统商店
- **价格监控**：监控系统商店的价格设置

### 2. 玩家体验
- **信息透明**：玩家可以清楚知道哪些是系统商店
- **购买决策**：帮助玩家做出更好的购买决策
- **信任度**：系统商店通常更可靠，库存充足

### 3. 经济管理
- **市场调节**：系统商店通常用于市场价格调节
- **供需平衡**：无限商店可以稳定市场供应
- **价格基准**：系统商店价格可作为市场价格基准

## ⚠️ 注意事项

### 使用限制
- **QuickShop版本**：需要支持无限商店功能的QuickShop版本
- **权限要求**：无特殊权限要求，所有用户都能看到状态标识
- **性能影响**：检测过程可能有轻微的性能开销

### 配置建议
- **定期更新**：建议定期更新QuickShop以获得最佳兼容性
- **监控日志**：注意检查是否有无限状态检测相关的错误日志
- **测试验证**：在生产环境部署前建议先在测试环境验证

## 🚀 立即体验

### 查看无限商店
```bash
# 搜索商店，查看哪些是系统商店
/st search diamond

# 查看所有商店，观察状态标识
/st page 1

# 搜索附近商店，识别系统商店
/st nearby diamond 100
```

### 预期效果
- 无限商店的店主显示为"系统商店"
- 无限商店的状态后会显示"[无限]"标识
- 普通商店保持原有显示方式不变

## 总结

无限商店状态检测功能为ShopTools增加了重要的商店分类能力：

- ✅ **智能检测**：多重机制确保准确识别无限商店
- ✅ **清晰显示**：系统商店有明确的标识和状态
- ✅ **完全兼容**：向后兼容，不影响现有功能
- ✅ **用户友好**：提供更透明的商店信息

这个功能让玩家能够更好地理解服务器的商店生态，区分玩家商店和系统商店，做出更明智的交易决策！🚀
