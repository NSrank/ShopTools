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
    private int stock;
    @Expose
    private final boolean isUnlimited;
    // 不序列化ItemStack，因为它可能包含复杂的内部结构
    private final ItemStack item;
    // 库存是否已被扫描器确认过（非持久化，服务器重启后需重新扫描）
    private boolean stockKnown = false;
    
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
     * @param isUnlimited 是否为无限商店
     * @param item 物品堆栈
     */
    public ShopData(UUID shopId, String itemId, String itemDisplayName, Location location,
                   double price, UUID ownerId, String ownerName, ShopType shopType,
                   int stock, boolean isUnlimited, ItemStack item) {
        this.shopId = shopId;
        this.itemId = itemId;
        this.itemDisplayName = itemDisplayName;
        this.location = location;
        this.price = price;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.shopType = shopType;
        this.stock = stock;
        this.isUnlimited = isUnlimited;
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
    public boolean isUnlimited() { return isUnlimited; }
    public ItemStack getItem() { return item; }
    public boolean isStockKnown() { return stockKnown; }

    /**
     * 更新库存数量并标记为已确认。
     * 仅由 StockScanQueue 在区块加载完成后调用。
     *
     * @param stock 从 QuickShop API 读取到的实际库存数量
     */
    public void setStock(int stock) {
        this.stock = stock;
        this.stockKnown = true;
    }

    /**
     * 获取显示用的店主名称
     * 如果是无限商店，返回"系统商店"
     *
     * @return 显示用的店主名称
     */
    public String getDisplayOwnerName() {
        return isUnlimited ? "系统商店" : ownerName;
    }

    /**
     * 获取商店状态描述
     *
     * @return 商店状态（"无限" 或 "普通"）
     */
    public String getStatusDescription() {
        return isUnlimited ? "无限" : "普通";
    }
    
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
     * 检查商店是否售罄。
     * <p>
     * 仅当 {@link #isStockKnown()} 为 {@code true}（即 StockScanQueue 已对该商店完成扫描）时
     * 才会返回有意义的结果，否则始终返回 {@code false} 以避免误报。
     * 无限商店（系统商店）永远不会售罄。
     *
     * @return 如果是售卖商店且库存已确认为 0，返回 {@code true}；否则返回 {@code false}
     */
    public boolean isOutOfStock() {
        if (!stockKnown || isUnlimited) {
            return false;
        }
        // 仅对售卖商店检查售罄
        return shopType == ShopType.SELLING && stock == 0;
    }

    /**
     * 获取库存状态文本，用于命令输出追加显示。
     *
     * @return 售罄时返回 {@code "&c售罄"}，否则返回空字符串
     */
    public String getStockStatusText() {
        return isOutOfStock() ? "&c售罄" : "";
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
