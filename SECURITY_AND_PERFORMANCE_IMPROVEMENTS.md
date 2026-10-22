# 安全性和性能改进说明

## 改进概述

基于实际测试反馈，我们实施了两项重要的安全性和性能改进，以确保插件在生产环境中的稳定性和安全性。

## 改进内容

### 1. 严格权限管理

#### 问题分析
- 原来`shoptools.use`权限默认为`true`，所有玩家都可以使用
- 可能导致未授权访问和权限管理混乱
- 在大型服务器中容易造成安全隐患

#### 解决方案
**权限配置更新**:
```yaml
permissions:
  shoptools.use:
    description: Allows using ShopTools commands
    default: false  # 改为false，需要手动授权
  shoptools.admin:
    description: Allows using ShopTools admin commands
    default: op
```

#### 实施效果
- ✅ **严格控制**: 只有明确授权的玩家才能使用插件
- ✅ **安全提升**: 避免未授权访问商店信息
- ✅ **管理便利**: 管理员可以精确控制谁能使用插件
- ✅ **向后兼容**: 现有的权限组配置仍然有效

### 2. 命令冷却机制

#### 问题分析
- 玩家可能频繁执行`/st search`和`/st near`命令
- 大量并发查询可能导致服务器性能问题
- 需要防止恶意或无意的命令刷屏

#### 解决方案

**配置文件新增**:
```yaml
# 命令冷却设置
cooldown:
  # 玩家命令冷却时间（秒）
  player-commands: 3
  # 管理员是否绕过冷却限制
  admin-bypass: true
```

**技术实现**:
```java
private final Map<UUID, Long> playerCooldowns = new HashMap<>();

private boolean checkCooldown(Player player) {
    // 管理员绕过冷却限制
    if (player.hasPermission("shoptools.admin") && 
        configManager.getConfig().getBoolean("cooldown.admin-bypass", true)) {
        return true;
    }
    
    UUID playerId = player.getUniqueId();
    long currentTime = System.currentTimeMillis();
    long cooldownSeconds = configManager.getConfig().getLong("cooldown.player-commands", 3);
    long cooldownMillis = cooldownSeconds * 1000;
    
    // 检查是否在冷却时间内
    if (playerCooldowns.containsKey(playerId)) {
        long lastUsed = playerCooldowns.get(playerId);
        long timePassed = currentTime - lastUsed;
        
        if (timePassed < cooldownMillis) {
            // 还在冷却时间内
            long remainingSeconds = (cooldownMillis - timePassed) / 1000 + 1;
            String message = configManager.getMessage("command-cooldown")
                    .replace("{seconds}", String.valueOf(remainingSeconds));
            MessageUtil.sendMessage(player, message);
            return false;
        }
    }
    
    // 更新最后使用时间
    playerCooldowns.put(playerId, currentTime);
    return true;
}
```

#### 功能特性

**冷却时间控制**:
- **默认冷却**: 3秒钟
- **可配置**: 管理员可以调整冷却时间
- **精确计算**: 毫秒级精度，准确的剩余时间显示

**管理员特权**:
- **绕过机制**: 持有`shoptools.admin`权限的用户不受限制
- **可配置**: 可以通过配置关闭管理员绕过功能
- **灵活控制**: 适应不同服务器的管理需求

**内存管理**:
- **自动清理**: 定期清理过期的冷却记录
- **防止泄漏**: 避免长期运行导致的内存问题
- **高效存储**: 使用HashMap实现O(1)查询效率

### 3. 用户体验优化

#### 友好的错误提示

**冷却时间提示**:
```yaml
command-cooldown: "&c命令冷却中，请等待 {seconds} 秒后再试！"
```

**示例输出**:
```
玩家: /st search DIAMOND
系统: 命令冷却中，请等待 2 秒后再试！

玩家: /st search DIAMOND  (3秒后)
系统: === DIAMOND 商店 ===
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      ...
```

#### 权限提示优化

**无权限提示**:
```
玩家: /st search DIAMOND
系统: 你没有权限使用此命令！
```

## 技术实现细节

### 1. 冷却时间检查流程

```java
// 在命令处理前检查冷却时间
if (sender instanceof Player && (subCommand.equals("search") || subCommand.equals("near"))) {
    if (!checkCooldown((Player) sender)) {
        return true; // 冷却时间未到，直接返回
    }
}
```

### 2. 内存优化策略

```java
private void cleanupExpiredCooldowns(long currentTime, long cooldownMillis) {
    playerCooldowns.entrySet().removeIf(entry -> 
        currentTime - entry.getValue() > cooldownMillis * 2); // 保留2倍冷却时间的记录
}
```

### 3. 配置热重载支持

冷却时间配置支持`/st reload`命令热重载，无需重启服务器即可调整设置。

## 性能影响分析

### 1. CPU使用

**冷却检查开销**:
- HashMap查询: O(1)时间复杂度
- 时间计算: 简单的数学运算
- 总体影响: 微乎其微

**内存清理开销**:
- 定期执行，不影响命令响应
- 清理频率合理，避免过度开销

### 2. 内存使用

**冷却记录存储**:
- 每个玩家: UUID(16字节) + Long(8字节) = 24字节
- 1000个活跃玩家: 约24KB内存占用
- 自动清理机制防止无限增长

### 3. 网络流量

**减少无效请求**:
- 冷却机制减少频繁查询
- 降低数据库访问频率
- 减少网络消息发送

## 配置建议

### 1. 权限配置

**推荐权限组设置**:
```yaml
groups:
  default:
    permissions:
      - shoptools.use  # 给予基础玩家使用权限
  
  vip:
    permissions:
      - shoptools.use
  
  admin:
    permissions:
      - shoptools.admin  # 管理员拥有所有权限
```

### 2. 冷却时间调整

**不同服务器类型建议**:
- **小型服务器** (< 50人): 1-2秒
- **中型服务器** (50-200人): 3秒 (默认)
- **大型服务器** (> 200人): 5-10秒

**调整方法**:
```yaml
cooldown:
  player-commands: 5  # 调整为5秒
  admin-bypass: true
```

### 3. 监控建议

**性能监控指标**:
- 命令执行频率
- 冷却触发次数
- 内存使用情况
- 服务器TPS影响

## 部署注意事项

### 1. 权限迁移

**升级前准备**:
1. 备份现有权限配置
2. 确认哪些玩家需要使用插件
3. 准备权限授权计划

**升级后操作**:
1. 为需要的玩家/组授予`shoptools.use`权限
2. 测试权限配置是否正确
3. 通知玩家权限变更

### 2. 配置调优

**初始设置**:
1. 使用默认3秒冷却时间
2. 观察服务器性能表现
3. 根据实际情况调整

**持续优化**:
1. 监控命令使用频率
2. 收集玩家反馈
3. 适时调整冷却时间

## 安全性提升

### 1. 访问控制

- **明确授权**: 只有授权用户才能访问
- **权限分离**: 普通用户和管理员权限清晰分离
- **审计友好**: 便于追踪谁在使用插件

### 2. 滥用防护

- **频率限制**: 防止命令刷屏
- **资源保护**: 避免恶意消耗服务器资源
- **优雅降级**: 冷却期间提供友好提示

### 3. 管理便利

- **灵活配置**: 支持不同场景的配置需求
- **热重载**: 无需重启即可调整设置
- **监控支持**: 便于管理员了解使用情况

## 总结

这两项改进显著提升了ShopTools插件的安全性和性能：

**安全性提升**:
- ✅ 严格的权限控制，防止未授权访问
- ✅ 防滥用机制，保护服务器资源
- ✅ 清晰的权限分离，便于管理

**性能优化**:
- ✅ 命令冷却机制，防止频繁请求
- ✅ 内存管理优化，避免资源泄漏
- ✅ 高效的实现，最小化性能影响

**用户体验**:
- ✅ 友好的错误提示和引导
- ✅ 管理员特权，保证管理便利性
- ✅ 可配置的设置，适应不同需求

这些改进确保了插件在生产环境中的稳定性和安全性，为服务器管理员提供了更好的控制能力，同时保持了良好的用户体验。
