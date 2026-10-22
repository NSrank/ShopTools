# 权限继承问题修复说明

## 问题描述

管理员拥有`shoptools.admin`权限，但无法执行任何命令，所有命令都提示"你没有权限使用此命令！"。

## 问题分析

### 权限检查逻辑问题

**原有的权限检查代码**:
```java
// 检查权限
if (!sender.hasPermission("shoptools.use")) {
    MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
    return true;
}
```

**问题所在**:
- 代码只检查`shoptools.use`权限
- 即使用户拥有更高级的`shoptools.admin`权限，也会被拒绝
- 缺少权限继承机制

### 权限层次结构

**预期的权限层次**:
```
shoptools.admin (管理员权限)
    ├── 包含所有管理员功能
    └── 应该自动包含 shoptools.use 权限

shoptools.use (基础权限)
    └── 包含基础玩家功能
```

**实际的权限检查**:
- 只检查`shoptools.use`
- 不考虑`shoptools.admin`的继承关系

## 解决方案

### 1. 修复权限检查逻辑

**修复前**:
```java
if (!sender.hasPermission("shoptools.use")) {
    MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
    return true;
}
```

**修复后**:
```java
// 检查权限（管理员权限包含基础权限）
if (!sender.hasPermission("shoptools.use") && !sender.hasPermission("shoptools.admin")) {
    MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
    return true;
}
```

**逻辑说明**:
- 如果用户拥有`shoptools.use`权限 → 允许执行
- 如果用户拥有`shoptools.admin`权限 → 也允许执行
- 只有两个权限都没有时 → 才拒绝执行

### 2. 添加权限继承关系

**在plugin.yml中添加权限继承**:
```yaml
permissions:
  shoptools.use:
    description: Allows using ShopTools commands
    default: false
  shoptools.admin:
    description: Allows using ShopTools admin commands
    default: op
    children:
      shoptools.use: true  # 管理员权限自动包含基础权限
```

**继承机制说明**:
- `children`字段定义了权限继承关系
- 拥有`shoptools.admin`权限的用户自动获得`shoptools.use`权限
- 这是Bukkit/Spigot的标准权限继承机制

## 技术实现

### 权限检查流程

**修复后的完整权限检查流程**:

```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // 1. 基础权限检查（支持权限继承）
    if (!sender.hasPermission("shoptools.use") && !sender.hasPermission("shoptools.admin")) {
        MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
        return true;
    }
    
    // 2. 命令解析
    String subCommand = args[0].toLowerCase();
    
    // 3. 玩家命令冷却检查
    if (sender instanceof Player && (subCommand.equals("search") || subCommand.equals("near"))) {
        if (!checkCooldown((Player) sender)) {
            return true;
        }
    }
    
    // 4. 具体命令处理
    switch (subCommand) {
        case "search":
        case "near":
            // 玩家命令，已通过基础权限检查
            break;
        case "page":
        case "list":
        case "who":
        case "reload":
            // 管理员命令，需要额外检查
            if (!sender.hasPermission("shoptools.admin")) {
                MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
                return true;
            }
            break;
    }
    
    // 5. 执行具体命令逻辑
    // ...
}
```

### 权限继承的工作原理

**Bukkit权限系统**:
1. 当插件加载时，Bukkit读取`plugin.yml`中的权限定义
2. 如果权限有`children`字段，会自动建立继承关系
3. 当检查权限时，会递归检查所有父权限

**示例**:
```yaml
permissions:
  parent.permission:
    children:
      child.permission: true
```

如果用户拥有`parent.permission`，那么`hasPermission("child.permission")`也会返回`true`。

## 测试验证

### 测试场景

**场景1: 只有基础权限的用户**
```
权限: shoptools.use
命令: /shoptools search DIAMOND
结果: ✅ 成功执行（通过shoptools.use检查）
```

**场景2: 只有管理员权限的用户**
```
权限: shoptools.admin
命令: /shoptools search DIAMOND
结果: ✅ 成功执行（通过shoptools.admin检查）
```

**场景3: 管理员执行管理员命令**
```
权限: shoptools.admin
命令: /shoptools page 1
结果: ✅ 成功执行（通过两层权限检查）
```

**场景4: 基础用户尝试管理员命令**
```
权限: shoptools.use
命令: /shoptools page 1
结果: ❌ 权限不足（基础权限检查通过，管理员权限检查失败）
```

**场景5: 无权限用户**
```
权限: 无
命令: /shoptools help
结果: ❌ 权限不足（基础权限检查失败）
```

### 权限继承测试

**使用权限插件验证**:
```bash
# 给用户分配管理员权限
/lp user <用户名> permission set shoptools.admin true

# 检查继承的基础权限
/lp user <用户名> permission check shoptools.use
# 应该显示: true (inherited from shoptools.admin)
```

## 部署和配置

### 权限插件配置

**LuckPerms配置示例**:
```bash
# 基础用户组
/lp group default permission set shoptools.use true

# 管理员组（自动继承基础权限）
/lp group admin permission set shoptools.admin true
```

**PermissionsEx配置示例**:
```yaml
groups:
  default:
    permissions:
      - shoptools.use
  
  admin:
    permissions:
      - shoptools.admin
    # shoptools.use 会通过plugin.yml中的继承关系自动获得
```

### 验证权限配置

**检查用户权限**:
```bash
# 检查基础权限
/lp user <用户名> permission check shoptools.use

# 检查管理员权限
/lp user <用户名> permission check shoptools.admin

# 查看用户的所有权限
/lp user <用户名> permission info
```

**检查组权限**:
```bash
# 检查组权限
/lp group <组名> permission info

# 检查权限继承
/lp group <组名> permission check shoptools.use
```

## 最佳实践

### 权限设计原则

1. **权限继承**: 高级权限应该包含低级权限
2. **最小权限**: 用户只获得必要的权限
3. **清晰层次**: 权限层次结构要清晰明了

### 推荐的权限配置

**服务器权限组结构**:
```
Owner (服务器所有者)
├── shoptools.admin
└── shoptools.use (继承)

Admin (管理员)
├── shoptools.admin
└── shoptools.use (继承)

Moderator (版主)
└── shoptools.use

Player (普通玩家)
└── shoptools.use

Guest (访客)
└── 无ShopTools权限
```

### 故障排除

**常见问题及解决方案**:

1. **问题**: 管理员仍然无法使用命令
   **解决**: 检查权限插件是否正确加载了plugin.yml中的权限定义

2. **问题**: 权限继承不生效
   **解决**: 重启服务器，确保权限插件重新读取权限定义

3. **问题**: 某些权限插件不支持继承
   **解决**: 手动给管理员组同时分配两个权限

## 总结

这个修复解决了权限继承的问题：

**修复前**:
- ❌ 管理员权限不包含基础权限
- ❌ 权限检查逻辑不支持继承
- ❌ 需要手动分配多个权限

**修复后**:
- ✅ 管理员权限自动包含基础权限
- ✅ 权限检查支持多级权限验证
- ✅ 符合Bukkit权限系统最佳实践
- ✅ 简化了权限管理工作

现在管理员只需要分配`shoptools.admin`权限，就可以使用所有功能，包括基础的玩家命令和高级的管理员命令。
