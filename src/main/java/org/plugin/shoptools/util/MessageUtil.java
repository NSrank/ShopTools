package org.plugin.shoptools.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * 消息工具类
 * 提供消息格式化和发送功能
 * 
 * @author NSrank & Augment
 */
public class MessageUtil {
    
    /**
     * 发送格式化消息给命令发送者
     * 
     * @param sender 命令发送者
     * @param message 消息内容
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null) {
            return;
        }
        
        String formattedMessage = formatMessage(message);
        sender.sendMessage(formattedMessage);
    }
    
    /**
     * 格式化消息，将颜色代码转换为实际颜色
     * 
     * @param message 原始消息
     * @return 格式化后的消息
     */
    public static String formatMessage(String message) {
        if (message == null) {
            return "";
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 发送带前缀的消息
     * 
     * @param sender 命令发送者
     * @param prefix 消息前缀
     * @param message 消息内容
     */
    public static void sendMessageWithPrefix(CommandSender sender, String prefix, String message) {
        if (sender == null || message == null) {
            return;
        }
        
        String fullMessage = (prefix != null ? prefix : "") + message;
        sendMessage(sender, fullMessage);
    }
}
