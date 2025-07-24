# ShopTools

一个帮助玩家更好使用QuickShop-Reremake插件的工具插件。
 **注意**：本插件由 AI 开发。

## 功能特性

- **商店数据查询**: 查看服务器上所有商店的详细信息
- **物品搜索**: 根据物品ID搜索相关商店
- **玩家商店查询**: 查看指定玩家拥有的所有商店
- **自动数据同步**: 服务器启动时自动从QuickShop读取商店数据
- **智能排序**: 支持多种排序方式（物品ID、价格、店主等）
- **缓存优化**: 高效的数据缓存机制，提升查询性能
- **中文本地化**: 完整的中文界面和消息支持

## 命令使用

### 玩家可用命令
- `/shoptools search <物品ID> [页码]` 或 `/st search <物品ID> [页码]` - 搜索指定物品的所有商店（智能距离显示，按距离排序）
- `/shoptools near [页码]` 或 `/st near [页码]` - 查看附近200格内的所有商店（按距离排序）
- `/shoptools help` 或 `/st help` - 显示帮助信息

### 管理员专用命令
- `/shoptools page <页码>` 或 `/st page <页码>` - 分页显示所有商店（每页10个，按物品ID字母序排序）
- `/shoptools list <物品ID> [页码]` 或 `/st list <物品ID> [页码]` - 显示指定物品的商店（支持分页，按价格升序排序）
- `/shoptools who <玩家名/UUID> [页码]` 或 `/st who <玩家名/UUID> [页码]` - 智能查找玩家商店（支持模糊匹配、UUID查找、分页显示）
- `/shoptools reload` 或 `/st reload` - 重新加载配置和数据

## 权限节点

- `shoptools.use` - 允许使用基础命令（默认：需要手动授权）
- `shoptools.admin` - 允许使用管理员命令（默认：OP）

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
- **版本**: 1.1
- **开源协议**: MIT License
- **GitHub**: https://github.com/NSrank/ShopTools

## 更新日志

### v1.1.0 (2025-07-24)
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
