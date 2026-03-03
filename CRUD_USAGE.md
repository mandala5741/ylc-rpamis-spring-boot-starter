# 商品订单 CRUD 使用示例

## 1. 数据库表结构

```sql
CREATE TABLE pay_goods_order (
    goods_order_id int8 NOT NULL,
    goods_id varchar(64) NULL,
    goods_name varchar(255) NULL,
    amount varchar(32) NULL,
    user_id varchar(255) NULL,
    status varchar(2) NULL,
    pay_order_id varchar(64) NULL,
    del_flag char(1) NULL DEFAULT '0',
    gmt_create timestamp NULL,
    gmt_modified timestamp NULL,
    tenant_id int8 NULL,
    CONSTRAINT pay_goods_order_pkey PRIMARY KEY (goods_order_id)
);

CREATE UNIQUE INDEX pay_goods_order_pay_order_id_idx ON pay_goods_order (pay_order_id);
```

## 2. 实体类配置

`PayGoodsOrder` 实体类中 `userId` 字段配置了自动加解密：

```java
@TableField(value = "user_id", typeHandler = TypeHandler.class)
private String userId;
```

这意味着：
- **插入/更新时**：`userId` 会自动使用 SM4 算法加密后存入数据库
- **查询时**：从数据库读取的 `userId` 会自动解密

## 3. API 接口说明

### 3.1 新增订单

**请求：**
```http
POST /pay/goods-order
Content-Type: application/json

{
  "goodsId": "GOODS001",
  "goodsName": "测试商品",
  "amount": "99.99",
  "userId": "user123",
  "status": "0",
  "payOrderId": "PAY20260226001"
}
```

**响应：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": true
}
```

### 3.2 修改订单

**请求：**
```http
PUT /pay/goods-order/1234567890
Content-Type: application/json

{
  "status": "1",
  "amount": "199.99"
}
```

### 3.3 删除订单

**请求：**
```http
DELETE /pay/goods-order/1234567890
```

### 3.4 根据ID查询

**请求：**
```http
GET /pay/goods-order/1234567890
```

**响应：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "goodsOrderId": 1234567890,
    "goodsId": "GOODS001",
    "goodsName": "测试商品",
    "amount": "99.99",
    "userId": "user123",
    "status": "0",
    "payOrderId": "PAY20260226001",
    "delFlag": "0",
    "gmtCreate": "2026-02-26T10:00:00",
    "gmtModified": "2026-02-26T10:00:00",
    "tenantId": 1
  }
}
```

### 3.5 分页查询

**请求：**
```http
GET /pay/goods-order/page?current=1&size=10&status=0&goodsName=测试
```

**响应：**
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [...],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 3.6 根据支付订单ID查询

**请求：**
```http
GET /pay/goods-order/pay-order/PAY20260226001
```

### 3.7 根据用户ID查询订单列表

**请求：**
```http
GET /pay/goods-order/user/user123
```

## 4. 加解密处理说明

### 4.1 自动加解密（推荐）

在实体类字段上配置 `typeHandler`，MyBatis-Plus 会自动处理：

```java
@TableField(value = "user_id", typeHandler = TypeHandler.class)
private String userId;
```

### 4.2 手动加解密

如果需要在查询条件中使用加密字段，需要手动加密：

```java
// Service 层示例
public List<PayGoodsOrder> listByUserId(String userId) {
    // 1. 加密查询条件
    String encryptedUserId = TypeHandler.encryptText(userId);

    // 2. 执行查询
    List<PayGoodsOrder> list = baseMapper.selectByUserId(encryptedUserId);

    // 3. 批量解密结果
    list.forEach(order -> TypeHandler.decrypt(order, "userId"));

    return list;
}
```

### 4.3 分页查询中的加解密

```java
@Override
public Page<PayGoodsOrder> pageQuery(Page<PayGoodsOrder> page, PayGoodsOrder query) {
    LambdaQueryWrapper<PayGoodsOrder> wrapper = new LambdaQueryWrapper<>();

    // 用户ID需要加密后查询
    if (StringUtils.isNotBlank(query.getUserId())) {
        String encryptedUserId = TypeHandler.encryptText(query.getUserId());
        wrapper.eq(PayGoodsOrder::getUserId, encryptedUserId);
    }

    // 执行分页查询
    Page<PayGoodsOrder> result = baseMapper.selectPage(page, wrapper);

    // 批量解密结果
    result.getRecords().forEach(order -> TypeHandler.decrypt(order, "userId"));

    return result;
}
```

## 5. 配置说明

### 5.1 application.yml 配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password
    driver-class-name: org.postgresql.Driver

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.cqcloud.platform.pay.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# Knife4j 文档配置
knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 5.2 访问 API 文档

启动应用后访问：`http://localhost:8080/doc.html`

## 6. 注意事项

1. **加密字段查询**：使用加密字段作为查询条件时，必须先加密再查询
2. **索引字段**：加密字段不适合建立索引，因为加密后的值无法进行范围查询
3. **性能考虑**：加解密会增加一定的性能开销，建议只对敏感字段使用
4. **密钥管理**：生产环境中应将 `SM4_PRIVATE_KEY` 配置在安全的地方，不要硬编码

## 7. 扩展其他加密字段

如需对其他字段加密，只需在实体类中添加 `typeHandler` 配置：

```java
@TableField(value = "phone", typeHandler = TypeHandler.class)
private String phone;

@TableField(value = "email", typeHandler = TypeHandler.class)
private String email;
```

然后在 Service 层查询后手动解密：

```java
TypeHandler.decrypt(order, "userId", "phone", "email");
```
