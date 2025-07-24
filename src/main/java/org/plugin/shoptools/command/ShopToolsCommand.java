package org.plugin.shoptools.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.plugin.shoptools.ShopTools;
import org.plugin.shoptools.config.ConfigManager;
import org.plugin.shoptools.model.ShopData;
import org.plugin.shoptools.storage.ShopDataManager;
import org.plugin.shoptools.util.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ShopTools主命令处理器
 * 处理所有与ShopTools相关的命令
 * 
 * @author NSrank & Augment
 */
public class ShopToolsCommand implements CommandExecutor, TabCompleter {
    
    private final ShopTools plugin;
    private final ConfigManager configManager;
    private ShopDataManager dataManager;
    
    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param configManager 配置管理器
     * @param dataManager 数据管理器（可以为null，稍后通过setDataManager设置）
     */
    public ShopToolsCommand(ShopTools plugin, ConfigManager configManager, ShopDataManager dataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    /**
     * 设置数据管理器
     *
     * @param dataManager 数据管理器
     */
    public void setDataManager(ShopDataManager dataManager) {
        this.dataManager = dataManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("shoptools.use")) {
            MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
            return true;
        }
        
        // 检查数据管理器是否已初始化
        if (dataManager == null) {
            MessageUtil.sendMessage(sender, configManager.getMessage("plugin-initializing"));
            return true;
        }

        // 检查数据是否已加载
        if (!dataManager.isDataLoaded()) {
            MessageUtil.sendMessage(sender, configManager.getMessage("data-loading"));
            return true;
        }
        
        // 处理命令
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "search":
                handleSearchCommand(sender, args);
                break;
            case "near":
                handleNearCommand(sender, args);
                break;
            case "list":
                handleListCommand(sender, args);
                break;
            case "page":
                handlePageCommand(sender, args);
                break;
            case "who":
                handleWhoCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "help":
                showHelp(sender);
                break;
            default:
                MessageUtil.sendMessage(sender, configManager.getMessage("invalid-command"));
                break;
        }
        
        return true;
    }
    
    /**
     * 处理search命令（玩家附近商店搜索）
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleSearchCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, configManager.getMessage("player-only"));
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&c用法: /shoptools search <物品ID> [页码]");
            MessageUtil.sendMessage(sender, "&7搜索范围: 以你为中心200格内的商店");
            return;
        }

        Player player = (Player) sender;
        String itemId = args[1];
        int page = 1; // 默认第一页

        // 检查是否指定了页码
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
                if (page < 1) {
                    MessageUtil.sendMessage(sender, "&c页码必须大于0！");
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(sender, "&c页码必须是数字！");
                return;
            }
        }

        // 获取所有指定物品的商店（不限制距离）
        List<ShopData> allShops = dataManager.getShopsByItem(itemId);

        if (allShops.isEmpty()) {
            MessageUtil.sendMessage(sender, "&e没有找到 &6" + itemId + "&e 的商店。");
            return;
        }

        // 按距离排序（从近到远，其他世界的商店排在最后）
        allShops.sort(createDistanceComparator(player.getLocation()));

        // 检查是否需要分页
        int pageSize = 10;
        if (allShops.size() <= pageSize) {
            // 商店数量少，直接显示所有
            displayNearbyShopList(sender, allShops, itemId + " 商店", false, player.getLocation());
        } else {
            // 商店数量多，使用分页显示
            displayNearbyShopListPaged(sender, allShops, itemId + " 商店", page, player.getLocation());

            // 添加导航提示
            int totalPages = (int) Math.ceil((double) allShops.size() / 10);
            StringBuilder navigation = new StringBuilder("&7");
            if (page > 1) {
                navigation.append("上一页: &e/st search ").append(itemId).append(" ").append(page - 1).append(" &7");
            }
            if (page < totalPages) {
                navigation.append("下一页: &e/st search ").append(itemId).append(" ").append(page + 1);
            }

            if (navigation.length() > 3) {
                MessageUtil.sendMessage(sender, navigation.toString());
            }
        }
    }

    /**
     * 处理near命令（玩家附近所有商店）
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleNearCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, configManager.getMessage("player-only"));
            return;
        }

        Player player = (Player) sender;
        int page = 1; // 默认第一页

        // 检查是否指定了页码
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    MessageUtil.sendMessage(sender, "&c页码必须大于0！");
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(sender, "&c页码必须是数字！");
                return;
            }
        }

        // 获取附近的所有商店
        List<ShopData> nearbyShops = getNearbyShops(player, 200.0);

        if (nearbyShops.isEmpty()) {
            MessageUtil.sendMessage(sender, "&e附近200格内没有找到任何商店。");
            return;
        }

        // 按距离排序（从近到远）
        nearbyShops.sort(Comparator.comparing(shop -> getDistance(player.getLocation(), shop.getLocation())));

        // 检查是否需要分页
        int pageSize = 10;
        if (nearbyShops.size() <= pageSize) {
            // 商店数量少，直接显示所有
            displayNearbyShopList(sender, nearbyShops, "附近的商店", false, player.getLocation());
        } else {
            // 商店数量多，使用分页显示
            displayNearbyShopListPaged(sender, nearbyShops, "附近的商店", page, player.getLocation());

            // 添加导航提示
            int totalPages = (int) Math.ceil((double) nearbyShops.size() / 10);
            StringBuilder navigation = new StringBuilder("&7");
            if (page > 1) {
                navigation.append("上一页: &e/st near ").append(page - 1).append(" &7");
            }
            if (page < totalPages) {
                navigation.append("下一页: &e/st near ").append(page + 1);
            }

            if (navigation.length() > 3) {
                MessageUtil.sendMessage(sender, navigation.toString());
            }
        }
    }

    /**
     * 处理list命令（管理员专用，支持特定物品查询和分页）
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleListCommand(CommandSender sender, String[] args) {
        // 检查管理员权限
        if (!sender.hasPermission("shoptools.admin")) {
            MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&c用法: /shoptools list <物品ID> [页码]");
            MessageUtil.sendMessage(sender, "&e提示: 使用 /shoptools page <页码> 查看所有商店");
            return;
        }

        String itemId = args[1];
        int page = 1; // 默认第一页

        // 检查是否指定了页码
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
                if (page < 1) {
                    MessageUtil.sendMessage(sender, "&c页码必须大于0！");
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(sender, "&c页码必须是数字！");
                return;
            }
        }

        // 获取指定物品的商店
        List<ShopData> shops = dataManager.getShopsByItem(itemId);

        if (shops.isEmpty()) {
            MessageUtil.sendMessage(sender, configManager.getMessage("item-not-found", "item", itemId));
            return;
        }

        // 按价格升序排序
        shops.sort(Comparator.comparing(ShopData::getPrice));

        // 检查是否需要分页
        int pageSize = 10;
        if (shops.size() <= pageSize) {
            // 商店数量少，直接显示所有
            displayShopList(sender, shops, "物品 " + itemId + " 的商店", false);
        } else {
            // 商店数量多，使用分页显示
            displayShopListPaged(sender, shops, "物品 " + itemId + " 的商店", page);
        }
    }

    /**
     * 处理page命令（管理员专用，分页显示所有商店）
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handlePageCommand(CommandSender sender, String[] args) {
        // 检查管理员权限
        if (!sender.hasPermission("shoptools.admin")) {
            MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&c用法: /shoptools page <页码>");
            return;
        }

        int page;
        try {
            page = Integer.parseInt(args[1]);
            if (page < 1) {
                MessageUtil.sendMessage(sender, "&c页码必须大于0！");
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "&c页码必须是数字！");
            return;
        }

        List<ShopData> allShops = dataManager.getAllShops();
        if (allShops.isEmpty()) {
            MessageUtil.sendMessage(sender, configManager.getMessage("no-shops-found"));
            return;
        }

        // 按物品ID字母序排序
        allShops.sort(Comparator.comparing(ShopData::getItemId));

        // 分页显示
        displayShopListPaged(sender, allShops, "所有商店", page);
    }

    /**
     * 处理who命令（增强版，支持分页）
     *
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleWhoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&c用法: /shoptools who <玩家名/UUID> [页码]");
            MessageUtil.sendMessage(sender, "&7支持: 完整名称、部分名称、UUID");
            return;
        }

        // 检查管理员权限
        if (!sender.hasPermission("shoptools.admin")) {
            MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
            return;
        }

        String searchTerm = args[1];
        int page = 1; // 默认第一页

        // 检查是否指定了页码
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
                if (page < 1) {
                    MessageUtil.sendMessage(sender, "&c页码必须大于0！");
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(sender, "&c页码必须是数字！");
                return;
            }
        }

        // 尝试智能查找玩家
        PlayerSearchResult searchResult = findPlayerShops(searchTerm);

        if (searchResult.getType() == PlayerSearchResult.ResultType.NO_MATCH) {
            MessageUtil.sendMessage(sender, configManager.getMessage("player-not-found", "player", searchTerm));

            // 提供相似玩家名建议
            List<String> suggestions = getSimilarPlayerNames(searchTerm);
            if (!suggestions.isEmpty()) {
                MessageUtil.sendMessage(sender, "&7你是否想查找: &e" + String.join("&7, &e", suggestions));
            }
            return;
        }

        if (searchResult.getType() == PlayerSearchResult.ResultType.MULTIPLE_MATCHES) {
            // 显示多个匹配结果供选择
            displayMultiplePlayerMatches(sender, searchTerm, searchResult.getPlayerMatches());
            return;
        }

        // 单个匹配结果，显示商店列表
        List<ShopData> shops = searchResult.getShops();
        String playerDisplayName = searchResult.getPlayerDisplayName();

        // 按商店ID字母序排序
        shops.sort(Comparator.comparing(shop -> shop.getShopId().toString()));

        // 检查是否需要分页
        int pageSize = 10;
        if (shops.size() <= pageSize) {
            // 商店数量少，直接显示所有
            displayShopList(sender, shops, "玩家 " + playerDisplayName + " 的商店", false);
        } else {
            // 商店数量多，使用分页显示
            displayShopListPaged(sender, shops, "玩家 " + playerDisplayName + " 的商店", page);
        }
    }
    
    /**
     * 处理reload命令
     * 
     * @param sender 命令发送者
     */
    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("shoptools.admin")) {
            MessageUtil.sendMessage(sender, configManager.getMessage("no-permission"));
            return;
        }
        
        MessageUtil.sendMessage(sender, "&a正在重新加载配置和数据...");
        
        // 重新加载配置
        configManager.reloadConfig();
        
        // 重新同步数据
        plugin.syncShopData();
        
        MessageUtil.sendMessage(sender, "&a重新加载完成！");
    }
    
    /**
     * 显示商店列表（不分页版本，用于特定物品查询）
     *
     * @param sender 命令发送者
     * @param shops 商店列表
     * @param title 列表标题
     * @param paginated 是否分页（此版本忽略此参数）
     */
    private void displayShopList(CommandSender sender, List<ShopData> shops, String title, boolean paginated) {
        // 发送标题
        MessageUtil.sendMessage(sender, configManager.getMessage("shop-list-header").replace("{title}", title));

        // 显示所有商店（不分页）
        for (ShopData shop : shops) {
            String ownerName = getPlayerName(shop.getOwnerId(), shop.getOwnerName());
            String shopStatus = getShopStatusText(shop.getShopType());

            String message = configManager.getMessage("shop-list-item")
                    .replace("{item}", shop.getItemDisplayName())
                    .replace("{location}", shop.getFormattedLocation())
                    .replace("{price}", shop.getFormattedPrice())
                    .replace("{owner}", ownerName)
                    .replace("{status}", shopStatus);

            MessageUtil.sendMessage(sender, message);
        }

        // 发送底部信息
        MessageUtil.sendMessage(sender, configManager.getMessage("shop-list-footer", "count", String.valueOf(shops.size())));
    }

    /**
     * 分页显示商店列表
     *
     * @param sender 命令发送者
     * @param shops 商店列表
     * @param title 列表标题
     * @param page 页码（从1开始）
     */
    private void displayShopListPaged(CommandSender sender, List<ShopData> shops, String title, int page) {
        int pageSize = 10; // 每页显示10个商店
        int totalPages = (int) Math.ceil((double) shops.size() / pageSize);

        if (page > totalPages) {
            MessageUtil.sendMessage(sender, "&c页码超出范围！总共 " + totalPages + " 页。");
            return;
        }

        // 发送标题
        MessageUtil.sendMessage(sender, configManager.getMessage("shop-list-header").replace("{title}", title + " (第" + page + "页/共" + totalPages + "页)"));

        // 计算显示范围
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, shops.size());

        // 显示当前页的商店
        for (int i = start; i < end; i++) {
            ShopData shop = shops.get(i);
            String ownerName = getPlayerName(shop.getOwnerId(), shop.getOwnerName());
            String shopStatus = getShopStatusText(shop.getShopType());

            String message = configManager.getMessage("shop-list-item")
                    .replace("{item}", shop.getItemDisplayName())
                    .replace("{location}", shop.getFormattedLocation())
                    .replace("{price}", shop.getFormattedPrice())
                    .replace("{owner}", ownerName)
                    .replace("{status}", shopStatus);

            MessageUtil.sendMessage(sender, message);
        }

        // 发送分页信息
        MessageUtil.sendMessage(sender, "&7显示第 " + start + "-" + end + " 个商店，共 " + shops.size() + " 个");

        // 发送导航提示
        StringBuilder navigation = new StringBuilder("&7");
        if (page > 1) {
            navigation.append("上一页: &e/st page ").append(page - 1).append(" &7");
        }
        if (page < totalPages) {
            navigation.append("下一页: &e/st page ").append(page + 1);
        }

        if (navigation.length() > 3) { // 如果有导航信息
            MessageUtil.sendMessage(sender, navigation.toString());
        }
    }

    /**
     * 获取玩家名称（优先使用缓存的名称，如果为空则尝试从UUID获取）
     *
     * @param playerId 玩家UUID
     * @param cachedName 缓存的玩家名称
     * @return 玩家名称
     */
    private String getPlayerName(UUID playerId, String cachedName) {
        // 如果缓存的名称不为空且不是UUID格式，直接使用
        if (cachedName != null && !cachedName.trim().isEmpty() && !isUUID(cachedName)) {
            return cachedName;
        }

        // 尝试从在线玩家获取名称
        Player onlinePlayer = Bukkit.getPlayer(playerId);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        // 尝试从离线玩家获取名称
        try {
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            String name = offlinePlayer.getName();
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        } catch (Exception e) {
            // 忽略错误
        }

        // 如果都失败了，返回UUID的简短形式
        return playerId.toString().substring(0, 8) + "...";
    }

    /**
     * 检查字符串是否为UUID格式
     *
     * @param str 要检查的字符串
     * @return 如果是UUID格式返回true
     */
    private boolean isUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取商店状态文本
     *
     * @param shopType 商店类型
     * @return 状态文本
     */
    private String getShopStatusText(ShopData.ShopType shopType) {
        switch (shopType) {
            case SELLING:
                return "&a[出售]";
            case BUYING:
                return "&b[收购]";
            case BOTH:
                return "&e[双向]";
            default:
                return "&7[未知]";
        }
    }

    /**
     * 智能查找玩家商店
     *
     * @param searchTerm 搜索词（玩家名或UUID）
     * @return 搜索结果
     */
    private PlayerSearchResult findPlayerShops(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new PlayerSearchResult(PlayerSearchResult.ResultType.NO_MATCH, new ArrayList<>());
        }

        String normalizedSearch = searchTerm.toLowerCase().trim();

        // 1. 尝试UUID精确匹配
        if (isUUID(searchTerm)) {
            try {
                UUID playerId = UUID.fromString(searchTerm);
                List<ShopData> shops = dataManager.getShopsByOwner(playerId);
                if (!shops.isEmpty()) {
                    String playerName = getPlayerName(playerId, null);
                    return new PlayerSearchResult(PlayerSearchResult.ResultType.SINGLE_MATCH, shops, playerName);
                }
            } catch (IllegalArgumentException e) {
                // 不是有效的UUID，继续其他匹配方式
            }
        }

        // 2. 收集所有玩家信息
        Map<UUID, PlayerMatch> playerMap = new HashMap<>();
        List<ShopData> allShops = dataManager.getAllShops();

        for (ShopData shop : allShops) {
            UUID ownerId = shop.getOwnerId();
            String ownerName = getPlayerName(ownerId, shop.getOwnerName());

            PlayerMatch match = playerMap.computeIfAbsent(ownerId,
                id -> new PlayerMatch(ownerName, id, 0));
            playerMap.put(ownerId, new PlayerMatch(match.getPlayerName(), ownerId, match.getShopCount() + 1));
        }

        // 3. 精确名称匹配
        for (PlayerMatch match : playerMap.values()) {
            if (match.getPlayerName().equalsIgnoreCase(normalizedSearch)) {
                List<ShopData> shops = dataManager.getShopsByOwner(match.getPlayerId());
                return new PlayerSearchResult(PlayerSearchResult.ResultType.SINGLE_MATCH, shops, match.getPlayerName());
            }
        }

        // 4. 部分名称匹配
        List<PlayerMatch> partialMatches = new ArrayList<>();
        for (PlayerMatch match : playerMap.values()) {
            if (match.getPlayerName().toLowerCase().contains(normalizedSearch)) {
                partialMatches.add(match);
            }
        }

        if (partialMatches.isEmpty()) {
            return new PlayerSearchResult(PlayerSearchResult.ResultType.NO_MATCH, new ArrayList<>());
        } else if (partialMatches.size() == 1) {
            PlayerMatch match = partialMatches.get(0);
            List<ShopData> shops = dataManager.getShopsByOwner(match.getPlayerId());
            return new PlayerSearchResult(PlayerSearchResult.ResultType.SINGLE_MATCH, shops, match.getPlayerName());
        } else {
            return new PlayerSearchResult(PlayerSearchResult.ResultType.MULTIPLE_MATCHES, partialMatches);
        }
    }

    /**
     * 获取相似的玩家名建议
     *
     * @param searchTerm 搜索词
     * @return 相似玩家名列表
     */
    private List<String> getSimilarPlayerNames(String searchTerm) {
        if (searchTerm == null || searchTerm.length() < 2) {
            return new ArrayList<>();
        }

        String normalizedSearch = searchTerm.toLowerCase();
        List<String> suggestions = new ArrayList<>();

        // 获取所有唯一的玩家名
        Set<String> playerNames = dataManager.getAllShops().stream()
                .map(shop -> getPlayerName(shop.getOwnerId(), shop.getOwnerName()))
                .collect(Collectors.toSet());

        // 查找相似的名称（编辑距离算法简化版）
        for (String playerName : playerNames) {
            if (playerName != null && isSimilar(normalizedSearch, playerName.toLowerCase())) {
                suggestions.add(playerName);
            }
        }

        // 限制建议数量并排序
        return suggestions.stream()
                .sorted()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 简单的相似度检查
     *
     * @param search 搜索词
     * @param target 目标词
     * @return 是否相似
     */
    private boolean isSimilar(String search, String target) {
        // 1. 包含关系
        if (target.contains(search) || search.contains(target)) {
            return true;
        }

        // 2. 开头匹配
        if (target.startsWith(search) || search.startsWith(target)) {
            return true;
        }

        // 3. 简单的编辑距离检查（仅适用于短字符串）
        if (search.length() <= 3 && target.length() <= 8) {
            return getLevenshteinDistance(search, target) <= 2;
        }

        return false;
    }

    /**
     * 计算编辑距离（简化版）
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 编辑距离
     */
    private int getLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0) return len2;
        if (len2 == 0) return len1;

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }

        return dp[len1][len2];
    }

    /**
     * 显示多个玩家匹配结果
     *
     * @param sender 命令发送者
     * @param searchTerm 搜索词
     * @param matches 匹配结果列表
     */
    private void displayMultiplePlayerMatches(CommandSender sender, String searchTerm, List<PlayerMatch> matches) {
        MessageUtil.sendMessage(sender, "&6找到多个匹配 \"" + searchTerm + "\" 的玩家:");
        MessageUtil.sendMessage(sender, "&7请使用更精确的名称重新搜索:");

        // 按商店数量降序排序
        matches.sort((a, b) -> Integer.compare(b.getShopCount(), a.getShopCount()));

        for (int i = 0; i < Math.min(matches.size(), 10); i++) {
            PlayerMatch match = matches.get(i);
            String command = "/st who " + match.getPlayerName();

            // 如果商店数量超过10个，提示可能需要分页
            if (match.getShopCount() > 10) {
                command += " [页码]";
            }

            MessageUtil.sendMessage(sender, String.format("&e%s &7(%d个商店) &8- &7%s",
                match.getPlayerName(),
                match.getShopCount(),
                command));
        }

        if (matches.size() > 10) {
            MessageUtil.sendMessage(sender, "&7... 还有 " + (matches.size() - 10) + " 个匹配结果");
        }
    }

    /**
     * 获取玩家附近指定物品的商店
     *
     * @param player 玩家
     * @param itemId 物品ID
     * @param radius 搜索半径
     * @return 附近的商店列表
     */
    private List<ShopData> getNearbyShopsByItem(Player player, String itemId, double radius) {
        List<ShopData> allShops = dataManager.getShopsByItem(itemId);
        List<ShopData> nearbyShops = new ArrayList<>();

        org.bukkit.Location playerLocation = player.getLocation();

        for (ShopData shop : allShops) {
            org.bukkit.Location shopLocation = shop.getLocation();
            if (shopLocation != null && shopLocation.getWorld() != null &&
                shopLocation.getWorld().equals(playerLocation.getWorld())) {

                double distance = getDistance(playerLocation, shopLocation);
                if (distance <= radius) {
                    nearbyShops.add(shop);
                }
            }
        }

        return nearbyShops;
    }

    /**
     * 获取玩家附近的所有商店
     *
     * @param player 玩家
     * @param radius 搜索半径
     * @return 附近的商店列表
     */
    private List<ShopData> getNearbyShops(Player player, double radius) {
        List<ShopData> allShops = dataManager.getAllShops();
        List<ShopData> nearbyShops = new ArrayList<>();

        org.bukkit.Location playerLocation = player.getLocation();

        for (ShopData shop : allShops) {
            org.bukkit.Location shopLocation = shop.getLocation();
            if (shopLocation != null && shopLocation.getWorld() != null &&
                shopLocation.getWorld().equals(playerLocation.getWorld())) {

                double distance = getDistance(playerLocation, shopLocation);
                if (distance <= radius) {
                    nearbyShops.add(shop);
                }
            }
        }

        return nearbyShops;
    }

    /**
     * 创建距离比较器（支持跨世界排序）
     *
     * @param playerLocation 玩家位置
     * @return 距离比较器
     */
    private Comparator<ShopData> createDistanceComparator(org.bukkit.Location playerLocation) {
        return (shop1, shop2) -> {
            org.bukkit.Location loc1 = shop1.getLocation();
            org.bukkit.Location loc2 = shop2.getLocation();

            // 检查是否在同一世界
            boolean shop1SameWorld = loc1 != null && loc1.getWorld() != null &&
                                   loc1.getWorld().equals(playerLocation.getWorld());
            boolean shop2SameWorld = loc2 != null && loc2.getWorld() != null &&
                                   loc2.getWorld().equals(playerLocation.getWorld());

            // 同世界的商店优先
            if (shop1SameWorld && !shop2SameWorld) {
                return -1;
            }
            if (!shop1SameWorld && shop2SameWorld) {
                return 1;
            }

            // 如果都在同一世界，按距离排序
            if (shop1SameWorld && shop2SameWorld) {
                double dist1 = playerLocation.distance(loc1);
                double dist2 = playerLocation.distance(loc2);
                return Double.compare(dist1, dist2);
            }

            // 如果都在其他世界，按世界名排序
            if (!shop1SameWorld && !shop2SameWorld) {
                String world1 = loc1 != null && loc1.getWorld() != null ? loc1.getWorld().getName() : "";
                String world2 = loc2 != null && loc2.getWorld() != null ? loc2.getWorld().getName() : "";
                return world1.compareTo(world2);
            }

            return 0;
        };
    }

    /**
     * 计算两个位置之间的距离
     *
     * @param loc1 位置1
     * @param loc2 位置2
     * @return 距离
     */
    private double getDistance(org.bukkit.Location loc1, org.bukkit.Location loc2) {
        if (loc1 == null || loc2 == null || !loc1.getWorld().equals(loc2.getWorld())) {
            return Double.MAX_VALUE;
        }

        return loc1.distance(loc2);
    }

    /**
     * 获取格式化的距离文本
     *
     * @param playerLocation 玩家位置
     * @param shopLocation 商店位置
     * @return 格式化的距离文本
     */
    private String getFormattedDistance(org.bukkit.Location playerLocation, org.bukkit.Location shopLocation) {
        if (shopLocation == null || shopLocation.getWorld() == null) {
            return "unknown";
        }

        // 检查是否在同一世界
        if (!shopLocation.getWorld().equals(playerLocation.getWorld())) {
            return "otherworld";
        }

        // 计算距离
        double distance = playerLocation.distance(shopLocation);

        // 超过200格显示"200m+"
        if (distance > 200.0) {
            return "200m+";
        }

        // 正常距离显示
        return String.format("%.1fm", distance);
    }

    /**
     * 显示附近商店列表（不分页版本）
     *
     * @param sender 命令发送者
     * @param shops 商店列表
     * @param title 列表标题
     * @param paginated 是否分页（此版本忽略此参数）
     * @param playerLocation 玩家位置
     */
    private void displayNearbyShopList(CommandSender sender, List<ShopData> shops, String title, boolean paginated, org.bukkit.Location playerLocation) {
        // 发送标题
        MessageUtil.sendMessage(sender, configManager.getMessage("shop-list-header").replace("{title}", title));

        // 显示所有商店（不分页）
        for (ShopData shop : shops) {
            String ownerName = getPlayerName(shop.getOwnerId(), shop.getOwnerName());
            String shopStatus = getShopStatusText(shop.getShopType());
            String distanceText = getFormattedDistance(playerLocation, shop.getLocation());

            String message = configManager.getMessage("shop-list-item")
                    .replace("{item}", shop.getItemDisplayName())
                    .replace("{location}", shop.getFormattedLocation())
                    .replace("{price}", distanceText)
                    .replace("{owner}", ownerName)
                    .replace("{status}", shopStatus);

            MessageUtil.sendMessage(sender, message);
        }

        // 发送底部信息
        MessageUtil.sendMessage(sender, configManager.getMessage("shop-list-footer", "count", String.valueOf(shops.size())));
    }

    /**
     * 分页显示附近商店列表
     *
     * @param sender 命令发送者
     * @param shops 商店列表
     * @param title 列表标题
     * @param page 页码（从1开始）
     * @param playerLocation 玩家位置
     */
    private void displayNearbyShopListPaged(CommandSender sender, List<ShopData> shops, String title, int page, org.bukkit.Location playerLocation) {
        int pageSize = 10; // 每页显示10个商店
        int totalPages = (int) Math.ceil((double) shops.size() / pageSize);

        if (page > totalPages) {
            MessageUtil.sendMessage(sender, "&c页码超出范围！总共 " + totalPages + " 页。");
            return;
        }

        // 发送标题
        MessageUtil.sendMessage(sender, configManager.getMessage("shop-list-header").replace("{title}", title + " (第" + page + "页/共" + totalPages + "页)"));

        // 计算显示范围
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, shops.size());

        // 显示当前页的商店
        for (int i = start; i < end; i++) {
            ShopData shop = shops.get(i);
            String ownerName = getPlayerName(shop.getOwnerId(), shop.getOwnerName());
            String shopStatus = getShopStatusText(shop.getShopType());
            String distanceText = getFormattedDistance(playerLocation, shop.getLocation());

            String message = configManager.getMessage("shop-list-item")
                    .replace("{item}", shop.getItemDisplayName())
                    .replace("{location}", shop.getFormattedLocation())
                    .replace("{price}", distanceText)
                    .replace("{owner}", ownerName)
                    .replace("{status}", shopStatus);

            MessageUtil.sendMessage(sender, message);
        }

        // 发送分页信息
        MessageUtil.sendMessage(sender, "&7显示第 " + (start + 1) + "-" + end + " 个商店，共 " + shops.size() + " 个");

        // 发送导航提示（这里需要根据调用上下文确定命令类型）
        // 由于这是通用方法，导航提示将在调用处单独处理
    }

    /**
     * 显示帮助信息
     *
     * @param sender 命令发送者
     */
    private void showHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, configManager.getMessage("help-header"));

        // 玩家可用命令
        MessageUtil.sendMessage(sender, configManager.getMessage("help-search"));
        MessageUtil.sendMessage(sender, configManager.getMessage("help-near"));

        // 管理员命令
        if (sender.hasPermission("shoptools.admin")) {
            MessageUtil.sendMessage(sender, configManager.getMessage("help-page"));
            MessageUtil.sendMessage(sender, configManager.getMessage("help-list"));
            MessageUtil.sendMessage(sender, configManager.getMessage("help-list-item"));
            MessageUtil.sendMessage(sender, configManager.getMessage("help-who"));
            MessageUtil.sendMessage(sender, configManager.getMessage("help-reload"));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一个参数：子命令
            // 玩家可用命令
            completions.add("search");
            completions.add("near");
            completions.add("help");

            // 管理员命令
            if (sender.hasPermission("shoptools.admin")) {
                completions.add("page");
                completions.add("list");
                completions.add("who");
                completions.add("reload");
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if ("search".equals(subCommand)) {
                // 物品ID补全（玩家命令）
                Set<String> itemIds = dataManager.getAllShops().stream()
                        .map(ShopData::getItemId)
                        .collect(Collectors.toSet());
                completions.addAll(itemIds);
            } else if ("near".equals(subCommand)) {
                // 页码补全（玩家命令）
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<ShopData> nearbyShops = getNearbyShops(player, 200.0);
                    if (nearbyShops.size() > 10) {
                        int totalPages = (int) Math.ceil((double) nearbyShops.size() / 10);
                        for (int i = 1; i <= Math.min(totalPages, 10); i++) {
                            completions.add(String.valueOf(i));
                        }
                    }
                }
            } else if ("page".equals(subCommand) && sender.hasPermission("shoptools.admin")) {
                // 页码补全（管理员命令）
                int totalShops = dataManager.getShopCount();
                int totalPages = (int) Math.ceil((double) totalShops / 10);
                for (int i = 1; i <= Math.min(totalPages, 10); i++) { // 最多显示前10页的补全
                    completions.add(String.valueOf(i));
                }
            } else if ("list".equals(subCommand) && sender.hasPermission("shoptools.admin")) {
                // 物品ID补全（管理员命令）
                Set<String> itemIds = dataManager.getAllShops().stream()
                        .map(ShopData::getItemId)
                        .collect(Collectors.toSet());
                completions.addAll(itemIds);
            } else if ("who".equals(subCommand) && sender.hasPermission("shoptools.admin")) {
                // 玩家名补全（管理员命令）
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if ("search".equals(subCommand) && sender instanceof Player) {
                // 第三个参数：页码补全（针对特定物品的全服搜索）
                String itemId = args[1];
                List<ShopData> allShops = dataManager.getShopsByItem(itemId);
                if (allShops.size() > 10) {
                    int totalPages = (int) Math.ceil((double) allShops.size() / 10);
                    for (int i = 1; i <= Math.min(totalPages, 10); i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            } else if ("list".equals(subCommand) && sender.hasPermission("shoptools.admin")) {
                // 第三个参数：页码补全（针对特定物品，管理员命令）
                String itemId = args[1];
                List<ShopData> shops = dataManager.getShopsByItem(itemId);
                if (shops.size() > 10) { // 只有超过10个商店才需要分页
                    int totalPages = (int) Math.ceil((double) shops.size() / 10);
                    for (int i = 1; i <= Math.min(totalPages, 10); i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            } else if ("who".equals(subCommand) && sender.hasPermission("shoptools.admin")) {
                // 第三个参数：页码补全（针对特定玩家，管理员命令）
                String playerName = args[1];
                List<ShopData> shops = dataManager.getShopsByOwnerName(playerName);
                if (shops.size() > 10) { // 只有超过10个商店才需要分页
                    int totalPages = (int) Math.ceil((double) shops.size() / 10);
                    for (int i = 1; i <= Math.min(totalPages, 10); i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        }
        
        // 过滤匹配的补全
        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(currentArg))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 玩家搜索结果类
     */
    private static class PlayerSearchResult {
        public enum ResultType {
            NO_MATCH,           // 没有匹配
            SINGLE_MATCH,       // 单个匹配
            MULTIPLE_MATCHES    // 多个匹配
        }

        private final ResultType type;
        private final List<ShopData> shops;
        private final String playerDisplayName;
        private final List<PlayerMatch> playerMatches;

        public PlayerSearchResult(ResultType type, List<ShopData> shops, String playerDisplayName) {
            this.type = type;
            this.shops = shops;
            this.playerDisplayName = playerDisplayName;
            this.playerMatches = new ArrayList<>();
        }

        public PlayerSearchResult(ResultType type, List<PlayerMatch> playerMatches) {
            this.type = type;
            this.shops = new ArrayList<>();
            this.playerDisplayName = null;
            this.playerMatches = playerMatches;
        }

        public ResultType getType() { return type; }
        public List<ShopData> getShops() { return shops; }
        public String getPlayerDisplayName() { return playerDisplayName; }
        public List<PlayerMatch> getPlayerMatches() { return playerMatches; }
    }

    /**
     * 玩家匹配信息类
     */
    private static class PlayerMatch {
        private final String playerName;
        private final UUID playerId;
        private final int shopCount;

        public PlayerMatch(String playerName, UUID playerId, int shopCount) {
            this.playerName = playerName;
            this.playerId = playerId;
            this.shopCount = shopCount;
        }

        public String getPlayerName() { return playerName; }
        public UUID getPlayerId() { return playerId; }
        public int getShopCount() { return shopCount; }
    }
}
