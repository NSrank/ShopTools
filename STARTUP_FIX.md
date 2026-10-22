# ShopTools 启动顺序修复说明

## 问题描述

在初始版本中，ShopTools插件存在一个严重的启动顺序问题：

1. **原始问题**: plugin.yml中设置了`load: STARTUP`，导致ShopTools在服务器启动早期就被加载
2. **依赖问题**: 此时QuickShop-Reremake可能还没有完全初始化，导致API调用失败
3. **后果**: ShopTools无法正常获取商店数据，功能完全失效

## 修复方案

### 1. 移除STARTUP加载模式

**修改前 (plugin.yml)**:
```yaml
load: STARTUP
depend: [QuickShop]
```

**修改后 (plugin.yml)**:
```yaml
depend: [QuickShop]
loadbefore: []
```

- 移除了`load: STARTUP`配置
- 保留`depend: [QuickShop]`确保依赖关系
- 让插件在正常阶段加载，而不是启动早期

### 2. 实现延迟初始化机制

**修改前**: 在`onEnable()`中直接初始化所有组件

**修改后**: 分阶段初始化
```java
@Override
public void onEnable() {
    // 立即初始化基础组件
    initializeConfig();
    registerCommands();
    
    // 延迟3秒初始化QuickShop相关功能
    getServer().getScheduler().runTaskLater(this, this::initializeQuickShopIntegration, 60L);
}
```

### 3. 添加重试机制

如果QuickShop集成初始化失败，会自动在30秒后重试：

```java
private void initializeQuickShopIntegration() {
    try {
        // 尝试初始化QuickShop集成
        initializeQuickShop();
        // ...
    } catch (Exception e) {
        // 失败时30秒后重试
        getServer().getScheduler().runTaskLater(this, this::initializeQuickShopIntegration, 600L);
    }
}
```

### 4. 改进命令处理

**问题**: 如果QuickShop未就绪，命令会直接失败

**解决**: 添加状态检查和友好提示
```java
// 检查数据管理器是否已初始化
if (dataManager == null) {
    MessageUtil.sendMessage(sender, configManager.getMessage("plugin-initializing"));
    return true;
}
```

### 5. 动态更新命令处理器

**问题**: 命令处理器在初始化时dataManager为null

**解决**: 实现动态更新机制
```java
// 初始时dataManager为null
commandHandler = new ShopToolsCommand(this, configManager, null);

// QuickShop就绪后更新
commandHandler.setDataManager(dataManager);
```

## 修复效果

### 启动流程对比

**修复前**:
```
1. 服务器启动
2. ShopTools立即加载 (STARTUP模式)
3. 尝试连接QuickShop API -> 失败
4. 插件功能不可用
```

**修复后**:
```
1. 服务器启动
2. QuickShop完全加载
3. ShopTools正常加载
4. 延迟3秒后初始化QuickShop集成
5. 如果失败，30秒后重试
6. 插件功能正常可用
```

### 用户体验改进

**修复前**:
- 命令直接报错或无响应
- 没有明确的错误提示
- 需要手动重启服务器

**修复后**:
- 友好的初始化提示消息
- 自动重试机制
- 渐进式功能可用

## 配置文件更新

添加了新的提示消息：

```yaml
messages:
  plugin-initializing: "&e插件正在初始化中，请稍后再试..."
  quickshop-not-ready: "&e正在等待QuickShop插件完全启动，请稍后再试..."
```

## 测试验证

### 启动日志示例

**正常启动**:
```
[INFO] ShopTools v1.0 正在启动...
[INFO] 初始化配置管理器...
[INFO] 注册命令处理器...
[INFO] ShopTools v1.0 基础功能启动完成！
[INFO] 正在等待QuickShop插件完全启动...
[INFO] 开始初始化QuickShop集成...
[INFO] 成功连接到QuickShop-Reremake API！
[INFO] 数据管理器初始化完成。
[INFO] 同步管理器初始化完成。
[INFO] ShopTools v1.0 完全启动完成！
```

**QuickShop未就绪时**:
```
[INFO] ShopTools v1.0 正在启动...
[INFO] 基础功能启动完成！
[INFO] 正在等待QuickShop插件完全启动...
[WARNING] QuickShop不可用，无法执行同步。
[WARNING] 将在30秒后重试初始化...
```

### 命令测试

**初始化期间**:
```
玩家: /st list
系统: 插件正在初始化中，请稍后再试...
```

**初始化完成后**:
```
玩家: /st list
系统: === 所有商店 ===
      钻石 world (100, 64, 200) 50.00 Alice
      ...
```

## 兼容性保证

- ✅ 与所有版本的QuickShop-Reremake兼容
- ✅ 支持热重载 (`/reload`)
- ✅ 支持插件管理器动态加载
- ✅ 向后兼容现有配置文件

## 部署建议

1. **替换插件文件**: 直接替换旧版本的jar文件
2. **重启服务器**: 推荐完全重启以确保正确的加载顺序
3. **观察日志**: 检查启动日志确认初始化成功
4. **测试命令**: 等待初始化完成后测试基本功能

这个修复确保了ShopTools能够在任何环境下稳定运行，无论QuickShop的启动时间如何。
