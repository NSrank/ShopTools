package org.plugin.shoptools.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.api.shop.ShopType;
import org.plugin.shoptools.model.ShopData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * QuickShop集成类
 * 负责与QuickShop-Reremake插件进行交互，获取商店数据
 * 
 * @author NSrank & Augment
 */
public class QuickShopIntegration {
    
    private final Logger logger;
    private QuickShopAPI quickShopAPI;
    private boolean isQuickShopAvailable = false;
    
    /**
     * 构造函数
     * 
     * @param logger 日志记录器
     */
    public QuickShopIntegration(Logger logger) {
        this.logger = logger;
        initializeQuickShop();
    }
    
    /**
     * 初始化QuickShop API
     */
    private void initializeQuickShop() {
        Plugin quickShopPlugin = Bukkit.getPluginManager().getPlugin("QuickShop");
        
        if (quickShopPlugin == null) {
            logger.warning("未找到QuickShop插件！请确保已安装QuickShop-Reremake。");
            return;
        }
        
        if (!quickShopPlugin.isEnabled()) {
            logger.warning("QuickShop插件未启用！");
            return;
        }
        
        try {
            // 尝试获取QuickShop API
            if (quickShopPlugin instanceof QuickShopAPI) {
                this.quickShopAPI = (QuickShopAPI) quickShopPlugin;
                this.isQuickShopAvailable = true;
                logger.info("成功连接到QuickShop-Reremake API！");
            } else {
                logger.severe("QuickShop插件不支持API接口！");
            }
        } catch (Exception e) {
            logger.severe("初始化QuickShop API时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查QuickShop是否可用
     * 
     * @return 如果QuickShop可用返回true
     */
    public boolean isQuickShopAvailable() {
        return isQuickShopAvailable && quickShopAPI != null;
    }
    
    /**
     * 获取所有商店数据
     * 
     * @return 商店数据列表
     */
    public List<ShopData> getAllShops() {
        List<ShopData> shopDataList = new ArrayList<>();
        
        if (!isQuickShopAvailable()) {
            logger.warning("QuickShop不可用，无法获取商店数据！");
            return shopDataList;
        }
        
        try {
            // 获取所有商店
            Collection<Shop> shops = quickShopAPI.getShopManager().getAllShops();
            
            if (shops == null || shops.isEmpty()) {
                logger.info("未找到任何商店数据。");
                return shopDataList;
            }
            
            logger.info("开始处理 " + shops.size() + " 个商店数据...");
            
            for (Shop shop : shops) {
                try {
                    ShopData shopData = convertShopToShopData(shop);
                    if (shopData != null) {
                        shopDataList.add(shopData);
                    }
                } catch (Exception e) {
                    logger.warning("处理商店数据时发生错误: " + e.getMessage());
                    if (logger.getLevel().intValue() <= 500) { // DEBUG级别
                        e.printStackTrace();
                    }
                }
            }
            
            logger.info("成功处理了 " + shopDataList.size() + " 个商店数据。");
            
        } catch (Exception e) {
            logger.severe("获取商店数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return shopDataList;
    }
    
    /**
     * 将QuickShop的Shop对象转换为ShopData对象
     * 
     * @param shop QuickShop的商店对象
     * @return 转换后的ShopData对象
     */
    private ShopData convertShopToShopData(Shop shop) {
        if (shop == null) {
            return null;
        }
        
        try {
            // 获取商店基本信息
            UUID shopId = UUID.randomUUID(); // 使用随机UUID作为商店ID
            String itemId = shop.getItem().getType().name();
            String itemDisplayName = getItemDisplayName(shop);
            double price = shop.getPrice();
            UUID ownerId = shop.getOwner();
            String ownerName = shop.getOwner().toString(); // 转换为字符串
            int stock = shop.getRemainingStock();
            
            // 转换商店类型
            ShopData.ShopType shopType = convertShopType(shop.getShopType());
            
            // 创建ShopData对象
            return new ShopData(
                shopId,
                itemId,
                itemDisplayName,
                shop.getLocation(),
                price,
                ownerId,
                ownerName,
                shopType,
                stock,
                shop.getItem()
            );
            
        } catch (Exception e) {
            logger.warning("转换商店数据时发生错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取物品显示名称
     * 
     * @param shop 商店对象
     * @return 物品显示名称
     */
    private String getItemDisplayName(Shop shop) {
        try {
            if (shop.getItem().hasItemMeta() && shop.getItem().getItemMeta().hasDisplayName()) {
                return shop.getItem().getItemMeta().getDisplayName();
            }
            return shop.getItem().getType().name();
        } catch (Exception e) {
            return shop.getItem().getType().name();
        }
    }
    
    /**
     * 转换商店类型
     * 
     * @param quickShopType QuickShop的商店类型
     * @return ShopData的商店类型
     */
    private ShopData.ShopType convertShopType(ShopType quickShopType) {
        switch (quickShopType) {
            case SELLING:
                return ShopData.ShopType.SELLING;
            case BUYING:
                return ShopData.ShopType.BUYING;
            default:
                return ShopData.ShopType.SELLING;
        }
    }

    /**
     * 获取QuickShop API实例
     *
     * @return QuickShop API实例，如果不可用返回null
     */
    public QuickShopAPI getQuickShopAPI() {
        return isQuickShopAvailable() ? quickShopAPI : null;
    }

    /**
     * 获取指定玩家拥有的所有商店
     *
     * @param playerUUID 玩家UUID
     * @return 该玩家拥有的商店列表
     */
    public List<Shop> getPlayerShops(UUID playerUUID) {
        if (!isQuickShopAvailable()) {
            logger.warning("QuickShop不可用，无法获取玩家商店！");
            return new ArrayList<>();
        }

        try {
            Collection<Shop> allShops = quickShopAPI.getShopManager().getAllShops();
            return allShops.stream()
                .filter(shop -> shop.getOwner().equals(playerUUID))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warning("获取玩家商店时发生错误: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 检查QuickShop API是否支持删除商店功能
     *
     * @return 如果支持删除功能返回true
     */
    public boolean supportsShopDeletion() {
        if (!isQuickShopAvailable()) {
            return false;
        }

        try {
            // 尝试获取ShopManager并检查是否有删除方法
            Object shopManager = quickShopAPI.getShopManager();
            if (shopManager == null) {
                return false;
            }

            // 使用反射检查是否存在删除方法
            Class<?> shopManagerClass = shopManager.getClass();

            // 检查常见的删除方法名
            String[] deleteMethodNames = {
                "deleteShop", "removeShop", "delete", "remove"
            };

            for (String methodName : deleteMethodNames) {
                try {
                    shopManagerClass.getMethod(methodName, Shop.class);
                    logger.info("找到商店删除方法: " + methodName);
                    return true;
                } catch (NoSuchMethodException ignored) {
                    // 继续检查下一个方法
                }
            }

            logger.info("QuickShop API不支持商店删除功能");
            return false;

        } catch (Exception e) {
            logger.warning("检查商店删除功能时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除指定商店
     *
     * @param shop 要删除的商店
     * @return 删除是否成功
     */
    public boolean removeShop(Shop shop) {
        if (!isQuickShopAvailable()) {
            logger.warning("QuickShop不可用，无法删除商店！");
            return false;
        }

        try {
            Object shopManager = quickShopAPI.getShopManager();
            if (shopManager == null) {
                logger.warning("无法获取ShopManager！");
                return false;
            }

            // 使用反射调用removeShop方法
            Class<?> shopManagerClass = shopManager.getClass();
            try {
                shopManagerClass.getMethod("removeShop", Shop.class).invoke(shopManager, shop);
                return true;
            } catch (NoSuchMethodException e) {
                logger.warning("找不到removeShop方法: " + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            logger.warning("删除商店时发生错误: " + e.getMessage());
            return false;
        }
    }





    /**
     * 尝试持久化删除商店
     *
     * @param shop 要删除的商店
     * @return 删除结果信息
     */
    public String attemptPersistentDelete(Shop shop) {
        if (!isQuickShopAvailable()) {
            return "QuickShop不可用";
        }

        StringBuilder result = new StringBuilder();
        result.append("尝试持久化删除商店: ").append(shop.toString()).append("\n");

        try {
            // 获取QuickShop实例
            Class<?> quickShopClass = Class.forName("org.maxgamer.quickshop.QuickShop");
            java.lang.reflect.Method getInstanceMethod = quickShopClass.getMethod("getInstance");
            Object quickShopInstance = getInstanceMethod.invoke(null);

            if (quickShopInstance == null) {
                result.append("无法获取QuickShop实例\n");
                return result.toString();
            }

            // 1. 先使用现有的removeShop方法（内存删除）
            boolean memoryDeleted = removeShop(shop);
            result.append("内存删除结果: ").append(memoryDeleted ? "成功" : "失败").append("\n");

            // 2. 尝试使用ShopPurger（商店清理器）
            try {
                java.lang.reflect.Method getShopPurgerMethod = quickShopClass.getMethod("getShopPurger");
                Object shopPurger = getShopPurgerMethod.invoke(quickShopInstance);

                if (shopPurger != null) {
                    result.append("成功获取ShopPurger: ").append(shopPurger.getClass().getName()).append("\n");

                    // 探索ShopPurger的方法
                    Class<?> shopPurgerClass = shopPurger.getClass();
                    java.lang.reflect.Method[] purgerMethods = shopPurgerClass.getDeclaredMethods();

                    for (java.lang.reflect.Method method : purgerMethods) {
                        String methodName = method.getName();
                        if (methodName.toLowerCase().contains("purge") ||
                            methodName.toLowerCase().contains("delete") ||
                            methodName.toLowerCase().contains("remove")) {
                            result.append("ShopPurger方法: ").append(methodName).append("(");
                            Class<?>[] params = method.getParameterTypes();
                            for (int i = 0; i < params.length; i++) {
                                if (i > 0) result.append(", ");
                                result.append(params[i].getSimpleName());
                            }
                            result.append(")\n");

                            // 尝试调用合适的方法
                            if (params.length == 1 && params[0].isAssignableFrom(shop.getClass())) {
                                try {
                                    method.invoke(shopPurger, shop);
                                    result.append("ShopPurger删除成功，使用方法: ").append(methodName).append("\n");
                                } catch (Exception e) {
                                    result.append("ShopPurger调用失败: ").append(e.getMessage()).append("\n");
                                }
                            } else if (params.length == 0 && methodName.equals("purge")) {
                                // 特别处理purge()方法（无参数）
                                try {
                                    method.invoke(shopPurger);
                                    result.append("ShopPurger全局清理成功，使用方法: ").append(methodName).append("\n");
                                } catch (Exception e) {
                                    result.append("ShopPurger全局清理失败: ").append(e.getMessage()).append("\n");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                result.append("获取ShopPurger失败: ").append(e.getMessage()).append("\n");
            }

            // 3. 尝试使用DatabaseManager
            try {
                java.lang.reflect.Method getDatabaseManagerMethod = quickShopClass.getMethod("getDatabaseManager");
                Object databaseManager = getDatabaseManagerMethod.invoke(quickShopInstance);

                if (databaseManager != null) {
                    result.append("成功获取DatabaseManager: ").append(databaseManager.getClass().getName()).append("\n");

                    // 探索DatabaseManager的方法
                    Class<?> databaseManagerClass = databaseManager.getClass();
                    java.lang.reflect.Method[] dbMethods = databaseManagerClass.getDeclaredMethods();

                    for (java.lang.reflect.Method method : dbMethods) {
                        String methodName = method.getName();
                        if (methodName.toLowerCase().contains("delete") ||
                            methodName.toLowerCase().contains("remove") ||
                            methodName.toLowerCase().contains("shop")) {
                            result.append("DatabaseManager方法: ").append(methodName).append("(");
                            Class<?>[] params = method.getParameterTypes();
                            for (int i = 0; i < params.length; i++) {
                                if (i > 0) result.append(", ");
                                result.append(params[i].getSimpleName());
                            }
                            result.append(")\n");
                        }
                    }
                }
            } catch (Exception e) {
                result.append("获取DatabaseManager失败: ").append(e.getMessage()).append("\n");
            }

            // 4. 尝试使用ShopManager的其他方法
            try {
                Object shopManager = quickShopAPI.getShopManager();
                if (shopManager != null) {
                    Class<?> shopManagerClass = shopManager.getClass();
                    result.append("探索ShopManager方法:\n");

                    java.lang.reflect.Method[] shopMethods = shopManagerClass.getDeclaredMethods();
                    for (java.lang.reflect.Method method : shopMethods) {
                        String methodName = method.getName();
                        if (methodName.toLowerCase().contains("delete") ||
                            methodName.toLowerCase().contains("remove") ||
                            methodName.toLowerCase().contains("unload") ||
                            methodName.toLowerCase().contains("purge")) {
                            result.append("ShopManager方法: ").append(methodName).append("(");
                            Class<?>[] params = method.getParameterTypes();
                            for (int i = 0; i < params.length; i++) {
                                if (i > 0) result.append(", ");
                                result.append(params[i].getSimpleName());
                            }
                            result.append(")\n");

                            // 尝试调用合适的方法
                            if (params.length == 1 && params[0].isAssignableFrom(shop.getClass())) {
                                try {
                                    method.invoke(shopManager, shop);
                                    result.append("ShopManager删除成功，使用方法: ").append(methodName).append("\n");
                                } catch (Exception e) {
                                    result.append("ShopManager调用失败: ").append(e.getMessage()).append("\n");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                result.append("探索ShopManager失败: ").append(e.getMessage()).append("\n");
            }

        } catch (Exception e) {
            result.append("持久化删除过程中发生错误: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }

    /**
     * 使用ShopPurger进行持久化删除
     *
     * @param shop 要删除的商店
     * @return 删除是否成功
     */
    public boolean deleteShopWithPurger(Shop shop) {
        if (!isQuickShopAvailable()) {
            logger.warning("QuickShop不可用，无法使用ShopPurger删除商店！");
            return false;
        }

        try {
            // 1. 先进行内存删除
            boolean memoryDeleted = removeShop(shop);
            if (!memoryDeleted) {
                logger.warning("内存删除失败，跳过ShopPurger操作");
                return false;
            }

            // 2. 获取QuickShop实例和ShopPurger
            Class<?> quickShopClass = Class.forName("org.maxgamer.quickshop.QuickShop");
            java.lang.reflect.Method getInstanceMethod = quickShopClass.getMethod("getInstance");
            Object quickShopInstance = getInstanceMethod.invoke(null);

            if (quickShopInstance == null) {
                logger.warning("无法获取QuickShop实例");
                return false;
            }

            java.lang.reflect.Method getShopPurgerMethod = quickShopClass.getMethod("getShopPurger");
            Object shopPurger = getShopPurgerMethod.invoke(quickShopInstance);

            if (shopPurger == null) {
                logger.warning("无法获取ShopPurger实例");
                return false;
            }

            // 3. 调用ShopPurger的purge方法
            Class<?> shopPurgerClass = shopPurger.getClass();

            // 尝试调用purge()方法（无参数，全局清理）
            try {
                java.lang.reflect.Method purgeMethod = shopPurgerClass.getMethod("purge");
                purgeMethod.invoke(shopPurger);
                logger.info("成功调用ShopPurger.purge()方法进行持久化清理");
                return true;
            } catch (NoSuchMethodException e) {
                logger.warning("找不到ShopPurger.purge()方法");
            } catch (Exception e) {
                logger.warning("调用ShopPurger.purge()方法失败: " + e.getMessage());
            }

            // 4. 尝试其他可能的删除方法
            java.lang.reflect.Method[] methods = shopPurgerClass.getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName();
                Class<?>[] params = method.getParameterTypes();

                // 寻找接受Shop参数的删除方法
                if ((methodName.toLowerCase().contains("purge") ||
                     methodName.toLowerCase().contains("delete") ||
                     methodName.toLowerCase().contains("remove")) &&
                    params.length == 1 &&
                    params[0].isAssignableFrom(shop.getClass())) {

                    try {
                        method.invoke(shopPurger, shop);
                        logger.info("成功调用ShopPurger." + methodName + "()方法删除商店");
                        return true;
                    } catch (Exception ex) {
                        logger.warning("调用ShopPurger." + methodName + "()方法失败: " + ex.getMessage());
                    }
                }
            }

            logger.warning("ShopPurger中没有找到合适的删除方法");
            return false;

        } catch (Exception e) {
            logger.warning("使用ShopPurger删除商店时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 尝试通过数据库直接删除商店
     *
     * @param shop 要删除的商店
     * @return 删除结果信息
     */
    public String attemptDatabaseDelete(Shop shop) {
        if (!isQuickShopAvailable()) {
            return "QuickShop不可用";
        }

        StringBuilder result = new StringBuilder();
        result.append("尝试数据库直接删除商店: ").append(shop.toString()).append("\n");

        try {
            // 1. 获取数据库连接
            Object shopManager = quickShopAPI.getShopManager();
            if (shopManager == null) {
                result.append("无法获取ShopManager\n");
                return result.toString();
            }

            // 2. 尝试获取数据库对象
            Class<?> shopManagerClass = shopManager.getClass();
            try {
                java.lang.reflect.Method getDatabaseMethod = shopManagerClass.getMethod("getDatabase");
                Object database = getDatabaseMethod.invoke(shopManager);

                if (database != null) {
                    result.append("成功获取数据库对象: ").append(database.getClass().getName()).append("\n");

                    // 3. 尝试获取数据库连接
                    try {
                        java.lang.reflect.Method getConnectionMethod = database.getClass().getMethod("getConnection");
                        Object connection = getConnectionMethod.invoke(database);

                        if (connection instanceof java.sql.Connection) {
                            java.sql.Connection conn = (java.sql.Connection) connection;
                            result.append("成功获取数据库连接\n");

                            // 4. 尝试删除商店记录
                            boolean deleted = deleteShopFromDatabase(conn, shop, result);
                            if (deleted) {
                                result.append("数据库删除成功\n");
                            } else {
                                result.append("数据库删除失败\n");
                            }
                        } else {
                            result.append("连接对象不是标准JDBC连接: ").append(connection.getClass().getName()).append("\n");
                        }
                    } catch (Exception e) {
                        result.append("获取数据库连接失败: ").append(e.getMessage()).append("\n");
                    }
                } else {
                    result.append("数据库对象为null\n");
                }
            } catch (Exception e) {
                result.append("获取数据库对象失败: ").append(e.getMessage()).append("\n");
            }

        } catch (Exception e) {
            result.append("数据库删除过程中发生错误: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }

    /**
     * 从数据库中删除商店记录
     *
     * @param conn 数据库连接
     * @param shop 要删除的商店
     * @param result 结果记录
     * @return 删除是否成功
     */
    private boolean deleteShopFromDatabase(java.sql.Connection conn, Shop shop, StringBuilder result) {
        try {
            // 常见的QuickShop表名
            String[] possibleTableNames = {
                "quickshop_shops", "qs_shops", "shops", "quickshop", "shop_data"
            };

            // 获取数据库元数据
            java.sql.DatabaseMetaData metaData = conn.getMetaData();

            // 查找实际的表名
            String actualTableName = null;
            for (String tableName : possibleTableNames) {
                java.sql.ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
                if (tables.next()) {
                    actualTableName = tableName;
                    result.append("找到商店表: ").append(tableName).append("\n");
                    break;
                }
                tables.close();
            }

            if (actualTableName == null) {
                result.append("未找到商店数据表\n");
                return false;
            }

            // 获取表结构
            java.sql.ResultSet columns = metaData.getColumns(null, null, actualTableName, null);
            result.append("表结构:\n");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                result.append("  - ").append(columnName).append(" (").append(columnType).append(")\n");
            }
            columns.close();

            // 尝试根据位置删除商店
            String deleteSQL = String.format(
                "DELETE FROM %s WHERE world = ? AND x = ? AND y = ? AND z = ?",
                actualTableName
            );

            try (java.sql.PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
                org.bukkit.Location loc = shop.getLocation();
                stmt.setString(1, loc.getWorld().getName());
                stmt.setInt(2, loc.getBlockX());
                stmt.setInt(3, loc.getBlockY());
                stmt.setInt(4, loc.getBlockZ());

                int deletedRows = stmt.executeUpdate();
                result.append("删除的记录数: ").append(deletedRows).append("\n");

                return deletedRows > 0;
            }

        } catch (Exception e) {
            result.append("数据库操作异常: ").append(e.getMessage()).append("\n");
            return false;
        }
    }

    /**
     * 尝试通过数据库直接删除商店（最终解决方案）
     *
     * @param shop 要删除的商店
     * @return 删除结果信息
     */
    public String attemptDirectDatabaseDelete(Shop shop) {
        if (!isQuickShopAvailable()) {
            return "QuickShop不可用";
        }

        StringBuilder result = new StringBuilder();
        result.append("尝试数据库直接删除商店: ").append(shop.toString()).append("\n");

        try {
            // 1. 先进行内存删除
            boolean memoryDeleted = removeShop(shop);
            result.append("内存删除结果: ").append(memoryDeleted ? "成功" : "失败").append("\n");

            // 2. 获取数据库连接
            java.sql.Connection connection = getDatabaseConnection();
            if (connection == null) {
                result.append("无法获取数据库连接\n");
                return result.toString();
            }

            result.append("成功获取数据库连接\n");

            // 3. 查找并删除商店记录
            boolean deleted = deleteShopFromDatabaseDirect(connection, shop, result);

            if (deleted) {
                result.append("数据库直接删除成功！\n");
            } else {
                result.append("数据库直接删除失败\n");
            }

            connection.close();

        } catch (Exception e) {
            result.append("数据库删除过程中发生错误: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }

    /**
     * 获取数据库连接
     */
    private java.sql.Connection getDatabaseConnection() {
        try {
            Class<?> quickShopClass = Class.forName("org.maxgamer.quickshop.QuickShop");
            java.lang.reflect.Method getInstanceMethod = quickShopClass.getMethod("getInstance");
            Object quickShopInstance = getInstanceMethod.invoke(null);

            if (quickShopInstance == null) {
                return null;
            }

            java.lang.reflect.Method getDatabaseManagerMethod = quickShopClass.getMethod("getDatabaseManager");
            Object databaseManager = getDatabaseManagerMethod.invoke(quickShopInstance);

            if (databaseManager == null) {
                return null;
            }

            Class<?> databaseManagerClass = databaseManager.getClass();

            // 尝试不同的获取连接方法
            String[] connectionMethods = {
                "getConnection", "getDataSource", "getDatabase"
            };

            for (String methodName : connectionMethods) {
                try {
                    java.lang.reflect.Method method = databaseManagerClass.getMethod(methodName);
                    Object result = method.invoke(databaseManager);

                    if (result instanceof java.sql.Connection) {
                        return (java.sql.Connection) result;
                    } else if (result != null) {
                        try {
                            java.lang.reflect.Method getConnMethod = result.getClass().getMethod("getConnection");
                            Object conn = getConnMethod.invoke(result);
                            if (conn instanceof java.sql.Connection) {
                                return (java.sql.Connection) conn;
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }

            return null;

        } catch (Exception e) {
            logger.warning("获取数据库连接失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从数据库中直接删除商店记录
     */
    private boolean deleteShopFromDatabaseDirect(java.sql.Connection conn, Shop shop, StringBuilder result) {
        try {
            // 1. 探索数据库表结构
            java.sql.DatabaseMetaData metaData = conn.getMetaData();
            result.append("数据库类型: ").append(metaData.getDatabaseProductName()).append("\n");

            // 2. 查找商店表
            String[] possibleTableNames = {
                "quickshop_shops", "qs_shops", "shops", "quickshop", "shop_data", "quickshop_data"
            };

            String actualTableName = null;
            for (String tableName : possibleTableNames) {
                java.sql.ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
                if (tables.next()) {
                    actualTableName = tableName;
                    result.append("找到商店表: ").append(tableName).append("\n");
                    break;
                }
                tables.close();
            }

            if (actualTableName == null) {
                // 列出所有表，寻找包含shop的表
                java.sql.ResultSet allTables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
                result.append("所有数据库表:\n");
                while (allTables.next()) {
                    String tableName = allTables.getString("TABLE_NAME");
                    result.append("  - ").append(tableName).append("\n");
                    if (tableName.toLowerCase().contains("shop")) {
                        actualTableName = tableName;
                        result.append("选择商店表: ").append(tableName).append("\n");
                    }
                }
                allTables.close();
            }

            if (actualTableName == null) {
                result.append("未找到商店数据表\n");
                return false;
            }

            // 3. 分析表结构
            java.sql.ResultSet columns = metaData.getColumns(null, null, actualTableName, null);
            result.append("表 ").append(actualTableName).append(" 的结构:\n");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                result.append("  - ").append(columnName).append(" (").append(columnType).append(")\n");
            }
            columns.close();

            // 4. 尝试删除商店记录
            org.bukkit.Location loc = shop.getLocation();

            // 尝试不同的删除策略
            String[] deleteStrategies = {
                "DELETE FROM " + actualTableName + " WHERE world = ? AND x = ? AND y = ? AND z = ?",
                "DELETE FROM " + actualTableName + " WHERE worldname = ? AND x = ? AND y = ? AND z = ?",
                "DELETE FROM " + actualTableName + " WHERE world_name = ? AND x = ? AND y = ? AND z = ?",
                "DELETE FROM " + actualTableName + " WHERE owner = ?",
            };

            for (String deleteSQL : deleteStrategies) {
                try {
                    java.sql.PreparedStatement stmt = conn.prepareStatement(deleteSQL);

                    if (deleteSQL.contains("owner")) {
                        stmt.setString(1, shop.getOwner().toString());
                    } else {
                        stmt.setString(1, loc.getWorld().getName());
                        stmt.setInt(2, loc.getBlockX());
                        stmt.setInt(3, loc.getBlockY());
                        stmt.setInt(4, loc.getBlockZ());
                    }

                    int deletedRows = stmt.executeUpdate();
                    result.append("删除策略: ").append(deleteSQL).append("\n");
                    result.append("删除的记录数: ").append(deletedRows).append("\n");

                    stmt.close();

                    if (deletedRows > 0) {
                        return true;
                    }

                } catch (Exception e) {
                    result.append("删除策略失败: ").append(e.getMessage()).append("\n");
                }
            }

            return false;

        } catch (Exception e) {
            result.append("数据库操作异常: ").append(e.getMessage()).append("\n");
            return false;
        }
    }

    /**
     * 重新初始化QuickShop连接
     */
    public void reinitialize() {
        logger.info("重新初始化QuickShop连接...");
        initializeQuickShop();
    }
}
