package org.plugin.shoptools.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 商店备份管理器
 * 用于备份和恢复被删除的商店数据
 * 
 * @author NSrank & Augment
 */
public class ShopBackupManager {
    
    private final File backupFolder;
    private final Gson gson;
    private final Logger logger;
    
    /**
     * 商店备份数据类
     */
    public static class ShopBackupData {
        private String shopId;
        private String ownerId; // 改为String避免UUID序列化问题
        private String ownerName;
        private String worldName; // 分解Location为简单字段
        private int x;
        private int y;
        private int z;
        private String itemType; // 改为String避免Material序列化问题
        private int amount;
        private double price;
        private String shopType; // "SELLING" or "BUYING"
        private String backupTime;
        private String backupReason;

        // 构造函数
        public ShopBackupData(String shopId, UUID ownerId, String ownerName, Location location,
                             Material itemType, int amount, double price, String shopType,
                             String backupReason) {
            this.shopId = shopId;
            this.ownerId = ownerId.toString(); // 转换为String
            this.ownerName = ownerName;

            // 分解Location为简单字段
            if (location != null) {
                this.worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
                this.x = location.getBlockX();
                this.y = location.getBlockY();
                this.z = location.getBlockZ();
            } else {
                this.worldName = "unknown";
                this.x = 0;
                this.y = 0;
                this.z = 0;
            }

            this.itemType = itemType.name(); // 转换为String
            this.amount = amount;
            this.price = price;
            this.shopType = shopType;
            this.backupTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.backupReason = backupReason;
        }
        
        // Getters
        public String getShopId() { return shopId; }
        public String getOwnerId() { return ownerId; }
        public String getOwnerName() { return ownerName; }
        public String getWorldName() { return worldName; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public String getItemType() { return itemType; }
        public int getAmount() { return amount; }
        public double getPrice() { return price; }
        public String getShopType() { return shopType; }
        public String getBackupTime() { return backupTime; }
        public String getBackupReason() { return backupReason; }

        // 便利方法：获取格式化的位置信息
        public String getFormattedLocation() {
            return String.format("%s (%d, %d, %d)", worldName, x, y, z);
        }
    }
    
    /**
     * 玩家备份数据类
     */
    public static class PlayerBackupData {
        private String playerId; // 改为String避免UUID序列化问题
        private String playerName;
        private String backupTime;
        private String backupReason;
        private List<ShopBackupData> shops;

        public PlayerBackupData(UUID playerId, String playerName, String backupReason) {
            this.playerId = playerId.toString(); // 转换为String
            this.playerName = playerName;
            this.backupTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.backupReason = backupReason;
            this.shops = new ArrayList<>();
        }

        // Getters and Setters
        public String getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public String getBackupTime() { return backupTime; }
        public String getBackupReason() { return backupReason; }
        public List<ShopBackupData> getShops() { return shops; }
        public void setShops(List<ShopBackupData> shops) { this.shops = shops; }
    }
    
    public ShopBackupManager(File dataFolder, Logger logger) {
        this.backupFolder = new File(dataFolder, "shop_backups");
        this.logger = logger;
        
        // 确保备份文件夹存在
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
        
        // 创建Gson实例
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * 备份玩家的商店数据
     * 
     * @param playerBackupData 玩家备份数据
     * @return 备份是否成功
     */
    public boolean backupPlayerShops(PlayerBackupData playerBackupData) {
        try {
            String fileName = String.format("%s_%s.json", 
                playerBackupData.getPlayerName(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            );
            
            File backupFile = new File(backupFolder, fileName);
            
            try (FileWriter writer = new FileWriter(backupFile)) {
                gson.toJson(playerBackupData, writer);
            }
            
            logger.info(String.format("成功备份玩家 %s 的 %d 个商店到文件: %s", 
                playerBackupData.getPlayerName(), 
                playerBackupData.getShops().size(),
                fileName));
            
            return true;
            
        } catch (IOException e) {
            logger.severe("备份玩家商店数据时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取玩家的最新备份数据
     * 
     * @param playerName 玩家名
     * @return 最新的备份数据，如果不存在返回null
     */
    public PlayerBackupData getLatestPlayerBackup(String playerName) {
        File[] backupFiles = backupFolder.listFiles((dir, name) -> 
            name.startsWith(playerName + "_") && name.endsWith(".json"));
        
        if (backupFiles == null || backupFiles.length == 0) {
            return null;
        }
        
        // 找到最新的备份文件
        File latestFile = null;
        long latestTime = 0;
        
        for (File file : backupFiles) {
            if (file.lastModified() > latestTime) {
                latestTime = file.lastModified();
                latestFile = file;
            }
        }
        
        if (latestFile == null) {
            return null;
        }
        
        try (FileReader reader = new FileReader(latestFile)) {
            Type type = new TypeToken<PlayerBackupData>(){}.getType();
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            logger.warning("读取备份文件时发生错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 列出所有备份文件
     * 
     * @return 备份文件列表
     */
    public List<String> listBackupFiles() {
        File[] backupFiles = backupFolder.listFiles((dir, name) -> name.endsWith(".json"));
        List<String> fileNames = new ArrayList<>();
        
        if (backupFiles != null) {
            for (File file : backupFiles) {
                fileNames.add(file.getName());
            }
        }
        
        return fileNames;
    }
    
    /**
     * 删除指定的备份文件
     * 
     * @param fileName 文件名
     * @return 删除是否成功
     */
    public boolean deleteBackupFile(String fileName) {
        File backupFile = new File(backupFolder, fileName);
        if (backupFile.exists()) {
            return backupFile.delete();
        }
        return false;
    }
}
