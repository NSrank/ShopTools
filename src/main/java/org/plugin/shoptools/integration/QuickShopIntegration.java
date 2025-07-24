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
     * 重新初始化QuickShop连接
     */
    public void reinitialize() {
        logger.info("重新初始化QuickShop连接...");
        initializeQuickShop();
    }
}
