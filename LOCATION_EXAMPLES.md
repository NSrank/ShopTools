# ShopTools 位置点功能使用示例

## 快速开始

### 1. 管理员创建位置点

```bash
# 在当前位置创建传送点
/shoptools clocate ~,~,~ 主城传送点 传送

# 在指定坐标创建商店区
/shoptools clocate 100,64,200 主城商业区 商店

# 创建地标建筑
/shoptools clocate 0,100,0 世界中心塔 地标
```

### 2. 玩家查找位置点

```bash
# 查找所有传送点
/shoptools locate 传送

# 查找商店位置
/shoptools locate 商店

# 查看所有可用关键字
/shoptools locate list
```

## 实际使用场景

### 场景1：服务器传送点管理

**管理员设置**：
```bash
# 主城传送点
/shoptools clocate ~,~,~ 主城传送点 传送
/shoptools clocate 500,64,500 副城传送点 传送
/shoptools clocate 1000,64,1000 工业城传送点 传送

# 特殊传送点
/shoptools clocate 0,64,2000 资源世界入口 传送
/shoptools clocate -500,64,-500 PVP竞技场 传送
```

**玩家使用**：
```bash
# 查看所有传送点
/shoptools locate 传送

# 输出示例：
=== 位置点查询结果 (传送) - 第1/1页 ===
1. 主城传送点 - world (0, 64, 0) - 23.5m
2. 副城传送点 - world (500, 64, 500) - 156.2m
3. 工业城传送点 - world (1000, 64, 1000) - 200m+
4. 资源世界入口 - world (0, 64, 2000) - 200m+
5. PVP竞技场 - world (-500, 64, -500) - 200m+
```

### 场景2：商业区管理

**管理员设置**：
```bash
# 各个商业区
/shoptools clocate 200,64,200 主城商业区 商店
/shoptools clocate 800,64,800 副城市场 商店
/shoptools clocate -300,64,300 玩家商店街 商店

# 特殊商店
/shoptools clocate 100,64,100 拍卖行 商店
/shoptools clocate 150,64,150 银行 商店
```

**玩家使用**：
```bash
# 查找购物地点
/shoptools locate 商店

# 输出示例：
=== 位置点查询结果 (商店) - 第1/1页 ===
1. 银行 - world (150, 64, 150) - 45.3m
2. 拍卖行 - world (100, 64, 100) - 67.8m
3. 主城商业区 - world (200, 64, 200) - 89.1m
4. 玩家商店街 - world (-300, 64, 300) - 178.9m
5. 副城市场 - world (800, 64, 800) - 200m+
```

### 场景3：重要建筑标记

**管理员设置**：
```bash
# 公共建筑
/shoptools clocate ~,~,~ 服务器大厅 建筑
/shoptools clocate 300,64,300 图书馆 建筑
/shoptools clocate -200,64,-200 博物馆 建筑

# 功能建筑
/shoptools clocate 400,64,400 附魔台区域 建筑
/shoptools clocate -400,64,-400 炼药室 建筑
```

**玩家使用**：
```bash
# 查找建筑
/shoptools locate 建筑

# 输出示例：
=== 位置点查询结果 (建筑) - 第1/1页 ===
1. 服务器大厅 - world (0, 64, 0) - 12.7m
2. 图书馆 - world (300, 64, 300) - 134.5m
3. 博物馆 - world (-200, 64, -200) - 167.3m
4. 附魔台区域 - world (400, 64, 400) - 200m+
5. 炼药室 - world (-400, 64, -400) - 200m+
```

## 高级使用技巧

### 1. 分层管理

```bash
# 按楼层管理建筑
/shoptools clocate 100,64,100 商店区1层 商店
/shoptools clocate 100,70,100 商店区2层 商店
/shoptools clocate 100,76,100 商店区3层 商店

# 按区域管理
/shoptools clocate 0,64,0 北区传送点 传送
/shoptools clocate 0,64,500 南区传送点 传送
/shoptools clocate 500,64,0 东区传送点 传送
/shoptools clocate -500,64,0 西区传送点 传送
```

### 2. 多关键字系统

```bash
# 同一地点多个关键字
/shoptools clocate 100,64,100 中央广场 地标
/shoptools clocate 100,64,100 活动中心 活动
/shoptools clocate 100,64,100 集合点 传送

# 玩家可以通过不同关键字找到同一地点
/shoptools locate 地标    # 找到中央广场
/shoptools locate 活动    # 找到活动中心（同一位置）
/shoptools locate 传送    # 找到集合点（同一位置）
```

### 3. 分页浏览

```bash
# 当位置点很多时
/shoptools locate 商店     # 查看第1页
/shoptools locate 商店 2   # 查看第2页
/shoptools locate 商店 3   # 查看第3页
```

## 管理建议

### 1. 命名规范

**推荐格式**：
- 传送点：`[区域名]传送点`（如：主城传送点、副城传送点）
- 商店：`[区域名][类型]`（如：主城商业区、玩家市场）
- 建筑：`[功能][类型]`（如：服务器大厅、图书馆）

### 2. 关键字分类

**推荐关键字**：
- `传送` - 各种传送点
- `商店` - 商业区和市场
- `建筑` - 重要建筑物
- `地标` - 地标性位置
- `活动` - 活动场所
- `资源` - 资源采集点
- `服务` - 服务设施

### 3. 定期维护

```bash
# 查看所有关键字
/shoptools locate list

# 检查每个分类的位置点数量
# 删除过时的位置点（需要手动编辑JSON文件）
```

## 故障排除

### 常见错误及解决方案

**错误1**：`无效的坐标格式！`
```bash
# 错误示例
/shoptools clocate 100 64 200 测试点 测试  # ❌ 缺少逗号

# 正确示例
/shoptools clocate 100,64,200 测试点 测试  # ✅ 使用逗号分隔
/shoptools clocate ~,~,~ 测试点 测试       # ✅ 相对坐标
```

**错误2**：`未找到关键字为 'xxx' 的位置点！`
```bash
# 先查看可用关键字
/shoptools locate list

# 使用正确的关键字
/shoptools locate 传送  # 而不是 /shoptools locate 传送点
```

**错误3**：权限不足
```bash
# 确保玩家有正确权限
/lp user <玩家名> permission set shoptools.use true      # 基础权限
/lp user <管理员名> permission set shoptools.admin true  # 管理员权限
```

这个位置点功能为ShopTools增加了强大的位置管理能力，让服务器管理更加便捷！
