# ShopTools v1.2.0 位置点管理增强功能说明

## 新增功能概述

本次更新为ShopTools的位置点管理系统添加了三个重要的增强功能：

1. **跨世界位置点查询支持**
2. **位置点列表管理功能**
3. **位置点删除功能**

## 功能详细说明

### 1. 跨世界位置点查询 (`/shoptools locate`)

#### 功能描述
- 现在`/shoptools locate <关键字>`命令支持显示所有世界的位置点
- 同世界的位置点按距离排序显示在前面
- 其他世界的位置点按名称排序显示在后面
- 其他世界的位置点距离显示为"otherworld"

#### 使用示例
```bash
/shoptools locate 传送
```

#### 输出示例
```
=== 位置点查询结果 (传送) - 第1/1页 ===
1. 主城传送点 - world (0, 64, 0) - E 45.2m
2. 副城传送点 - world (200, 64, 300) - S 156.8m
3. 末地传送点 - world_the_end (100, 50, 0) - otherworld
4. 下界传送点 - world_nether (-50, 64, -50) - otherworld
```

### 2. 位置点列表功能 (`/shoptools listlocate`)

#### 功能描述
- 管理员专用命令，需要`shoptools.admin`权限
- 显示所有位置点的详细信息，包括ID、名称、关键字、坐标
- 支持分页显示，每页10个位置点
- 按关键字和名称排序

#### 使用语法
```bash
/shoptools listlocate [页码]
```

#### 使用示例
```bash
# 查看第一页
/shoptools listlocate

# 查看第二页
/shoptools listlocate 2
```

#### 输出示例
```
=== 所有位置点列表 - 第1/2页 ===
1. 主城传送点 [传送] - world (0, 64, 0) - ID: a1b2c3d4
2. 副城传送点 [传送] - world (200, 64, 300) - ID: e5f6g7h8
3. 末地传送点 [传送] - world_the_end (100, 50, 0) - ID: i9j0k1l2
4. 主城商业区 [商店] - world (150, 64, 150) - ID: m3n4o5p6
5. 工业区市场 [商店] - world (500, 64, 500) - ID: q7r8s9t0
使用 /shoptools listlocate 2 查看下一页
总计: 12 个位置点
```

### 3. 位置点删除功能 (`/shoptools dellocate`)

#### 功能描述
- 管理员专用命令，需要`shoptools.admin`权限
- 通过位置点ID删除指定的位置点
- 需要二次确认，防止误删除
- 删除后会从内存和空间索引中移除

#### 使用语法
```bash
/shoptools dellocate <ID> [confirm]
```

#### 使用示例
```bash
# 第一步：查看要删除的位置点信息
/shoptools dellocate a1b2c3d4

# 第二步：确认删除
/shoptools dellocate a1b2c3d4 confirm
```

#### 输出示例
```bash
# 第一次执行（显示确认信息）
确认要删除以下位置点吗？
名称: 主城传送点
关键字: 传送
位置: world (0, 64, 0)
ID: a1b2c3d4
请再次执行命令确认删除: /shoptools dellocate a1b2c3d4 confirm

# 第二次执行（确认删除）
成功删除位置点！
ID: a1b2c3d4
名称: 主城传送点
关键字: 传送
位置: world (0, 64, 0)
位置点删除成功！
```

## Tab补全支持

### 新增补全功能

1. **listlocate命令补全**
   - 自动补全页码（基于实际位置点数量）

2. **dellocate命令补全**
   - 第二个参数：自动补全所有位置点的ID
   - 第三个参数：自动补全"confirm"

### 补全示例
```bash
/shoptools listlocate <TAB>
→ 1, 2, 3, ...

/shoptools dellocate <TAB>
→ a1b2c3d4, e5f6g7h8, i9j0k1l2, ...

/shoptools dellocate a1b2c3d4 <TAB>
→ confirm
```

## 权限要求

### 新增权限控制
- `shoptools.admin` - 使用listlocate和dellocate命令的必需权限
- 普通玩家（仅有`shoptools.use`权限）无法使用管理员命令
- 防止恶意玩家删除或查看敏感位置点信息

### 权限配置示例
```yaml
# LuckPerms配置
/lp group admin permission set shoptools.admin true
/lp group default permission set shoptools.use true
```

## 使用场景

### 服务器管理场景

1. **位置点维护**
   ```bash
   # 查看所有位置点
   /shoptools listlocate
   
   # 删除过时的位置点
   /shoptools dellocate old_id_here confirm
   ```

2. **跨世界传送点管理**
   ```bash
   # 查看所有传送点（包括其他世界）
   /shoptools locate 传送
   
   # 列出所有位置点进行整理
   /shoptools listlocate
   ```

3. **位置点清理**
   ```bash
   # 查找要删除的位置点ID
   /shoptools listlocate
   
   # 安全删除位置点
   /shoptools dellocate unwanted_id confirm
   ```

## 技术实现

### 跨世界查询优化
- 分离同世界和其他世界的位置点
- 同世界位置点使用距离排序
- 其他世界位置点使用名称排序
- 合并结果时保持正确的显示顺序

### 数据一致性保证
- 删除操作同时更新内存缓存和空间索引
- 确保数据持久化到JSON文件
- 维护位置点ID的唯一性

### 安全性考虑
- 严格的权限检查
- 二次确认机制防止误删除
- 详细的操作日志记录

## 兼容性说明

- 完全向后兼容现有的位置点功能
- 不影响现有的`/shoptools locate`和`/shoptools clocate`命令
- 新功能仅对有管理员权限的用户可见
- 支持所有现有的Tab补全功能

## 更新建议

建议服务器管理员在更新后：

1. 测试新的跨世界查询功能
2. 使用`/shoptools listlocate`检查现有位置点
3. 清理不需要的位置点
4. 为管理员团队培训新的管理命令使用方法
