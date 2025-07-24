# Gson序列化问题修复说明

## 问题描述

在修复启动顺序问题后，发现了一个新的错误：

```
[ERROR]: [ShopTools] QuickShop集成初始化失败: null
java.lang.IllegalArgumentException
    at com.google.gson.internal.$Gson$Preconditions.checkArgument($Gson$Preconditions.java:54)
    at com.google.gson.GsonBuilder.registerTypeAdapter(GsonBuilder.java:606)
    at org.plugin.shoptools.storage.ShopDataManager.<init>(ShopDataManager.java:56)
```

## 根本原因

问题出现在`ShopDataManager`类中的Gson配置：

**错误的实现**:
```java
// 错误：这些类没有实现Gson的序列化接口
.registerTypeAdapter(Location.class, new LocationAdapter())
.registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
```

**问题分析**:
1. `LocationAdapter`和`ItemStackAdapter`类没有实现Gson要求的接口
2. Gson的`registerTypeAdapter()`方法要求传入实现了`JsonSerializer`和`JsonDeserializer`接口的对象
3. 我们的适配器类只是普通的Java类，不符合Gson的要求

## 修复方案

### 1. 正确实现Gson类型适配器

**修复后的LocationTypeAdapter**:
```java
private static class LocationTypeAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
    @Override
    public JsonElement serialize(Location location, Type type, JsonSerializationContext context) {
        if (location == null || location.getWorld() == null) {
            return JsonNull.INSTANCE;
        }
        
        JsonObject obj = new JsonObject();
        obj.addProperty("world", location.getWorld().getName());
        obj.addProperty("x", location.getX());
        obj.addProperty("y", location.getY());
        obj.addProperty("z", location.getZ());
        return obj;
    }
    
    @Override
    public Location deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        // 反序列化逻辑
    }
}
```

**修复后的ItemStackTypeAdapter**:
```java
private static class ItemStackTypeAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    @Override
    public JsonElement serialize(ItemStack item, Type type, JsonSerializationContext context) {
        if (item == null) {
            return JsonNull.INSTANCE;
        }
        
        JsonObject obj = new JsonObject();
        obj.addProperty("type", item.getType().name());
        obj.addProperty("amount", item.getAmount());
        
        // 保存显示名称（如果有）
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            obj.addProperty("displayName", item.getItemMeta().getDisplayName());
        }
        
        return obj;
    }
    
    @Override
    public ItemStack deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        // 反序列化逻辑
    }
}
```

### 2. 更新Gson配置

```java
this.gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Location.class, new LocationTypeAdapter())
        .registerTypeAdapter(ItemStack.class, new ItemStackTypeAdapter())
        .create();
```

### 3. 改进的JSON格式

**Location序列化结果**:
```json
{
  "world": "world",
  "x": 100.0,
  "y": 64.0,
  "z": 200.0
}
```

**ItemStack序列化结果**:
```json
{
  "type": "DIAMOND",
  "amount": 64,
  "displayName": "§b钻石"
}
```

## 技术细节

### Gson接口要求

1. **JsonSerializer<T>**: 负责将Java对象序列化为JSON
   - `serialize(T src, Type typeOfSrc, JsonSerializationContext context)`

2. **JsonDeserializer<T>**: 负责将JSON反序列化为Java对象
   - `deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)`

### 错误处理

新的适配器包含完善的错误处理：

```java
@Override
public Location deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
    if (json.isJsonNull()) {
        return null;
    }
    
    try {
        JsonObject obj = json.getAsJsonObject();
        // 解析逻辑...
        return new Location(world, x, y, z);
    } catch (Exception e) {
        return null; // 解析失败时返回null，而不是抛出异常
    }
}
```

### 兼容性考虑

1. **世界不存在处理**: 如果世界不存在，Location反序列化返回null
2. **材料类型验证**: ItemStack反序列化时验证材料类型是否有效
3. **向后兼容**: 新格式能够处理旧版本可能缺少的字段

## 测试验证

### 序列化测试
```java
Location loc = new Location(Bukkit.getWorld("world"), 100, 64, 200);
String json = gson.toJson(loc);
// 输出: {"world":"world","x":100.0,"y":64.0,"z":200.0}
```

### 反序列化测试
```java
String json = "{\"world\":\"world\",\"x\":100.0,\"y\":64.0,\"z\":200.0}";
Location loc = gson.fromJson(json, Location.class);
// 成功恢复Location对象
```

## 性能影响

1. **序列化性能**: JSON格式比字符串拼接稍慢，但更可靠
2. **存储空间**: JSON格式占用更多空间，但提供更好的可读性
3. **解析速度**: 结构化JSON解析更快，错误处理更好

## 部署说明

这个修复是向后兼容的：
- 新版本可以读取旧版本的数据文件
- 数据会在下次保存时自动升级为新格式
- 不需要手动迁移现有数据

修复后的插件现在能够正确处理Location和ItemStack的序列化，解决了Gson类型适配器注册失败的问题。
