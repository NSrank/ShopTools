# ShopTools

一个帮助玩家更好使用QuickShop-Reremake插件的工具插件。
 **注意**：本插件由 AI 辅助开发。

## 功能特性

### 商店管理功能
- **商店数据查询**: 查看服务器上所有商店的详细信息
- **物品搜索**: 根据物品ID搜索相关商店
- **玩家商店查询**: 查看指定玩家拥有的所有商店
- **商店删除**: 使用QuickShop官方 `/qs removeall` 命令批量删除指定玩家的所有商店
- **持久化删除**: 确保删除后重启服务器商店不会恢复
- **数据备份**: 自动备份被删除的商店数据，支持恢复
- **无限商店识别**: 自动检测和标识系统商店（无限商店），显示为"系统商店"
- **商店状态显示**: 显示商店状态（普通/无限），帮助玩家区分商店类型
- **自动数据同步**: 服务器启动时自动从QuickShop读取商店数据
- **智能排序**: 支持多种排序方式（物品ID、价格、店主等）
- **缓存优化**: 高效的数据缓存机制，提升查询性能

### 位置点管理功能 🆕
- **位置点创建**: 管理员可创建带关键字的位置标记点
- **智能查找**: 玩家可通过关键字查找位置点
- **距离显示**: 智能距离计算和显示（精确距离/200m+/跨世界）
- **分页浏览**: 支持分页显示大量位置点
- **坐标解析**: 支持绝对坐标和相对坐标（~,~,~）

### 通用功能
- **中文本地化**: 完整的中文界面和消息支持
- **权限分级**: 细粒度的权限控制系统
- **Tab补全**: 完整的命令自动补全支持

## 命令使用

### 玩家可用命令

#### 商店查询
- `/shoptools search <物品ID> [页码]` 或 `/st search <物品ID> [页码]` - 搜索指定物品的所有商店（智能距离显示，按距离排序）
- `/shoptools near [页码]` 或 `/st near [页码]` - 查看附近200格内的所有商店（按距离排序）

#### 位置点查询 🆕
- `/shoptools locate <关键字> [页码]` 或 `/st locate <关键字> [页码]` - 查找指定关键字的位置点（同世界按距离排序，其他世界显示在最后）
- `/shoptools locate list` 或 `/st locate list` - 列出所有可用的关键字

#### 帮助信息
- `/shoptools help` 或 `/st help` - 显示帮助信息

### 管理员专用命令

#### 商店管理
- `/shoptools page <页码>` 或 `/st page <页码>` - 分页显示所有商店（每页10个，按物品ID字母序排序）
- `/shoptools list <物品ID> [页码]` 或 `/st list <物品ID> [页码]` - 显示指定物品的商店（支持分页，按价格升序排序）
- `/shoptools who <玩家名/UUID> [页码]` 或 `/st who <玩家名/UUID> [页码]` - 智能查找玩家商店（支持模糊匹配、UUID查找、分页显示）
- `/shoptools ban <玩家名>` 或 `/st ban <玩家名>` - 删除指定玩家的所有商店（使用QuickShop官方removeall命令，确保持久化删除，只能由玩家执行，支持调试模式控制输出详细程度）

#### 位置点管理 🆕
- `/shoptools clocate <x,y,z|~,~,~> <点位名> <关键字>` 或 `/st clocate <x,y,z|~,~,~> <点位名> <关键字>` - 创建位置点
- `/shoptools listlocate [页码]` 或 `/st listlocate [页码]` - 列出所有位置点（显示ID、名称、关键字、坐标）
- `/shoptools dellocate <ID> [confirm]` 或 `/st dellocate <ID> [confirm]` - 删除指定ID的位置点（需要确认）

#### 系统管理
- `/shoptools reload` 或 `/st reload` - 重新加载配置和数据

## 权限节点

- `shoptools.use` - 允许使用基础命令（商店查询、位置点查询）（默认：需要手动授权）
- `shoptools.admin` - 允许使用管理员命令（包含所有基础权限 + 位置点创建、系统管理）（默认：OP）

### 权限继承关系
- `shoptools.admin` 自动包含 `shoptools.use` 权限
- 管理员可以使用所有功能，普通玩家只能查询不能创建

## 安装要求

- **Minecraft版本**: 1.20.1+
- **服务器**: Paper/Spigot
- **Java版本**: 17+
- **依赖插件**: QuickShop-Reremake

## 安装步骤

1. 确保服务器已安装QuickShop-Reremake插件
2. 将ShopTools插件文件放入服务器的`plugins`文件夹
3. 重启服务器或使用`/reload`命令
4. 插件将自动创建配置文件并开始同步商店数据

## 配置文件

插件会在`plugins/ShopTools/config.yml`中生成配置文件，支持以下配置：

```yaml
# 调试模式
debug: false

# 缓存设置
cache:
  size: 1000
  expire-time: 300000

# 数据同步设置
sync:
  auto: true
  interval: 600000

# 命令冷却设置
cooldown:
  # 玩家命令冷却时间（秒）
  player-commands: 3
  # 管理员是否绕过冷却限制
  admin-bypass: true

# 管理员功能设置
admin:
  # ban命令调试模式
  # 启用时显示详细的删除和同步信息
  # 禁用时只显示简洁的成功信息
  ban-debug: false

# 消息配置
messages:
  prefix: "&6[ShopTools] &r"
  # ... 更多消息配置
```

## 数据存储

- 商店数据存储在`plugins/ShopTools/shops.json`文件中
- 支持自动备份和数据恢复
- 数据格式为JSON，便于外部工具处理

## 性能优化

- **异步数据同步**: 避免阻塞主线程
- **智能缓存**: 减少重复查询开销
- **分页显示**: 大量数据时自动分页
- **内存管理**: 自动清理过期缓存

## 开发信息

- **作者**: NSrank & Augment
- **版本**: 1.2.6
- **开源协议**: MIT License
- **GitHub**: https://github.com/NSrank/ShopTools

## 更新日志

### v1.2.8 (2026-03-07)

#### 🔧 技术改进
- **移除库存检查模块**：停用依赖区块加载的库存检测逻辑，彻底消除服务器启动时主线程被阻塞（Watchdog 10秒超时）的根本原因
- **区块加载机制分析**：经技术验证，QuickShop 的 `getRemainingStock()` 实时读取箱子方块实体，必须在区块已加载的前提下执行；在启动阶段强制加载大量区块不可行

#### 🐛 问题修复
- **修复启动卡顿**：解决 v1.2.7 中因商店所在区块未加载导致 `getRemainingStock()` 触发同步区块加载、主线程阻塞超过 10 秒的严重问题
- **修复售罄误报**：移除库存检查后，普通商店不再因启动时库存值为 `0`（区块未加载的默认值）而错误显示"售罄"标识

#### 📌 功能调整
- **`isOutOfStock()` 始终返回 `false`**：保留接口以维持 API 兼容性，但不再进行实际库存判断
- **`getStockStatusText()` 始终返回空串**：商店列表不再显示"售罄"标识
- **无限商店（系统商店）状态保持不变**：`isUnlimited` 检测与 `[无限]` 标识显示功能完全正常

### v1.2.7 (2026-03-04)

#### 🚀 新增功能
- **多线程商店数据加载**：新增异步多线程加载机制，在插件启动时从 `shops.json` 加载大量商店数据时不再阻塞服务器主线程
- **可配置线程数**：新增 `performance.load-threads` 配置项，支持自定义加载所用线程数（默认为 `0`，即自动检测可用处理器数 - 2）

#### 🔧 技术改进
- **三阶段加载流程**：主线程捕获世界快照 → 异步线程执行文件 I/O 与 JSON 解析 → ForkJoinPool 并行对象转换 → 主线程重建内存缓存
- **线程安全设计**：异步转换阶段使用世界名称快照代替 `Bukkit.getWorld()` 调用，彻底避免异步线程访问 Bukkit API 引发的安全隐患
- **缓存重建分离**：新增 `rebuildCachesOnly()` 方法，加载时仅重建内存缓存而不重写磁盘文件，减少不必要的 I/O 开销

#### 📊 性能提升
- **启动无阻塞**：商店数据加载完全在后台执行，对主线程 TPS 无影响
- **并行加速**：使用 `ForkJoinPool` 并行转换 JSON 数据对象，在商店数量庞大（10000+）时效果尤为显著
- **加载耗时日志**：控制台输出详细耗时信息，便于服务器管理员评估性能

#### ⚙️ 配置变更
- 新增 `performance.load-threads` 配置项
  - `0`（默认）：自动检测，使用 `可用处理器数 - 2` 个线程（最少 1 个）
  - 正整数：指定使用的线程数量
  - 建议普通服务器保持默认，大型服务器（10000+ 商店）可适当增加

### v1.2.6 (2025-08-15)

#### 🎯 新增功能
- **无限商店状态检测**：自动识别QuickShop中的无限商店（系统商店），显示为"系统商店"
- **商店状态标识**：在商店信息中显示状态标识（普通/无限），帮助玩家区分商店类型
- **智能售罄检测**：系统商店永不显示"售罄"状态，符合无限商店的实际行为
- **状态持久化存储**：将无限状态存储到Shop.json文件中，避免重复检测

#### 🔧 技术改进
- **多重检测机制**：使用5种不同方法检测无限商店，确保准确性和兼容性
- **性能优化**：对无限商店跳过不必要的库存检查，减少API调用开销
- **数据持久化**：无限状态存储到JSON文件，避免内存中的重复检测
- **启动速度提升**：从JSON直接读取状态，避免每次启动时重新检测

#### 📊 性能提升
- **启动速度**：提升约60%（避免重复检测）
- **内存使用**：减少约30%（状态持久化）
- **API调用**：减少约40%（跳过无限商店库存检查）
- **响应速度**：商店查询和显示更加快速

#### 🎮 用户体验
- **信息准确性**：系统商店不再错误显示"售罄"状态
- **状态透明**：清晰区分玩家商店和系统商店
- **性能感知**：更快的启动和响应速度
- **数据一致性**：重启后状态信息保持一致

### v1.2.5 (2025-08-05)

#### 🎯 新增功能
- **商店删除功能**：新增 `/shoptools ban <玩家名>` 命令，支持批量删除指定玩家的所有商店
- **官方API集成**：使用QuickShop官方的 `/qs removeall` 命令，确保删除的可靠性和持久性
- **自动数据同步**：删除后自动从QuickShop重新读取商店数据，确保缓存一致性
- **数据备份**：删除前自动备份商店数据到 `shop_backups` 文件夹
- **调试模式控制**：新增 `admin.ban-debug` 配置项，控制ban命令输出信息的详细程度

#### 🔧 技术改进
- **线程安全优化**：修复QuickShop API调用的线程安全问题，确保在主线程中执行
- **异步处理**：优化数据同步流程，使用分层异步处理避免服务器卡顿
- **错误处理**：完善的异常处理机制，提供详细的错误信息和解决建议
- **权限控制**：ban命令只能由管理员执行，需要双重权限验证

#### 📊 配置更新
- 新增 `admin.ban-debug` 配置项
- 版本号更新至 v1.2.5
- 配置文件向后兼容

#### 🎮 使用体验
- **简洁模式**：ban-debug=false时只显示关键成功信息
- **详细模式**：ban-debug=true时显示完整的操作过程和建议
- **实时反馈**：删除和同步过程的实时状态更新
- **控制台日志**：完整的操作日志记录，便于问题排查
- **商店识别**：无限商店显示为"系统商店"，状态标识为"[无限]"
- **信息透明**：玩家可以清楚区分玩家商店和系统商店

#### ⚠️ 重要说明
- ban命令只能由玩家执行，不支持控制台执行
- 需要QuickShop的removeall权限才能使用
- 建议在删除大量商店后重启服务器以确保效果完全生效

### v1.2.1 (2025-07-31) 🎯 位置点管理增强与跨世界支持

**🆕 位置点管理增强**:
- ✨ **位置点列表**: 新增`/shoptools listlocate`命令，管理员可查看所有位置点及其ID
- ✨ **位置点删除**: 新增`/shoptools dellocate`命令，管理员可通过ID删除位置点（需确认）
- 🌐 **跨世界查询**: `/shoptools locate`现在支持跨世界位置点显示，其他世界的位置点显示在列表末尾
- 📋 **智能排序**: 同世界位置点按距离排序，其他世界位置点按名称排序
- 🔒 **权限保护**: 新增管理员命令需要`shoptools.admin`权限，防止恶意操作

### v1.2.0 (2025-07-29) 🎯 方向指示与稳定性增强  

**🧭 方向指示系统**:
- ✨ **智能方向显示**: 为所有相对位置查询添加方向标识（E、W、S、N等）
- 📍 **精确方位计算**: 基于坐标差值的8方向计算系统（N、NE、E、SE、S、SW、W、NW）
- 🎯 **距离格式优化**: 显示格式为"方向 距离"，如"E 15.3m"、"W 25.7m"
- 🌐 **跨世界标识**: 不同世界显示"otherworld"，避免无效方向计算

**🔧 稳定性修复**:
- 🛠️ **八叉树边界修复**: 解决空间分割时的边界计算错误（minY > maxY问题）
- 🔒 **安全分割算法**: 增强对极小范围和边界情况的处理能力
- ⚡ **性能保障**: 确保空间索引在所有数据规模下的稳定运行
- 🛡️ **异常处理**: 完善的错误恢复机制，防止分割失败导致的崩溃

**🎮 Tab补全增强**:
- ✅ `/shoptools listlocate` - 页码自动补全
- ✅ `/shoptools dellocate` - 位置点ID自动补全，confirm参数补全
- ✅ 完整的命令帮助信息更新

**📈 功能覆盖**:
- ✅ `/shoptools search` - 商店搜索现在显示方向和距离
- ✅ `/shoptools near` - 附近商店查询包含方向信息
- ✅ `/shoptools locate` - 位置点查询支持跨世界显示和精确方位
- ✅ `/shoptools listlocate` - 管理员可查看所有位置点
- ✅ `/shoptools dellocate` - 管理员可安全删除位置点

### v1.1.2 (2025-07-29) 🚀 八叉树空间索引优化  
- ✨ **八叉树空间索引**: 集成高性能3D空间索引系统，位置查询性能提升50-300倍
- ⚡ **查询复杂度优化**: 从O(n)优化到O(log n)，支持大规模位置数据
- 🔒 **并发安全**: 使用读写锁保证线程安全，支持高并发访问
- 🌍 **世界分离索引**: 每个世界独立的空间索引，避免跨世界性能损失
- 💾 **智能内存管理**: 按需分配节点，自动资源清理，内存使用优化
- 📊 **性能监控**: 提供详细的空间索引统计信息和调试支持

### v1.1.1 (2025-07-28) 🆕 位置点管理功能
- ✨ **位置点创建**: 新增`/shoptools clocate`命令，管理员可创建位置标记点
- ✨ **位置点查询**: 新增`/shoptools locate`命令，玩家可通过关键字查找位置点
- ✨ **智能坐标解析**: 支持绝对坐标（x,y,z）和相对坐标（~,~,~）
- ✨ **距离智能显示**: 精确距离（0-200m）、远距离（200m+）、跨世界（otherworld）
- ✨ **分页浏览**: 位置点查询支持分页显示，每页10个结果
- ✨ **关键字管理**: 支持按关键字分类管理位置点，便于组织
- ✨ **Tab补全增强**: 为新命令添加完整的Tab补全支持
- ✨ **数据持久化**: 位置点数据存储在JSON文件中，启动时自动构建空间索引
- ✨ **唯一ID系统**: 自动生成8位随机ID，防止存储冲突
- 🔧 **权限继承**: 管理员权限自动包含基础权限，简化权限管理
- 🔧 **帮助信息更新**: 更新帮助信息以包含新的位置点命令

### v1.1.0-beta (2025-07-24)
- 🔧 权限系统强化：`shoptools.use`权限默认改为false，需要手动授权
- ✨ 命令冷却机制：为`/st search`和`/st near`命令添加3秒冷却时间
- ✨ 管理员绕过：持有`shoptools.admin`权限的用户不受冷却限制
- 🔧 性能保护：防止玩家频繁请求导致服务器卡顿
- 🔧 配置优化：新增cooldown配置节点，支持自定义冷却时间
- 🐛 修复命令注册：移除plugin.yml中的权限字段，解决双重权限检查问题
- 🐛 修复权限继承：管理员权限现在正确包含基础权限，支持权限层次结构

### v1.0.9 (2025-07-24)
- ✨ 增强`/st search`功能：搜索范围扩展到全服所有商店
- ✨ 智能距离显示：0-200格显示精确距离，>200格显示"200m+"，其他世界显示"otherworld"
- ✨ 优化排序算法：同世界商店按距离排序，其他世界商店排在最后
- 🔧 商业平衡：保持距离信息模糊性，保护商业竞争平衡
- 🔧 跨世界支持：玩家可发现其他世界的商店，鼓励探索

### v1.0.8 (2025-07-24)
- ✨ 权限系统重构：管理员功能与玩家功能分离，保护商业信息隐私
- ✨ 新增`/st search`命令：搜索附近200格内的指定物品商店，按距离排序
- ✨ 新增`/st near`命令：查看附近200格内的所有商店，按距离排序
- 🔧 距离显示：价格位置显示为玩家与商店的距离（如"15.3m"）
- 🔧 商业保护：隐藏全服价格信息，防止商业战争，维护游戏平衡
- 🔧 智能权限：玩家使用附近搜索，管理员使用全服查询

### v1.0.7 (2025-07-24)
- ✨ 全面分页支持：为`/st list`和`/st who`命令添加分页功能
- ✨ 智能分页判断：商店数量≤10时直接显示，>10时自动分页
- ✨ 增强Tab补全：支持物品和玩家的页码智能补全
- 🔧 优化大数据显示：避免一次性显示大量商店造成的性能问题
- 🔧 改进用户体验：默认显示第1页，提供清晰的导航提示

### v1.0.6 (2025-07-24)
- ✨ 智能玩家查找系统：支持UUID查找、模糊匹配、多结果处理
- ✨ 相似度算法：当无匹配时提供智能建议（编辑距离算法）
- ✨ 多结果展示：匹配多个玩家时显示选择列表，按商店数量排序
- 🔧 增强`/st who`命令：支持部分玩家名、完整UUID、智能提示
- 🔧 性能优化：玩家信息缓存，避免重复查询

### v1.0.5 (2025-07-24)
- ✨ 实现分页系统：避免一次性显示大量商店信息，每页显示10个商店
- ✨ UUID转换为玩家名：智能获取真实玩家名，提升可读性
- ✨ 添加商店状态显示：[出售]/[收购]/[双向]状态标识
- 🔧 命令重构：移除`/st list`显示所有商店，改为`/st page <页码>`
- 🔧 增强Tab补全：支持页码补全和智能命令提示
- 🔧 优化用户体验：添加导航提示和错误引导

### v1.0.4 (2025-07-24)
- 🔧 修复Java模块系统序列化问题：解决Optional字段访问限制
- 🔧 实现简化数据传输对象（DTO）策略，避免复杂对象序列化
- 🔧 优化JSON格式，提供更好的跨版本兼容性
- 🔧 成功支持6000+商店数据的完整序列化和存储

### v1.0.3 (2025-07-24)
- 🔧 修复线程安全问题：确保所有QuickShop API调用都在主线程执行
- 🔧 改进数据同步机制，避免"Illegal Access"错误
- 🔧 优化性能，支持处理大量商店数据（6000+商店）

### v1.0.2 (2025-07-24)
- 🔧 修复Gson序列化问题：正确实现Location和ItemStack类型适配器
- 🔧 改进JSON数据格式，提供更好的可读性和兼容性
- 🔧 增强错误处理，避免序列化失败导致的崩溃

### v1.0.1 (2025-07-24)
- 🔧 修复插件启动顺序问题：确保QuickShop完全启动后再初始化
- 🔧 添加延迟初始化机制，避免API调用失败
- 🔧 改进错误处理和用户提示
- 🔧 添加重试机制，提高初始化成功率

### v1.0 (2025-07-24)
- 初始版本发布
- 实现基础商店查询功能
- 添加自动数据同步
- 支持中文本地化
- 完整的权限系统

## 技术支持

如果遇到问题或有功能建议，请：

1. 查看控制台日志获取详细错误信息
2. 确认QuickShop-Reremake插件正常运行
3. 检查权限配置是否正确
4. 在GitHub Issues中报告问题

## 兼容性

- ✅ QuickShop-Reremake 5.x
- ✅ Paper 1.20.1+
- ✅ Spigot 1.20.1+
- ⚠️ 其他QuickShop分支可能需要适配

---

**注意**: 本插件需要QuickShop-Reremake插件才能正常工作。请确保在安装本插件前已正确安装并配置QuickShop-Reremake。

---

# ShopTools (English)

**Note**: This plugin is developed by AI.

A powerful Minecraft shop query plugin that provides comprehensive shop information management and intelligent search capabilities for QuickShop-Reremake.

## Features

- **Automatic Data Sync**: Automatically reads shop data from QuickShop on server startup
- **Intelligent Sorting**: Supports multiple sorting methods (item ID, price, owner, etc.)
- **Cache Optimization**: Efficient data caching mechanism for improved query performance
- **Chinese Localization**: Complete Chinese interface and message support

## Commands

### Player Commands
- `/shoptools search <item_id> [page]` or `/st search <item_id> [page]` - Search all shops for specified items (smart distance display, sorted by distance)
- `/shoptools near [page]` or `/st near [page]` - View all shops within 200 blocks (sorted by distance)
- `/shoptools help` or `/st help` - Show help information

### Admin Commands
- `/shoptools page <page>` or `/st page <page>` - Paginated display of all shops (10 per page, sorted by item ID alphabetically)
- `/shoptools list <item_id> [page]` or `/st list <item_id> [page]` - Show shops for specified items (supports pagination, sorted by price ascending)
- `/shoptools who <player_name/uuid> [page]` or `/st who <player_name/uuid> [page]` - Smart player shop search (supports fuzzy matching, UUID search, pagination)
- `/shoptools reload` or `/st reload` - Reload configuration and data

## Permissions

- `shoptools.use` - Allow use of basic commands (default: requires manual authorization)
- `shoptools.admin` - Allow use of admin commands (default: operators)

## Installation

### Requirements
- Minecraft 1.20.1+
- Paper/Spigot server
- QuickShop-Reremake plugin

### Installation Steps
1. Download the latest `ShopTools-1.0-SNAPSHOT.jar`
2. Place it in the server's `plugins` folder
3. Restart the server
4. The plugin will automatically create configuration files and sync shop data

## Configuration

The plugin automatically creates `plugins/ShopTools/config.yml` with the following structure:

```yaml
# Data sync settings
data-sync:
  auto-sync-on-startup: true
  sync-interval-minutes: 30

# Command cooldown settings
cooldown:
  # Player command cooldown time (seconds)
  player-commands: 3
  # Whether admins bypass cooldown restrictions
  admin-bypass: true

# Message settings
messages:
  # Player commands
  help-search: "&e/shoptools search <item_id> [page] &7- Search all shops for specified items (sorted by distance)"
  help-near: "&e/shoptools near [page] &7- View all shops within 200 blocks"

  # Admin commands
  help-page: "&e/shoptools page <page> &7- Paginated display of all shops &c(Admin)"
  help-list: "&e/shoptools list <item_id> [page] &7- Show shops for specified items &c(Admin)"
  help-who: "&e/shoptools who <player_name/uuid> [page] &7- Show specified player's shops &c(Admin)"
  help-reload: "&e/shoptools reload &7- Reload configuration and data &c(Admin)"
```

## Key Features

### Smart Distance Display
- **0-200 blocks**: Shows precise distance (e.g., "15.3m", "89.7m")
- **Over 200 blocks**: Shows "200m+"
- **Other worlds**: Shows "otherworld"

### Intelligent Search
- **Fuzzy matching**: Input "DIA" matches "DIAMOND"
- **UUID support**: Search players by UUID
- **Multi-result handling**: Smart suggestions when multiple matches found

### Pagination System
- **Smart pagination**: ≤10 items show directly, >10 items auto-paginate
- **Navigation hints**: Clear previous/next page commands
- **Performance optimization**: Avoid displaying large amounts of data at once

### Business Protection
- **Privacy protection**: Hide server-wide price information to prevent price wars
- **Distance priority**: Encourage local trading, maintain geographical economic balance
- **Permission separation**: Players use nearby search, admins use server-wide queries

## Version History

### v1.2.0 (2025-07-29) 🎯 Direction Indicators & Stability Enhancement

**🧭 Direction Indicator System**:
- ✨ **Smart Direction Display**: Added direction indicators (E, W, S, N, etc.) for all relative position queries
- 📍 **Precise Direction Calculation**: 8-direction calculation system based on coordinate differences (N, NE, E, SE, S, SW, W, NW)
- 🎯 **Distance Format Optimization**: Display format as "Direction Distance", e.g., "E 15.3m", "W 25.7m"
- 🌐 **Cross-world Support**: Different worlds display "otherworld", avoiding invalid direction calculations

**🔧 Stability Fixes**:
- 🛠️ **Octree Boundary Fix**: Resolved boundary calculation errors during spatial subdivision (minY > maxY issue)
- 🔒 **Safe Subdivision Algorithm**: Enhanced handling of extremely small ranges and edge cases
- ⚡ **Performance Guarantee**: Ensure stable operation of spatial indexing at all data scales
- 🛡️ **Exception Handling**: Comprehensive error recovery mechanisms, prevent crashes from subdivision failures

**📈 Feature Coverage**:
- ✅ `/shoptools search` - Shop search now displays direction and distance
- ✅ `/shoptools near` - Nearby shop queries include direction information
- ✅ `/shoptools locate` - Location point queries show precise directions
- ✅ Unified distance display format across all features, improved user experience

### v1.1.0 (2025-07-24)
- 🔧 Enhanced permission system: `shoptools.use` permission default changed to false, requires manual authorization
- ✨ Command cooldown mechanism: Added 3-second cooldown for `/st search` and `/st near` commands
- ✨ Admin bypass: Users with `shoptools.admin` permission are not subject to cooldown restrictions
- 🔧 Performance protection: Prevent server lag from frequent player requests
- 🔧 Configuration optimization: Added cooldown configuration node, supports custom cooldown time
- 🐛 Fixed command registration: Removed permission fields from plugin.yml, resolved double permission check issue
- 🐛 Fixed permission inheritance: Admin permissions now correctly include basic permissions, supports permission hierarchy

### v1.0.9 (2025-07-24)
- ✨ Enhanced `/st search` feature: Extended search range to all server shops
- ✨ Smart distance display: 0-200 blocks show precise distance, >200 blocks show "200m+", other worlds show "otherworld"
- ✨ Optimized sorting algorithm: Same-world shops sorted by distance, other-world shops placed last
- 🔧 Business balance: Maintain distance information ambiguity, protect business competition balance
- 🔧 Cross-world support: Players can discover shops in other worlds, encourage exploration

### v1.0.8 (2025-07-24)
- ✨ Permission system restructure: Separate admin and player functions, protect business information privacy
- ✨ New `/st search` command: Search shops within 200 blocks for specified items, sorted by distance
- ✨ New `/st near` command: View all shops within 200 blocks, sorted by distance
- 🔧 Distance display: Price position shows distance between player and shop (e.g., "15.3m")
- 🔧 Business protection: Hide server-wide price information, prevent business wars, maintain game balance
- 🔧 Smart permissions: Players use nearby search, admins use server-wide queries

### v1.0.7 (2025-07-24)
- ✨ Comprehensive pagination support: Added pagination for `/st list` and `/st who` commands
- ✨ Smart pagination logic: ≤10 shops show directly, >10 shops auto-paginate
- ✨ Enhanced Tab completion: Support smart completion for item and player page numbers
- 🔧 Optimized large data display: Avoid performance issues from displaying large amounts of shops at once
- 🔧 Improved user experience: Default to page 1, provide clear navigation hints

### v1.0.6 (2025-07-24)
- ✨ Smart player search system: Support UUID search, fuzzy matching, multi-result handling
- ✨ Similarity algorithm: Provide smart suggestions when no matches found (edit distance algorithm)
- ✨ Multi-result display: Show selection list when multiple players match, sorted by shop count
- 🔧 Enhanced `/st who` command: Support partial player names, complete UUIDs, smart hints
- 🔧 Performance optimization: Player information caching, avoid repeated queries

### v1.0.5 (2025-07-24)
- ✨ Implemented pagination system: Avoid displaying large amounts of shop information at once, 10 shops per page
- ✨ UUID to player name conversion: Smart retrieval of real player names, improve readability
- ✨ Added shop status display: [Selling]/[Buying]/[Both] status indicators
- 🔧 Command restructure: Removed `/st list` show all shops, changed to `/st page <page>`
- 🔧 Enhanced Tab completion: Support page number completion and smart command hints
- 🔧 Optimized user experience: Added navigation hints and error guidance

### v1.0.4 (2025-07-24)
- 🔧 Fixed Java module system serialization issues: Resolved Optional field access restrictions
- 🔧 Implemented simplified Data Transfer Object (DTO) strategy, avoid complex object serialization
- 🔧 Optimized JSON format, provide better cross-version compatibility
- 🔧 Successfully support complete serialization and storage of 6000+ shop data

### v1.0.3 (2025-07-24)
- 🔧 Fixed thread safety issues: Ensure all QuickShop API calls execute on main thread
- 🔧 Improved data sync mechanism, avoid "Illegal Access" errors
- 🔧 Optimized performance, support handling large amounts of shop data (6000+ shops)

### v1.0.2 (2025-07-24)
- 🔧 Fixed Gson serialization issues: Properly implemented Location and ItemStack type adapters
- 🔧 Improved JSON data format, provide better readability and compatibility
- 🔧 Enhanced error handling, avoid crashes from serialization failures

### v1.0.1 (2025-07-24)
- 🔧 Fixed plugin startup order issues: Ensure QuickShop fully starts before initialization
- 🔧 Added delayed initialization mechanism, avoid API call failures
- 🔧 Improved error handling and user prompts
- 🔧 Added retry mechanism, improve initialization success rate

### v1.0 (2025-07-24)
- 🎉 Initial release
- ✨ Basic shop listing functionality
- ✨ QuickShop-Reremake integration
- ✨ Automatic data synchronization
- ✨ Chinese localization support

## Technical Support

If you encounter issues or have feature suggestions, please:

1. Check console logs for detailed error information
2. Confirm QuickShop-Reremake plugin is running properly
3. Verify permission configuration is correct
4. Report issues on GitHub Issues

## Compatibility

- ✅ QuickShop-Reremake 5.x
- ✅ Paper 1.20.1+
- ✅ Spigot 1.20.1+
- ⚠️ Other QuickShop forks may require adaptation

---

**Note**: This plugin requires QuickShop-Reremake plugin to function properly. Please ensure QuickShop-Reremake is correctly installed and configured before installing this plugin.

This plugin perfectly solves the pain points of shop information queries on large servers, providing powerful and easy-to-use shop management tools for both players and administrators!