# 命令注册问题修复说明

## 问题描述

在实施严格权限管理后，发现持有正确权限的玩家仍然无法使用`/shoptools`命令，提示权限不足。

## 问题分析

### 根本原因

在`plugin.yml`中，命令定义包含了`permission`字段：

```yaml
commands:
  shoptools:
    description: ShopTools main command
    usage: /<command> [args...]
    aliases: [st]
    permission: shoptools.use  # 这里导致了问题
  st:
    description: ShopTools short command
    usage: /<command> [args...]
    permission: shoptools.use  # 这里也导致了问题
```

### 双重权限检查问题

1. **Bukkit层面检查**: `plugin.yml`中的`permission`字段使得Bukkit在命令执行前就检查权限
2. **代码层面检查**: 在`onCommand`方法中又进行了一次权限检查

```java
// 代码中的权限检查
if (!sender.hasPermission("shoptools.use")) {
    MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
    return true;
}
```

### 问题流程

1. 玩家执行`/shoptools`命令
2. Bukkit检查`plugin.yml`中的`permission: shoptools.use`
3. 如果玩家没有权限，命令根本不会传递到`onCommand`方法
4. 玩家看到系统默认的权限错误消息，而不是我们自定义的消息

## 解决方案

### 修复方法

移除`plugin.yml`中的`permission`字段，让权限检查完全在代码中处理：

```yaml
commands:
  shoptools:
    description: ShopTools main command
    usage: /<command> [search <物品ID>|near|page <页码>|list <物品ID>|who <玩家名>] [页码]
    aliases: [st]
    # 移除了 permission: shoptools.use
  st:
    description: ShopTools short command
    usage: /<command> [search <物品ID>|near|page <页码>|list <物品ID>|who <玩家名>] [页码]
    # 移除了 permission: shoptools.use
```

### 权限检查流程

修复后的权限检查流程：

1. 玩家执行`/shoptools`命令
2. Bukkit直接将命令传递到`onCommand`方法
3. 代码中检查`shoptools.use`权限
4. 如果没有权限，显示自定义的错误消息
5. 如果有权限，继续执行命令逻辑

## 技术细节

### Bukkit命令权限机制

**plugin.yml中的permission字段**:
- 在命令执行前进行权限检查
- 如果检查失败，命令不会传递到插件
- 使用系统默认的权限错误消息

**代码中的权限检查**:
- 在`onCommand`方法中手动检查
- 可以自定义错误消息和处理逻辑
- 更灵活的权限控制

### 最佳实践

**推荐做法**:
```yaml
commands:
  mycommand:
    description: My command
    usage: /<command> [args...]
    # 不设置permission字段，在代码中处理
```

```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // 在代码中检查权限
    if (!sender.hasPermission("myplugin.use")) {
        sender.sendMessage("自定义的权限错误消息");
        return true;
    }
    
    // 命令逻辑...
    return true;
}
```

**避免的做法**:
```yaml
commands:
  mycommand:
    description: My command
    usage: /<command> [args...]
    permission: myplugin.use  # 避免在这里设置权限
```

## 修复验证

### 测试场景

1. **无权限玩家**:
   ```
   玩家: /shoptools help
   系统: 你没有权限使用此命令！  (自定义消息)
   ```

2. **有权限玩家**:
   ```
   玩家: /shoptools help
   系统: === ShopTools 帮助 ===
         /shoptools search <物品ID> [页码] - 搜索指定物品的所有商店
         ...
   ```

3. **管理员**:
   ```
   管理员: /shoptools help
   系统: === ShopTools 帮助 ===
         /shoptools search <物品ID> [页码] - 搜索指定物品的所有商店
         /shoptools page <页码> - 分页显示所有商店 (管理员)
         ...
   ```

### 权限配置示例

**LuckPerms配置**:
```
/lp group default permission set shoptools.use true
/lp group admin permission set shoptools.admin true
```

**PermissionsEx配置**:
```yaml
groups:
  default:
    permissions:
      - shoptools.use
  admin:
    permissions:
      - shoptools.admin
```

## 相关改进

### 错误消息优化

现在所有权限相关的错误消息都使用自定义配置：

```yaml
messages:
  no-permission: "&c你没有权限使用此命令！"
  command-cooldown: "&c命令冷却中，请等待 {seconds} 秒后再试！"
```

### 权限检查层次

1. **基础权限检查**: `shoptools.use`
2. **管理员权限检查**: `shoptools.admin`
3. **冷却时间检查**: 管理员可绕过

```java
// 基础权限检查
if (!sender.hasPermission("shoptools.use")) {
    MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
    return true;
}

// 管理员命令权限检查
if (!sender.hasPermission("shoptools.admin")) {
    MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
    return true;
}

// 冷却时间检查（管理员可绕过）
if (sender instanceof Player && !checkCooldown((Player) sender)) {
    return true;
}
```

## 部署注意事项

### 升级步骤

1. **备份现有插件**
2. **替换新版本插件**
3. **重启服务器**
4. **测试权限配置**

### 权限配置检查

确保权限插件中正确配置了`shoptools.use`权限：

```bash
# 检查玩家权限
/lp user <玩家名> permission check shoptools.use

# 检查组权限
/lp group <组名> permission check shoptools.use
```

### 常见问题排查

**问题**: 玩家仍然无法使用命令
**排查步骤**:
1. 检查权限插件配置
2. 确认权限继承关系
3. 检查插件加载顺序
4. 查看服务器日志

**问题**: 管理员无法使用管理员命令
**排查步骤**:
1. 确认拥有`shoptools.admin`权限
2. 检查权限插件中的OP权限设置
3. 验证权限继承配置

## 总结

这个修复解决了命令注册中的双重权限检查问题：

**修复前**:
- ❌ plugin.yml和代码中都检查权限
- ❌ 权限错误时显示系统默认消息
- ❌ 无法自定义权限处理逻辑

**修复后**:
- ✅ 只在代码中检查权限
- ✅ 显示自定义的权限错误消息
- ✅ 灵活的权限控制和错误处理
- ✅ 支持多层次权限检查

这个修复确保了权限系统的正确工作，为后续的功能扩展提供了坚实的基础。
