package org.plugin.shoptools.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * 物品别名管理器
 * 负责加载 message.yml 中的中文别名与英文物品ID映射
 *
 * @author ShopTools
 */
public class ItemAliasManager {

    private final JavaPlugin plugin;
    private final File aliasFile;
    private FileConfiguration aliasConfig;
    private final Map<String, String> aliasToItem = new LinkedHashMap<>();
    private final Map<String, String> aliasOriginal = new LinkedHashMap<>();
    private final Map<String, List<String>> itemToAliases = new LinkedHashMap<>();

    public ItemAliasManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.aliasFile = new File(plugin.getDataFolder(), "message.yml");
        loadAliases();
    }

    public void loadAliases() {
        if (!aliasFile.exists()) {
            plugin.saveResource("message.yml", false);
        }

        aliasConfig = YamlConfiguration.loadConfiguration(aliasFile);
        aliasToItem.clear();
        aliasOriginal.clear();
        itemToAliases.clear();

        ConfigurationSection section = aliasConfig.getConfigurationSection("item-aliases");
        if (section == null) {
            return;
        }

        for (String alias : section.getKeys(false)) {
            String mappedItem = section.getString(alias);
            if (mappedItem == null || mappedItem.trim().isEmpty()) {
                continue;
            }

            String normalizedAlias = alias.trim().toLowerCase();
            String normalizedItem = mappedItem.trim();
            aliasToItem.put(normalizedAlias, normalizedItem);
            aliasOriginal.put(normalizedAlias, alias.trim());

            String normalizedItemKey = normalizedItem.toLowerCase();
            itemToAliases.computeIfAbsent(normalizedItemKey, k -> new ArrayList<>())
                    .add(alias.trim());
        }
    }

    public void reload() {
        loadAliases();
        plugin.getLogger().info("物品别名映射已重新加载。文件路径: " + aliasFile.getAbsolutePath());
    }

    /**
     * 将输入的中文别名或英文ID转换为实际物品ID。
     * 如果输入是别名，则返回映射的英文ID；否则返回原始输入。
     */
    public String resolveItemId(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String normalized = input.trim().toLowerCase();
        if (aliasToItem.containsKey(normalized)) {
            return aliasToItem.get(normalized);
        }
        return input.trim();
    }

    /**
     * 如果输入是别名，则返回别名本身；否则返回映射后的id对应的主中文名或原始输入。
     */
    public String getDisplayLabel(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String normalized = input.trim().toLowerCase();
        if (aliasOriginal.containsKey(normalized)) {
            return aliasOriginal.get(normalized);
        }

        String resolved = resolveItemId(input);
        String alias = getPrimaryAlias(resolved);
        return alias != null ? alias : input.trim();
    }

    /**
     * 获取物品ID对应的主中文别名（如果配置了多个别名，则返回第一个）。
     */
    public String getPrimaryAlias(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            return null;
        }
        List<String> aliases = itemToAliases.get(itemId.trim().toLowerCase());
        if (aliases == null || aliases.isEmpty()) {
            return null;
        }
        return aliases.get(0);
    }

    public Set<String> getAllAliases() {
        return new LinkedHashSet<>(aliasOriginal.values());
    }

    public boolean hasAliasForItem(String itemId) {
        if (itemId == null) {
            return false;
        }
        return itemToAliases.containsKey(itemId.trim().toLowerCase());
    }
}
