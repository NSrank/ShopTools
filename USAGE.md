# ShopTools 使用指南

## 快速开始

### 1. 安装插件
1. 确保服务器已安装 QuickShop-Reremake 插件
2. 将 `ShopTools-1.0-SNAPSHOT.jar` 放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload` 命令

### 2. 首次使用
插件启动后会自动：
- 创建配置文件 `plugins/ShopTools/config.yml`
- 从 QuickShop 同步商店数据到 `plugins/ShopTools/shops.json`
- 在控制台显示加载进度

## 命令详解

### 玩家可用命令

#### 搜索指定物品的所有商店
```
/shoptools search <物品ID> [页码]
/st search DIAMOND        # 搜索所有钻石商店
/st search DIAMOND 2      # 搜索所有钻石商店第2页
```
- 搜索范围：全服所有商店
- 按距离从近到远排序（同世界优先，其他世界最后）
- 格式：`物品名称 坐标 距离 店主 [状态]`
- 距离显示：0-200格显示精确距离，>200格显示"200m+"，其他世界显示"otherworld"
- 支持模糊匹配（如输入 "DIA" 可匹配 "DIAMOND"）
- 智能分页：≤10个商店直接显示，>10个商店自动分页

#### 查看附近的所有商店
```
/shoptools near [页码]
/st near                  # 查看附近所有商店
/st near 2                # 查看附近所有商店第2页
```
- 搜索范围：以玩家为中心200格内
- 按距离从近到远排序
- 格式：`物品名称 坐标 距离 店主 [状态]`
- 智能分页：≤10个商店直接显示，>10个商店自动分页

#### 查看帮助
```
/shoptools help
/st help
```
- 显示所有可用命令
- 根据权限显示不同的命令列表

### 管理员专用命令

#### 分页查看所有商店
```
/shoptools page <页码>
/st page 1
```
- 分页显示服务器上所有商店（每页10个）
- 按物品ID字母序排序（0-9, A-Z）
- 格式：`物品名称 坐标 价格 店主 [状态]`
- 自动显示导航提示（上一页/下一页）
- 需要 `shoptools.admin` 权限

#### 搜索特定物品的商店（支持分页）
```
/shoptools list <物品ID> [页码]
/st list DIAMOND        # 默认显示第1页
/st list DIAMOND 2      # 显示第2页
```
- 显示售卖指定物品的所有商店
- 按价格从低到高排序
- 格式：`物品名称 坐标 价格 店主 [状态]`
- 支持模糊匹配（如输入 "DIA" 可匹配 "DIAMOND"）
- 智能分页：≤10个商店直接显示，>10个商店自动分页
- 需要 `shoptools.admin` 权限

#### 智能查看玩家的商店（支持分页）
```
/shoptools who <玩家名/UUID> [页码]
/st who Steve                    # 完整玩家名，默认第1页
/st who Ste 2                    # 部分玩家名，第2页
/st who 123e4567-e89b-12d3... 1  # UUID查找，第1页
```
- 支持多种查找方式：完整名称、部分名称、UUID
- 智能匹配算法，自动处理多结果情况
- 无匹配时提供相似玩家名建议
- 按商店ID字母序排序
- 智能分页：≤10个商店直接显示，>10个商店自动分页
- 需要 `shoptools.admin` 权限

#### 重新加载配置
```
/shoptools reload
/st reload
```
- 重新加载配置文件和商店数据
- 需要 `shoptools.admin` 权限

#### 重新加载插件
```
/shoptools reload
/st reload
```
- 重新加载配置文件
- 重新同步商店数据
- 需要 `shoptools.admin` 权限

## 权限配置

在权限插件中配置以下权限节点：

```yaml
permissions:
  shoptools.use:
    description: 允许使用基础命令
    default: true
  shoptools.admin:
    description: 允许使用管理员命令
    default: op
```

## 配置文件说明

### config.yml 主要配置项

```yaml
# 调试模式 - 启用后会显示详细的日志信息
debug: false

# 缓存设置
cache:
  size: 1000          # 最大缓存商店数量
  expire-time: 300000 # 缓存过期时间（毫秒）

# 数据同步设置
sync:
  auto: true          # 是否启用自动同步
  interval: 600000    # 同步间隔（毫秒，默认10分钟）

# 消息自定义
messages:
  prefix: "&6[ShopTools] &r"
  no-permission: "&c你没有权限使用此命令！"
  # ... 更多消息配置
```

### 消息颜色代码
支持 Minecraft 标准颜色代码：
- `&0-&9`: 数字颜色
- `&a-&f`: 字母颜色
- `&l`: 粗体
- `&o`: 斜体
- `&r`: 重置格式

## 使用示例

### 场景1：玩家搜索钻石商店（全服搜索）
```
玩家: /st search DIAMOND
系统: === DIAMOND 商店 (第1页/共2页) ===
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      钻石 world (150, 70, 300) 28.7m Bob [出售]
      钻石 world (200, 65, 400) 45.2m Charlie [收购]
      钻石 world (500, 64, 600) 200m+ David [出售]
      钻石 world (800, 64, 900) 200m+ Eve [出售]
      钻石 nether (100, 64, 200) otherworld Frank [出售]
      钻石 end (50, 64, 100) otherworld Grace [出售]
      ...（共10个商店）
      显示第 1-10 个商店，共 15 个
      下一页: /st search DIAMOND 2
```

### 场景1.1：玩家查看附近所有商店
```
玩家: /st near
系统: === 附近的商店 (第1页/共2页) ===
      苹果 world (105, 64, 205) 8.2m Alice [出售]
      钻石 world (100, 64, 200) 15.3m Alice [出售]
      石头 world (120, 64, 220) 22.1m Bob [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 18 个
      下一页: /st near 2
```

### 场景1.2：管理员查看全服钻石商店（少量商店）
```
管理员: /st list DIAMOND
系统: === 物品 DIAMOND 的商店 ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      钻石 world (150, 70, 300) 55.00 Bob [出售]
      钻石 world (200, 65, 400) 60.00 Charlie [收购]
      === 共 3 个商店 ===
```

### 场景1.3：管理员查看热门物品（大量商店）
```
管理员: /st list STONE
系统: === 物品 STONE 的商店 (第1页/共5页) ===
      石头 world (100, 64, 200) 0.50 Alice [出售]
      石头 world (150, 70, 300) 0.55 Bob [出售]
      石头 world (200, 65, 400) 0.60 Charlie [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 47 个
      下一页: /st list STONE 2

管理员: /st list STONE 2
系统: === 物品 STONE 的商店 (第2页/共5页) ===
      石头 world (250, 64, 450) 0.65 David [出售]
      ...
      上一页: /st list STONE 1  下一页: /st list STONE 3
```

### 场景2：管理员智能查找玩家商店
```
管理员: /st who Ali
系统: === 玩家 Alice 的商店 ===
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      石头 world (120, 64, 220) 1.00 Alice [收购]
      苹果 world (140, 64, 240) 5.00 Alice [出售]
      === 共 3 个商店 ===
```

### 场景2.1：多个匹配结果
```
管理员: /st who play
系统: 找到多个匹配 "play" 的玩家:
      请使用更精确的名称重新搜索:
      Player1 (15个商店) - /st who Player1
      Player2 (8个商店) - /st who Player2
      PlayerABC (3个商店) - /st who PlayerABC
```

### 场景2.2：智能建议
```
管理员: /st who Alise
系统: 未找到玩家 Alise 的商店。
      你是否想查找: Alice, Alicia
```

### 场景2.3：大商户分页查看
```
管理员: /st who BigMerchant
系统: === 玩家 BigMerchant 的商店 (第1页/共8页) ===
      钻石 world (100, 64, 200) 50.00 BigMerchant [出售]
      石头 world (120, 64, 220) 1.00 BigMerchant [收购]
      ...（共10个商店）
      显示第 1-10 个商店，共 75 个
      下一页: /st who BigMerchant 2

管理员: /st who BigMerchant 5
系统: === 玩家 BigMerchant 的商店 (第5页/共8页) ===
      ...
      上一页: /st who BigMerchant 4  下一页: /st who BigMerchant 6
```

### 场景3：分页浏览所有商店
```
玩家: /st page 1
系统: === 所有商店 (第1页/共672页) ===
      苹果 world (140, 64, 240) 5.00 Alice [出售]
      钻石 world (100, 64, 200) 50.00 Alice [出售]
      石头 world (120, 64, 220) 1.00 Alice [收购]
      铁锭 world (160, 64, 260) 8.00 Bob [出售]
      金锭 world (180, 64, 280) 25.00 Charlie [双向]
      ...（共10个商店）
      显示第 1-10 个商店，共 6717 个
      下一页: /st page 2

玩家: /st page 2
系统: === 所有商店 (第2页/共672页) ===
      ...
      上一页: /st page 1  下一页: /st page 3
```

## 故障排除

### 常见问题

#### 1. 插件无法启动
- 检查是否安装了 QuickShop-Reremake
- 确认 Java 版本为 17+
- 查看控制台错误信息

#### 2. 商店数据为空
- 确认 QuickShop 中有商店数据
- 检查插件权限配置
- 尝试使用 `/st reload` 重新同步

#### 3. 命令无响应
- 检查玩家是否有 `shoptools.use` 权限
- 确认命令格式正确
- 查看控制台是否有错误信息

#### 4. 数据同步失败
- 检查 QuickShop 插件状态
- 查看 `plugins/ShopTools/shops.json` 文件
- 启用调试模式查看详细日志

### 调试模式
在 `config.yml` 中设置 `debug: true` 可以获得详细的调试信息：
```yaml
debug: true
```

重新加载配置后，控制台会显示：
- 数据同步过程
- API调用详情
- 错误堆栈信息
- 性能统计

## 性能优化建议

1. **合理设置缓存大小**：根据服务器商店数量调整 `cache.size`
2. **调整同步间隔**：商店变化频繁时可缩短 `sync.interval`
3. **定期清理数据**：删除无效商店数据以提升性能
4. **监控内存使用**：大型服务器建议监控插件内存占用

## 技术支持

如遇问题请：
1. 查看控制台完整错误日志
2. 启用调试模式获取详细信息
3. 检查插件版本兼容性
4. 在 GitHub Issues 中报告问题
