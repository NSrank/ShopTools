package org.plugin.shoptools.util;

import org.plugin.shoptools.model.ShopData;

import java.util.Comparator;
import java.util.List;

/**
 * 商店数据排序工具类
 * 提供各种排序方式的实现
 * 
 * @author NSrank & Augment
 */
public class ShopSorter {
    
    /**
     * 按物品ID字母序排序（0-9, A-Z）
     */
    public static final Comparator<ShopData> BY_ITEM_ID = (shop1, shop2) -> {
        String id1 = shop1.getItemId();
        String id2 = shop2.getItemId();
        
        // 处理null值
        if (id1 == null && id2 == null) return 0;
        if (id1 == null) return 1;
        if (id2 == null) return -1;
        
        // 自然排序，数字在前，字母在后
        return compareAlphanumeric(id1, id2);
    };
    
    /**
     * 按价格升序排序
     */
    public static final Comparator<ShopData> BY_PRICE_ASC = Comparator.comparing(ShopData::getPrice);
    
    /**
     * 按价格降序排序
     */
    public static final Comparator<ShopData> BY_PRICE_DESC = Comparator.comparing(ShopData::getPrice).reversed();
    
    /**
     * 按店主名称字母序排序
     */
    public static final Comparator<ShopData> BY_OWNER_NAME = (shop1, shop2) -> {
        String name1 = shop1.getOwnerName();
        String name2 = shop2.getOwnerName();
        
        // 处理null值
        if (name1 == null && name2 == null) return 0;
        if (name1 == null) return 1;
        if (name2 == null) return -1;
        
        return name1.compareToIgnoreCase(name2);
    };
    
    /**
     * 按商店ID字母序排序
     */
    public static final Comparator<ShopData> BY_SHOP_ID = (shop1, shop2) -> {
        String id1 = shop1.getShopId().toString();
        String id2 = shop2.getShopId().toString();
        return id1.compareToIgnoreCase(id2);
    };
    
    /**
     * 按库存数量降序排序
     */
    public static final Comparator<ShopData> BY_STOCK_DESC = Comparator.comparing(ShopData::getStock).reversed();
    
    /**
     * 复合排序：先按物品ID，再按价格
     */
    public static final Comparator<ShopData> BY_ITEM_THEN_PRICE = BY_ITEM_ID.thenComparing(BY_PRICE_ASC);
    
    /**
     * 复合排序：先按店主，再按物品ID
     */
    public static final Comparator<ShopData> BY_OWNER_THEN_ITEM = BY_OWNER_NAME.thenComparing(BY_ITEM_ID);
    
    /**
     * 排序类型枚举
     */
    public enum SortType {
        ITEM_ID("按物品ID排序", BY_ITEM_ID),
        PRICE_ASC("按价格升序", BY_PRICE_ASC),
        PRICE_DESC("按价格降序", BY_PRICE_DESC),
        OWNER_NAME("按店主名称排序", BY_OWNER_NAME),
        SHOP_ID("按商店ID排序", BY_SHOP_ID),
        STOCK_DESC("按库存降序", BY_STOCK_DESC),
        ITEM_THEN_PRICE("按物品ID和价格排序", BY_ITEM_THEN_PRICE),
        OWNER_THEN_ITEM("按店主和物品ID排序", BY_OWNER_THEN_ITEM);
        
        private final String description;
        private final Comparator<ShopData> comparator;
        
        SortType(String description, Comparator<ShopData> comparator) {
            this.description = description;
            this.comparator = comparator;
        }
        
        public String getDescription() {
            return description;
        }
        
        public Comparator<ShopData> getComparator() {
            return comparator;
        }
    }
    
    /**
     * 使用指定的排序类型对商店列表进行排序
     * 
     * @param shops 商店列表
     * @param sortType 排序类型
     */
    public static void sortShops(List<ShopData> shops, SortType sortType) {
        if (shops == null || shops.isEmpty() || sortType == null) {
            return;
        }
        
        shops.sort(sortType.getComparator());
    }
    
    /**
     * 使用自定义比较器对商店列表进行排序
     * 
     * @param shops 商店列表
     * @param comparator 比较器
     */
    public static void sortShops(List<ShopData> shops, Comparator<ShopData> comparator) {
        if (shops == null || shops.isEmpty() || comparator == null) {
            return;
        }
        
        shops.sort(comparator);
    }
    
    /**
     * 字母数字混合排序比较
     * 确保数字在字母前面，并且数字按数值大小排序
     * 
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 比较结果
     */
    private static int compareAlphanumeric(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        int i = 0, j = 0;
        
        while (i < len1 && j < len2) {
            char c1 = str1.charAt(i);
            char c2 = str2.charAt(j);
            
            // 如果都是数字，按数值比较
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                int num1 = 0, num2 = 0;
                
                // 提取完整的数字
                while (i < len1 && Character.isDigit(str1.charAt(i))) {
                    num1 = num1 * 10 + (str1.charAt(i) - '0');
                    i++;
                }
                
                while (j < len2 && Character.isDigit(str2.charAt(j))) {
                    num2 = num2 * 10 + (str2.charAt(j) - '0');
                    j++;
                }
                
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            } else {
                // 数字优先于字母
                if (Character.isDigit(c1) && !Character.isDigit(c2)) {
                    return -1;
                }
                if (!Character.isDigit(c1) && Character.isDigit(c2)) {
                    return 1;
                }
                
                // 都是字母，按字典序比较
                int cmp = Character.toLowerCase(c1) - Character.toLowerCase(c2);
                if (cmp != 0) {
                    return cmp;
                }
                
                i++;
                j++;
            }
        }
        
        // 比较剩余长度
        return Integer.compare(len1, len2);
    }
}
