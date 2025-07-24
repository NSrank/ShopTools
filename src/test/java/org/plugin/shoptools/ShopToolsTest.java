package org.plugin.shoptools;

import org.junit.jupiter.api.Test;
import org.plugin.shoptools.model.ShopData;
import org.plugin.shoptools.util.ShopSorter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ShopTools插件测试类
 * 
 * @author NSrank & Augment
 */
public class ShopToolsTest {
    
    /**
     * 测试商店数据排序功能
     */
    @Test
    public void testShopSorting() {
        // 创建测试数据
        List<ShopData> shops = createTestShops();
        
        // 测试按物品ID排序
        ShopSorter.sortShops(shops, ShopSorter.SortType.ITEM_ID);
        assertEquals("APPLE", shops.get(0).getItemId());
        assertEquals("DIAMOND", shops.get(1).getItemId());
        assertEquals("STONE", shops.get(2).getItemId());
        
        // 测试按价格排序
        ShopSorter.sortShops(shops, ShopSorter.SortType.PRICE_ASC);
        assertEquals(1.0, shops.get(0).getPrice());
        assertEquals(10.0, shops.get(1).getPrice());
        assertEquals(100.0, shops.get(2).getPrice());
    }
    
    /**
     * 测试商店数据模型
     */
    @Test
    public void testShopDataModel() {
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        
        ShopData shop = new ShopData(
            shopId,
            "DIAMOND",
            "钻石",
            null,
            100.0,
            ownerId,
            "TestPlayer",
            ShopData.ShopType.SELLING,
            64,
            null
        );
        
        assertEquals(shopId, shop.getShopId());
        assertEquals("DIAMOND", shop.getItemId());
        assertEquals("钻石", shop.getItemDisplayName());
        assertEquals(100.0, shop.getPrice());
        assertEquals(ownerId, shop.getOwnerId());
        assertEquals("TestPlayer", shop.getOwnerName());
        assertEquals(ShopData.ShopType.SELLING, shop.getShopType());
        assertEquals(64, shop.getStock());
        
        // 测试格式化方法
        assertEquals("100.00", shop.getFormattedPrice());
        assertTrue(shop.getShopDescription().contains("钻石"));
        assertTrue(shop.getShopDescription().contains("100.00"));
        assertTrue(shop.getShopDescription().contains("TestPlayer"));
    }
    
    /**
     * 测试字母数字排序
     */
    @Test
    public void testAlphanumericSorting() {
        List<ShopData> shops = new ArrayList<>();
        
        // 添加包含数字的物品ID
        shops.add(createShop("ITEM_1", 10.0));
        shops.add(createShop("ITEM_10", 20.0));
        shops.add(createShop("ITEM_2", 30.0));
        shops.add(createShop("APPLE", 40.0));
        shops.add(createShop("1_SPECIAL", 50.0));
        
        ShopSorter.sortShops(shops, ShopSorter.SortType.ITEM_ID);
        
        // 验证排序结果：数字在前，然后是字母
        assertEquals("1_SPECIAL", shops.get(0).getItemId());
        assertEquals("APPLE", shops.get(1).getItemId());
        assertEquals("ITEM_1", shops.get(2).getItemId());
        assertEquals("ITEM_2", shops.get(3).getItemId());
        assertEquals("ITEM_10", shops.get(4).getItemId());
    }
    
    /**
     * 测试空值处理
     */
    @Test
    public void testNullHandling() {
        List<ShopData> shops = new ArrayList<>();
        
        // 添加包含null值的商店
        shops.add(new ShopData(
            UUID.randomUUID(),
            null,
            null,
            null,
            0.0,
            UUID.randomUUID(),
            null,
            ShopData.ShopType.SELLING,
            0,
            null
        ));
        
        shops.add(createShop("DIAMOND", 100.0));
        
        // 测试排序不会抛出异常
        assertDoesNotThrow(() -> {
            ShopSorter.sortShops(shops, ShopSorter.SortType.ITEM_ID);
            ShopSorter.sortShops(shops, ShopSorter.SortType.OWNER_NAME);
        });
    }
    
    /**
     * 创建测试商店数据
     */
    private List<ShopData> createTestShops() {
        List<ShopData> shops = new ArrayList<>();
        
        shops.add(createShop("STONE", 1.0));
        shops.add(createShop("DIAMOND", 100.0));
        shops.add(createShop("APPLE", 10.0));
        
        return shops;
    }
    
    /**
     * 创建单个测试商店
     */
    private ShopData createShop(String itemId, double price) {
        return new ShopData(
            UUID.randomUUID(),
            itemId,
            itemId.toLowerCase(),
            null,
            price,
            UUID.randomUUID(),
            "TestPlayer",
            ShopData.ShopType.SELLING,
            64,
            null
        );
    }
}
