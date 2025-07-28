# ShopTools 位置点功能说明

## 功能概述

ShopTools v1.1 新增了位置点管理功能，允许管理员创建位置标记点，玩家可以通过关键字查找这些位置点。这个功能非常适合标记重要地点，如传送点、商业区、建筑等。

## 命令说明

### 管理员命令

#### `/shoptools clocate <坐标> <点位名> <关键字>`

创建新的位置点。

**权限要求**: `shoptools.admin`

**参数说明**:
- `<坐标>`: 位置坐标，支持两种格式：
  - 绝对坐标: `x,y,z` (如: `100,64,200`)
  - 相对坐标: `~,~,~` (使用当前位置)
- `<点位名>`: 位置点的名称
- `<关键字>`: 用于分类的关键字

**使用示例**:
```
/shoptools clocate ~,~,~ 主城传送点 传送
/shoptools clocate 100,64,200 商业区入口 商店
/shoptools clocate 0,100,0 世界中心 地标
```

**功能特性**:
- 自动生成唯一ID防止存储冲突
- 检查同一关键字下的名称重复
- 支持相对坐标（`~,~,~`）使用当前位置
- 记录创建者和创建时间

### 玩家命令

#### `/shoptools locate <关键字> [页码]`

查找指定关键字的位置点。

**权限要求**: `shoptools.use`

**参数说明**:
- `<关键字>`: 要查找的关键字
- `[页码]`: 可选，指定页码（默认第1页）

**使用示例**:
```
/shoptools locate 传送
/shoptools locate 商店 2
/shoptools locate 地标
```

#### `/shoptools locate list`

列出所有可用的关键字。

**使用示例**:
```
/shoptools locate list
```

**输出示例**:
```
=== 可用关键字 ===
- 传送 (3个位置点)
- 商店 (5个位置点)
- 地标 (2个位置点)
```

## 显示格式

### 位置点查询结果

```
=== 位置点查询结果 (传送) - 第1/2页 ===
1. 主城传送点 - world (0, 64, 0) - 45.2m
2. 副城传送点 - world (200, 64, 300) - 156.8m
3. 末地传送点 - world_the_end (100, 50, 0) - otherworld
4. 远程传送点 - world (1000, 64, 1000) - 200m+
使用 /shoptools locate 传送 2 查看下一页
```

### 距离显示规则

- **精确距离**: 0-200米显示具体距离（如: `45.2m`）
- **远距离**: 超过200米显示 `200m+`
- **跨世界**: 不同世界显示 `otherworld`

## 数据存储

### 存储位置

位置点数据存储在 `plugins/ShopTools/locations.json` 文件中。

### 数据结构

```json
{
  "abc12345": {
    "id": "abc12345",
    "name": "主城传送点",
    "keyword": "传送",
    "worldName": "world",
    "x": 0.0,
    "y": 64.0,
    "z": 0.0,
    "createdTime": 1627123456789,
    "createdBy": "admin"
  }
}
```

### 数据字段说明

- `id`: 8位随机生成的唯一标识符
- `name`: 位置点名称
- `keyword`: 分类关键字
- `worldName`: 世界名称
- `x`, `y`, `z`: 坐标
- `createdTime`: 创建时间戳
- `createdBy`: 创建者名称

## Tab补全功能

### clocate命令补全

```
/shoptools clocate <TAB>
→ ~,~,~
→ 100,64,200 (当前位置坐标)
```

### locate命令补全

```
/shoptools locate <TAB>
→ list
→ 传送
→ 商店
→ 地标

/shoptools locate 传送 <TAB>
→ 1
→ 2
→ 3
```

## 使用场景

### 服务器管理

**传送点管理**:
```bash
/shoptools clocate ~,~,~ 主城传送 传送
/shoptools clocate 1000,64,1000 副城传送 传送
/shoptools clocate 0,64,2000 资源世界传送 传送
```

**商业区标记**:
```bash
/shoptools clocate ~,~,~ 主城商业区 商店
/shoptools clocate 500,64,500 工业区市场 商店
/shoptools clocate -200,64,300 玩家商店街 商店
```

**重要建筑标记**:
```bash
/shoptools clocate ~,~,~ 服务器大厅 建筑
/shoptools clocate 200,100,200 图书馆 建筑
/shoptools clocate -100,64,-100 竞技场 建筑
```

### 玩家使用

**查找传送点**:
```bash
/shoptools locate 传送
# 显示所有传送点，按距离排序
```

**查找商店**:
```bash
/shoptools locate 商店
# 显示所有商店位置，方便购物
```

**浏览所有类别**:
```bash
/shoptools locate list
# 查看所有可用的位置类别
```

## 权限配置

### 基础权限

```yaml
# 玩家权限 - 查看位置点
shoptools.use: true

# 管理员权限 - 创建位置点
shoptools.admin: true
```

### LuckPerms配置示例

```bash
# 给普通玩家组添加查看权限
/lp group default permission set shoptools.use true

# 给管理员组添加创建权限
/lp group admin permission set shoptools.admin true
```

## 最佳实践

### 命名规范

**推荐的关键字**:
- `传送` - 各种传送点
- `商店` - 商业区和市场
- `建筑` - 重要建筑物
- `地标` - 地标性位置
- `资源` - 资源采集点
- `活动` - 活动场所

**推荐的命名格式**:
- 传送点: `主城传送点`、`副城传送点`
- 商店: `主城商业区`、`玩家市场`
- 建筑: `服务器大厅`、`图书馆`

### 管理建议

1. **分类明确**: 使用清晰的关键字分类
2. **命名规范**: 采用统一的命名格式
3. **定期维护**: 删除过时的位置点
4. **权限控制**: 只给信任的管理员创建权限

### 性能考虑

- 位置点数据在内存中缓存，查询速度快
- 支持大量位置点存储
- 自动按距离排序，用户体验好
- JSON格式存储，便于备份和迁移

## 故障排除

### 常见问题

**问题**: 无法创建位置点
**解决**: 检查是否有 `shoptools.admin` 权限

**问题**: 坐标格式错误
**解决**: 使用 `x,y,z` 或 `~,~,~` 格式，注意逗号分隔

**问题**: 名称重复
**解决**: 在同一关键字下使用不同的名称

**问题**: 查找不到位置点
**解决**: 使用 `/shoptools locate list` 查看可用关键字

### 数据备份

定期备份 `plugins/ShopTools/locations.json` 文件：

```bash
cp plugins/ShopTools/locations.json backups/locations_$(date +%Y%m%d).json
```

## 更新日志

### v1.1.0 新增功能

- ✨ 位置点创建功能 (`/shoptools clocate`)
- ✨ 位置点查询功能 (`/shoptools locate`)
- ✨ 智能距离显示和排序
- ✨ 完整的Tab补全支持
- ✨ JSON数据持久化存储
- ✨ 权限分级管理

这个功能为ShopTools增加了强大的位置管理能力，让服务器管理更加便捷，玩家导航更加方便！
