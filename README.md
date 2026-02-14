## 独立的加解密 Spring Boot Starter

# TypeHandler 使用说明文档

## 📌 功能概述

`TypeHandler` 是一个基于 MyBatis 的**自定义类型处理器**，用于实现字段级别的**自动加密存储**和**自动解密查询**。它采用 **SM4 国密算法**对敏感数据进行加密处理，同时提供了便捷的对象加解密工具方法。

---

## 一、MyBatis 自动加解密配置

### 1.1 实体类配置（注解方式）

在需要加解密的实体字段上添加 `@TableField` 注解，指定 `typeHandler`：

```java
@TableName(value = "user")
public class User {
    
    @TableField(typeHandler = TypeHandler.class)
    private String username;
    
    @TableField(typeHandler = TypeHandler.class)
    private String phone;
    
    @TableField(typeHandler = TypeHandler.class)
    private String email;
    
    // 普通字段，不加解密
    private Integer age;
}
```

**说明：**
- **插入/更新时**：自动将明文加密为 Base64 密文存入数据库
- **查询时**：自动将数据库密文解密为明文返回

---

### 1.2 XML 映射文件配置

在 `<result>` 或 `<id>` 标签中指定 `typeHandler`：

```xml
<resultMap id="BaseResultMap" type="com.cqcloud.platform.entity.User">
    <id column="id" property="id" />
    <result column="user_name" property="username" 
            typeHandler="com.cqcloud.platform.utils.TypeHandler"/>
    <result column="phone" property="phone" 
            typeHandler="com.cqcloud.platform.utils.TypeHandler"/>
    <result column="email" property="email" 
            typeHandler="com.cqcloud.platform.utils.TypeHandler"/>
</resultMap>

<!-- 插入时也需要指定 typeHandler -->
<insert id="insert">
    INSERT INTO user (user_name, phone, email)
    VALUES (
        #{username, typeHandler=com.cqcloud.platform.utils.TypeHandler},
        #{phone, typeHandler=com.cqcloud.platform.utils.TypeHandler},
        #{email, typeHandler=com.cqcloud.platform.utils.TypeHandler}
    )
</insert>
```

---

### 1.3 查询示例

```java
@Mapper
public interface UserMapper {
    
    // 查询时会自动解密
    User selectById(@Param("id") Long id);
    
    // 插入时会自动加密
    int insert(User user);
    
    // 更新时会自动加密
    int updateById(User user);
}
```

**无需手动调用任何加解密方法**，MyBatis 会自动处理。

---

## 二、手动加解密工具方法

除了 MyBatis 自动处理外，`TypeHandler` 还提供了手动加解密的静态工具方法。

### 2.1 单个字符串加解密

```java
// 加密
String encrypted = TypeHandler.encryptText("13800138000");
System.out.println(encrypted); // 5nJgY3X... (Base64密文)

// 解密（MyBatis查询时自动执行，一般无需手动调用）
// String decrypted = Sm4Utils.decryptFromBase64(encrypted, SM4_KEY);
```

---

### 2.2 对象字段批量解密

```java
// 从数据库查询出的对象，字段已是明文，通常不需要手动解密
User user = userMapper.selectById(1L);

// 但如果你直接从 ResultSet 或缓存中拿到的是密文对象，可以：
TypeHandler.decrypt(user); // 使用默认字段解密

// 或指定字段解密
TypeHandler.decrypt(user, "username", "phone");

// 支持带 @ 前缀的特殊格式（如 "email@密文"）
// 解密后为 "email@明文"
```

**默认解密字段列表：**
```java
username, phone, nickname, realName, name, cardNo, email, 
wxOpenid, miniOpenid, ykbTalkOpenid, dingTalkOpenid
```

---

### 2.3 对象字段批量加密

```java
// 新增用户，手动加密（通常由 MyBatis 自动处理，特殊场景使用）
User user = new User();
user.setUsername("张三");
user.setPhone("13800138000");

TypeHandler.encrypt(user); // 使用默认字段加密

// 或指定字段加密
TypeHandler.encrypt(user, "username", "phone");

userMapper.insert(user); // 此时字段已是密文
```

**注意：** 如果实体类配置了 `@TableField(typeHandler = TypeHandler.class)`，**无需手动调用** `encrypt()`，MyBatis 会在执行 SQL 时自动加密。

---

### 2.4 函数式编程方式加解密

```java
// 灵活指定 getter/setter 进行加密
TypeHandler.encryptField(user, 
    User::getUsername,   // 获取原始值
    User::setUsername    // 设置加密值
);
```

---

## 三、@前缀特殊处理机制

处理器支持**带前缀的加密存储**格式，适用于需要保留标识符的场景。

### 3.1 存储格式

数据库存储格式：`前缀@密文`

例如邮箱字段：
```
加密前：weimeilayer@gmail.com
加密后：weimeilayer@5nJgY3XzF8q...
```

### 3.2 解密行为

- **自动识别 `@` 符号**：保留 `@` 之前的内容作为前缀
- **仅解密 `@` 之后的部分**
- 最终结果：`前缀@明文`

### 3.3 使用场景

```java
// 插入时自动加密
user.setEmail("weimeilayer@gmail.com");
userMapper.insert(user);
// 数据库存储: "weimeilayer@gmail.com" → "weimeilayer@5nJgY3X..."

// 查询时自动解密
User user = userMapper.selectById(1L);
System.out.println(user.getEmail()); 
// 输出: weimeilayer@gmail.com
```

**注意：** 此机制仅在 `decryptField()` 方法中生效。手动调用 `encryptField()` 时，**不会添加 `@` 前缀**，直接存储纯密文。

---

## 四、常见问题

### ❓ 1. 插入时没有自动加密
- 检查实体字段是否添加 `@TableField(typeHandler = TypeHandler.class)`
- 检查 MyBatis 配置是否正确扫描了 TypeHandler

### ❓ 2. 查询时返回的字段仍是密文
- 检查 resultMap 是否配置了 `typeHandler`
- 确认数据库存储的确实是密文

### ❓ 3. 加密/解密失败
- 确认 `CommonConstants.SM4_PRIVATE_KEY` 已正确配置
- 密钥是原始字符串，不是 MD5 值（Sm4Utils 内部会自动 MD5）
- 检查待加密/解密字符串是否为 null 或空字符串

---

## 五、安全建议

1. **密钥管理**：`SM4_PRIVATE_KEY` 不应硬编码在代码中，建议通过配置中心或环境变量注入
2. **字段选择**：仅对必要的敏感字段进行加解密，避免性能损耗
3. **索引问题**：加密字段无法直接使用数据库索引，如需查询请考虑其他方案（如密文索引、Hash 字段等）

---

## 六、完整示例代码

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public void addUser(User user) {
        // ✅ MyBatis 自动加密（字段上有 @TableField(typeHandler)）
        userMapper.insert(user);
    }
    
    public User getUser(Long id) {
        // ✅ MyBatis 自动解密
        return userMapper.selectById(id);
    }
    
    public void batchDecrypt(List<User> users) {
        // ⚠️ 特殊场景手动解密（如从 Redis 缓存中取出）
        users.forEach(TypeHandler::decrypt);
    }
}
```

---

**总结：** 只需在实体字段或 XML 中配置 `typeHandler`，即可实现**全自动的字段级加解密**，无需任何业务代码侵入。手动加解密方法主要用于特殊场景或批量处理。