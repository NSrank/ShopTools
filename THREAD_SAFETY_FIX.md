# 线程安全问题修复说明

## 问题描述

在修复了Gson序列化问题后，发现了一个新的线程安全问题：

```
[WARN]: [ShopTools] 转换商店数据时发生错误: #[Illegal Access] This method require runs on server main thread.
```

## 根本原因

**问题分析**:
1. QuickShop-Reremake的API调用必须在Minecraft服务器的主线程中执行
2. 我们的数据同步代码使用了`CompletableFuture.runAsync()`在异步线程中执行
3. 当异步线程尝试调用QuickShop API时，触发了线程安全检查

**错误的实现**:
```java
// 错误：在异步线程中调用QuickShop API
CompletableFuture.runAsync(() -> {
    syncShopData(); // 这里会调用QuickShop API
});
```

## Bukkit/Spigot线程模型

### 主线程 vs 异步线程

**主线程 (Server Thread)**:
- 处理游戏逻辑、玩家交互、世界更新
- 所有Bukkit API调用必须在主线程执行
- 包括：玩家操作、方块操作、实体操作、插件API调用

**异步线程 (Async Thread)**:
- 用于I/O操作、网络请求、数据库查询
- 不能直接调用Bukkit API
- 需要通过调度器切换回主线程

### QuickShop API的线程要求

QuickShop-Reremake为了保证数据一致性，要求所有API调用都在主线程执行：
- `getAllShops()` - 必须主线程
- `getShopManager()` - 必须主线程
- 商店对象的所有方法 - 必须主线程

## 修复方案

### 1. 初始同步修复

**修复前**:
```java
// 异步执行同步操作
CompletableFuture.runAsync(() -> {
    try {
        syncShopData(); // 错误：异步调用QuickShop API
        isInitialSyncCompleted = true;
    } catch (Exception e) {
        logger.severe("初始数据同步失败: " + e.getMessage());
    }
});
```

**修复后**:
```java
// 在主线程执行同步操作
Bukkit.getScheduler().runTask(plugin, () -> {
    try {
        syncShopData(); // 正确：主线程调用QuickShop API
        isInitialSyncCompleted = true;
    } catch (Exception e) {
        logger.severe("初始数据同步失败: " + e.getMessage());
    }
});
```

### 2. 定期同步修复

**修复前**:
```java
syncTask = new BukkitRunnable() {
    @Override
    public void run() {
        if (!isSyncing && quickShopIntegration.isQuickShopAvailable()) {
            // 错误：在定时任务中再次创建异步任务
            CompletableFuture.runAsync(() -> {
                syncShopData();
            });
        }
    }
}.runTaskTimer(plugin, interval, interval);
```

**修复后**:
```java
syncTask = new BukkitRunnable() {
    @Override
    public void run() {
        if (!isSyncing && quickShopIntegration.isQuickShopAvailable()) {
            // 正确：直接在主线程执行
            try {
                syncShopData();
            } catch (Exception e) {
                logger.warning("定期数据同步失败: " + e.getMessage());
            }
        }
    }
}.runTaskTimer(plugin, interval, interval);
```

### 3. 手动同步修复

**修复后**:
```java
public boolean performManualSync() {
    // 检查是否在主线程
    if (Bukkit.isPrimaryThread()) {
        // 已经在主线程，直接执行
        try {
            syncShopData();
            return true;
        } catch (Exception e) {
            logger.severe("手动数据同步失败: " + e.getMessage());
            return false;
        }
    } else {
        // 不在主线程，切换到主线程执行
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                syncShopData();
            } catch (Exception e) {
                logger.severe("手动数据同步失败: " + e.getMessage());
            }
        });
        return true;
    }
}
```

## 性能考虑

### 主线程执行的影响

**优点**:
- ✅ 线程安全，避免并发问题
- ✅ 可以安全调用所有Bukkit API
- ✅ 数据一致性得到保证

**缺点**:
- ⚠️ 大量数据处理可能影响服务器TPS
- ⚠️ 同步操作会阻塞主线程

### 优化策略

1. **分批处理**:
```java
// 将大量商店数据分批处理
List<Shop> allShops = quickShopAPI.getShopManager().getAllShops();
int batchSize = 100;

for (int i = 0; i < allShops.size(); i += batchSize) {
    int end = Math.min(i + batchSize, allShops.size());
    List<Shop> batch = allShops.subList(i, end);
    
    // 处理一批数据后让出CPU时间
    if (i > 0) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            processBatch(batch);
        }, 1L);
    } else {
        processBatch(batch);
    }
}
```

2. **异步I/O操作**:
```java
// 在主线程获取数据，异步保存到文件
List<ShopData> shopData = getAllShopsFromQuickShop(); // 主线程

CompletableFuture.runAsync(() -> {
    saveToFile(shopData); // 异步I/O
});
```

## 最佳实践

### 1. 线程检查
```java
if (!Bukkit.isPrimaryThread()) {
    throw new IllegalStateException("This method must be called from the main thread");
}
```

### 2. 安全的异步调用
```java
// 错误
CompletableFuture.runAsync(() -> {
    player.sendMessage("Hello"); // 错误：异步调用Bukkit API
});

// 正确
CompletableFuture.runAsync(() -> {
    String data = fetchDataFromDatabase(); // 异步I/O
    
    Bukkit.getScheduler().runTask(plugin, () -> {
        player.sendMessage(data); // 主线程调用Bukkit API
    });
});
```

### 3. 调度器使用
```java
// 立即在主线程执行
Bukkit.getScheduler().runTask(plugin, runnable);

// 延迟在主线程执行
Bukkit.getScheduler().runTaskLater(plugin, runnable, 20L); // 1秒后

// 定期在主线程执行
Bukkit.getScheduler().runTaskTimer(plugin, runnable, 0L, 20L); // 每秒执行

// 异步执行（不能调用Bukkit API）
Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
```

## 测试验证

### 修复前的错误日志
```
[WARN]: [ShopTools] 转换商店数据时发生错误: #[Illegal Access] This method require runs on server main thread.
```

### 修复后的正常日志
```
[INFO]: [ShopTools] 开始处理 6717 个商店数据...
[INFO]: [ShopTools] 成功处理了 6717 个商店数据。
[INFO]: [ShopTools] 数据同步完成，耗时: 1250ms，同步了 6717 个商店。
```

## 部署说明

这个修复确保了：
- ✅ 所有QuickShop API调用都在主线程执行
- ✅ 数据同步操作线程安全
- ✅ 避免了并发访问问题
- ✅ 保持了良好的性能表现

修复后的插件现在能够正确处理大量商店数据而不会触发线程安全错误。
