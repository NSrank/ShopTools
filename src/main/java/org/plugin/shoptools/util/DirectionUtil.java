package org.plugin.shoptools.util;

import org.bukkit.Location;

/**
 * 方向计算工具类
 * 用于计算目标相对于玩家的方向（东西南北）
 * 
 * @author NSrank & Augment
 */
public class DirectionUtil {
    
    /**
     * 计算目标相对于玩家的方向
     * 
     * @param playerLocation 玩家位置
     * @param targetLocation 目标位置
     * @return 方向字符串（E、W、S、N）
     */
    public static String getDirection(Location playerLocation, Location targetLocation) {
        if (playerLocation == null || targetLocation == null) {
            return "";
        }
        
        // 检查是否在同一世界
        if (!playerLocation.getWorld().equals(targetLocation.getWorld())) {
            return "";
        }
        
        // 计算相对坐标差
        double deltaX = targetLocation.getX() - playerLocation.getX();
        double deltaZ = targetLocation.getZ() - playerLocation.getZ();
        
        // 如果距离太近，不显示方向
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 1.0) {
            return "";
        }
        
        // 计算角度（以北为0度，顺时针）
        double angle = Math.atan2(deltaX, -deltaZ) * 180.0 / Math.PI;
        if (angle < 0) {
            angle += 360.0;
        }
        
        // 根据角度确定方向
        // 北: 337.5° - 22.5° (0°)
        // 东北: 22.5° - 67.5°
        // 东: 67.5° - 112.5°
        // 东南: 112.5° - 157.5°
        // 南: 157.5° - 202.5°
        // 西南: 202.5° - 247.5°
        // 西: 247.5° - 292.5°
        // 西北: 292.5° - 337.5°
        
        if (angle >= 337.5 || angle < 22.5) {
            return "N";  // 北
        } else if (angle >= 22.5 && angle < 67.5) {
            return "NE"; // 东北
        } else if (angle >= 67.5 && angle < 112.5) {
            return "E";  // 东
        } else if (angle >= 112.5 && angle < 157.5) {
            return "SE"; // 东南
        } else if (angle >= 157.5 && angle < 202.5) {
            return "S";  // 南
        } else if (angle >= 202.5 && angle < 247.5) {
            return "SW"; // 西南
        } else if (angle >= 247.5 && angle < 292.5) {
            return "W";  // 西
        } else {
            return "NW"; // 西北
        }
    }
    
    /**
     * 计算目标相对于玩家的简化方向（仅四个主方向）
     * 
     * @param playerLocation 玩家位置
     * @param targetLocation 目标位置
     * @return 方向字符串（E、W、S、N）
     */
    public static String getSimpleDirection(Location playerLocation, Location targetLocation) {
        if (playerLocation == null || targetLocation == null) {
            return "";
        }
        
        // 检查是否在同一世界
        if (!playerLocation.getWorld().equals(targetLocation.getWorld())) {
            return "";
        }
        
        // 计算相对坐标差
        double deltaX = targetLocation.getX() - playerLocation.getX();
        double deltaZ = targetLocation.getZ() - playerLocation.getZ();
        
        // 如果距离太近，不显示方向
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 1.0) {
            return "";
        }
        
        // 比较绝对值来确定主要方向
        double absX = Math.abs(deltaX);
        double absZ = Math.abs(deltaZ);
        
        if (absX > absZ) {
            // X轴方向更明显
            return deltaX > 0 ? "E" : "W";  // 东或西
        } else {
            // Z轴方向更明显
            return deltaZ > 0 ? "S" : "N";  // 南或北
        }
    }
    
    /**
     * 获取方向的中文名称
     * 
     * @param direction 英文方向代码
     * @return 中文方向名称
     */
    public static String getDirectionName(String direction) {
        switch (direction.toUpperCase()) {
            case "N": return "北";
            case "NE": return "东北";
            case "E": return "东";
            case "SE": return "东南";
            case "S": return "南";
            case "SW": return "西南";
            case "W": return "西";
            case "NW": return "西北";
            default: return "";
        }
    }
    
    /**
     * 格式化带方向的距离文本
     * 
     * @param playerLocation 玩家位置
     * @param targetLocation 目标位置
     * @param distance 距离
     * @return 格式化的距离文本，如 "E 15.3m" 或 "200m+"
     */
    public static String formatDistanceWithDirection(Location playerLocation, Location targetLocation, double distance) {
        if (playerLocation == null || targetLocation == null) {
            return "unknown";
        }
        
        // 检查是否在同一世界
        if (!targetLocation.getWorld().equals(playerLocation.getWorld())) {
            return "otherworld";
        }
        
        // 超过200格显示"200m+"
        if (distance > 200.0) {
            return "200m+";
        }
        
        // 获取方向
        String direction = getSimpleDirection(playerLocation, targetLocation);
        
        // 格式化距离
        String distanceStr = String.format("%.1fm", distance);
        
        // 组合方向和距离
        if (!direction.isEmpty()) {
            return direction + " " + distanceStr;
        } else {
            return distanceStr;
        }
    }
}
