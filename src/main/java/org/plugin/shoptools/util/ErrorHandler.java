package org.plugin.shoptools.util;

import org.bukkit.command.CommandSender;
import org.plugin.shoptools.config.ConfigManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 错误处理工具类
 * 提供统一的错误处理和日志记录功能
 * 
 * @author NSrank & Augment
 */
public class ErrorHandler {
    
    private final Logger logger;
    private final ConfigManager configManager;
    
    /**
     * 构造函数
     * 
     * @param logger 日志记录器
     * @param configManager 配置管理器
     */
    public ErrorHandler(Logger logger, ConfigManager configManager) {
        this.logger = logger;
        this.configManager = configManager;
    }
    
    /**
     * 处理一般错误
     * 
     * @param message 错误消息
     * @param throwable 异常对象
     */
    public void handleError(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
        
        if (configManager.isDebugEnabled()) {
            logger.info("调试信息: " + getDebugInfo(throwable));
        }
    }
    
    /**
     * 处理警告
     * 
     * @param message 警告消息
     */
    public void handleWarning(String message) {
        logger.warning(message);
    }
    
    /**
     * 处理警告（带异常）
     * 
     * @param message 警告消息
     * @param throwable 异常对象
     */
    public void handleWarning(String message, Throwable throwable) {
        logger.log(Level.WARNING, message, throwable);
        
        if (configManager.isDebugEnabled()) {
            logger.info("调试信息: " + getDebugInfo(throwable));
        }
    }
    
    /**
     * 处理命令执行错误
     * 
     * @param sender 命令发送者
     * @param command 命令名称
     * @param throwable 异常对象
     */
    public void handleCommandError(CommandSender sender, String command, Throwable throwable) {
        String errorMessage = "执行命令 '" + command + "' 时发生错误: " + throwable.getMessage();
        logger.log(Level.SEVERE, errorMessage, throwable);
        
        // 向用户发送友好的错误消息
        MessageUtil.sendMessage(sender, "&c命令执行失败！请联系管理员查看详细错误信息。");
        
        if (configManager.isDebugEnabled() && sender.hasPermission("shoptools.admin")) {
            MessageUtil.sendMessage(sender, "&7调试信息: " + throwable.getMessage());
        }
    }
    
    /**
     * 处理数据同步错误
     * 
     * @param operation 操作名称
     * @param throwable 异常对象
     */
    public void handleSyncError(String operation, Throwable throwable) {
        String errorMessage = "数据同步操作 '" + operation + "' 失败: " + throwable.getMessage();
        logger.log(Level.SEVERE, errorMessage, throwable);
        
        if (configManager.isDebugEnabled()) {
            logger.info("同步错误调试信息: " + getDebugInfo(throwable));
        }
    }
    
    /**
     * 处理配置错误
     * 
     * @param configKey 配置键
     * @param throwable 异常对象
     */
    public void handleConfigError(String configKey, Throwable throwable) {
        String errorMessage = "配置项 '" + configKey + "' 处理失败: " + throwable.getMessage();
        logger.log(Level.SEVERE, errorMessage, throwable);
    }
    
    /**
     * 记录调试信息
     * 
     * @param message 调试消息
     */
    public void debug(String message) {
        if (configManager.isDebugEnabled()) {
            logger.info("[DEBUG] " + message);
        }
    }
    
    /**
     * 记录信息
     * 
     * @param message 信息消息
     */
    public void info(String message) {
        logger.info(message);
    }
    
    /**
     * 安全执行操作
     * 
     * @param operation 操作名称
     * @param runnable 要执行的操作
     * @return 操作是否成功
     */
    public boolean safeExecute(String operation, Runnable runnable) {
        try {
            runnable.run();
            return true;
        } catch (Exception e) {
            handleError("执行操作 '" + operation + "' 时发生错误", e);
            return false;
        }
    }
    
    /**
     * 安全执行操作（带返回值）
     * 
     * @param operation 操作名称
     * @param supplier 要执行的操作
     * @param defaultValue 默认返回值
     * @param <T> 返回值类型
     * @return 操作结果或默认值
     */
    public <T> T safeExecute(String operation, java.util.function.Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            handleError("执行操作 '" + operation + "' 时发生错误", e);
            return defaultValue;
        }
    }
    
    /**
     * 获取异常的调试信息
     * 
     * @param throwable 异常对象
     * @return 调试信息字符串
     */
    private String getDebugInfo(Throwable throwable) {
        if (throwable == null) {
            return "无异常信息";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("异常类型: ").append(throwable.getClass().getSimpleName()).append("\n");
        sb.append("异常消息: ").append(throwable.getMessage()).append("\n");
        
        if (throwable.getCause() != null) {
            sb.append("根本原因: ").append(throwable.getCause().getMessage()).append("\n");
        }
        
        // 添加关键的堆栈跟踪信息
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length > 0) {
            sb.append("发生位置: ");
            for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
                StackTraceElement element = stackTrace[i];
                if (element.getClassName().contains("shoptools")) {
                    sb.append(element.getClassName()).append(".").append(element.getMethodName())
                      .append("(").append(element.getFileName()).append(":").append(element.getLineNumber()).append(")");
                    break;
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 验证对象不为空
     * 
     * @param obj 要验证的对象
     * @param name 对象名称
     * @throws IllegalArgumentException 如果对象为空
     */
    public static void requireNonNull(Object obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }
    
    /**
     * 验证字符串不为空
     * 
     * @param str 要验证的字符串
     * @param name 字符串名称
     * @throws IllegalArgumentException 如果字符串为空或空白
     */
    public static void requireNonEmpty(String str, String name) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空或空白");
        }
    }
}
