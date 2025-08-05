package org.plugin.shoptools.model;

import com.google.gson.annotations.Expose;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * 商店数据模型类
 * 用于存储从QuickShop获取的商店信息
 * 
 * @author NSrank & Augment
 */
public class ShopData {

    @Expose
    private final UUID shopId;
    @Expose
    private final String itemId;
    @Expose
    private final String itemDisplayName;
    @Expose
    private final Location location;
    @Expose
    private final double price;
    @Expose
    private final UUID ownerId;
    @Expose
    private final String ownerName;
    @Expose
    private final ShopType shopType;
    @Expose
    private final int stock;
    // 不序列化ItemStack，因为它可能包含复杂的内部结构
    private final ItemStack item;
    
    /**
     * 商店类型枚举
     */
    public enum ShopType {
        SELLING,    // 售卖商店
        BUYING,     // 收购商店
        BOTH        // 双向商店
    }
    
    /**
     * 构造函数
     * 
     * @param shopId 商店唯一ID
     * @param itemId 物品ID
     * @param itemDisplayName 物品显示名称
     * @param location 商店位置
     * @param price 价格
     * @param ownerId 店主UUID
     * @param ownerName 店主名称
     * @param shopType 商店类型
     * @param stock 库存数量
     * @param item 物品堆栈
     */
    public ShopData(UUID shopId, String itemId, String itemDisplayName, Location location, 
                   double price, UUID ownerId, String ownerName, ShopType shopType, 
                   int stock, ItemStack item) {
        this.shopId = shopId;
        this.itemId = itemId;
        this.itemDisplayName = itemDisplayName;
        this.location = location;
        this.price = price;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.shopType = shopType;
        this.stock = stock;
        this.item = item;
    }
    
    // Getter方法
    public UUID getShopId() { return shopId; }
    public String getItemId() { return itemId; }
    public String getItemDisplayName() { return itemDisplayName; }
    public Location getLocation() { return location; }
    public double getPrice() { return price; }
    public UUID getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public ShopType getShopType() { return shopType; }
    public int getStock() { return stock; }
    public ItemStack getItem() { return item; }
    
    /**
     * 获取格式化的位置字符串
     * 
     * @return 格式化的位置信息
     */
    public String getFormattedLocation() {
        if (location == null) return "未知位置";
        return String.format("%s (%d, %d, %d)", 
            location.getWorld() != null ? location.getWorld().getName() : "未知世界",
            location.getBlockX(), 
            location.getBlockY(), 
            location.getBlockZ());
    }
    
    /**
     * 获取格式化的价格字符串
     * 
     * @return 格式化的价格信息
     */
    public String getFormattedPrice() {
        return String.format("%.2f", price);
    }
    
    /**
     * 获取商店的简短描述
     * 
     * @return 商店描述字符串
     */
    public String getShopDescription() {
        return String.format("%s %s %.2f %s", 
            itemDisplayName != null ? itemDisplayName : itemId,
            getFormattedLocation(),
            price,
            ownerName != null ? ownerName : "未知玩家");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopData shopData = (ShopData) o;
        return Objects.equals(shopId, shopData.shopId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(shopId);
    }
    
    /**
     * 检查商店是否售罄
     *
     * @return 如果商店售罄返回true
     */
    public boolean isOutOfStock() {
        // 对于售卖商店，库存为0或负数表示售罄
        if (shopType == ShopType.SELLING) {
            return stock <= 0;
        }
        // 对于收购商店，不存在售罄概念
        return false;
    }

    /**
     * 获取库存状态文本
     *
     * @return 库存状态文本，如果售罄返回红色的"售罄"
     */
    public String getStockStatusText() {
        if (isOutOfStock()) {
            return "&c售罄";
        }
        return "";
    }

    @Override
    public String toString() {
        return "ShopData{" +
                "shopId=" + shopId +
                ", itemId='" + itemId + '\'' +
                ", location=" + getFormattedLocation() +
                ", price=" + price +
                ", ownerName='" + ownerName + '\'' +
                ", shopType=" + shopType +
                ", stock=" + stock +
                '}';
    }
}
