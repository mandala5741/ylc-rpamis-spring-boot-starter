---
description: 生成完整的 CRUD 代码（Entity、Mapper、Service、Controller）
argument-hint: [表名] [实体类名] [模块名]
---

# 任务：生成 MyBatis-Plus CRUD 代码

基于提供的参数生成完整的 CRUD 代码结构。

## 参数说明
- **$1**: 数据库表名（如：sys_user）
- **$2**: 实体类名（如：SysUser）
- **$3**: 模块名/包名（如：system）

## 生成内容

### 1. Entity 实体类
- 使用 `@TableName` 注解指定表名
- 使用 `@TableId` 注解标识主键（默认使用雪花算法）
- 使用 `@TableField` 注解配置字段映射
- 对敏感字段（如 username、phone、email）添加 `typeHandler = TypeHandler.class` 实现自动加解密
- 使用 Lombok 注解（`@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`）
- 添加常用字段：id、gmt_create、gmt_modified、delFlag

### 2. Mapper 接口
- 继承 `BaseMapper<实体类>`
- 添加 `@Mapper` 注解
- 根据需要添加自定义查询方法

### 3. Service 接口
- 继承 `IService<实体类>`
- 定义业务方法

### 4. ServiceImpl 实现类
- 继承 `ServiceImpl<Mapper, 实体类>`
- 实现 Service 接口
- 添加 `@Service` 注解
- 实现分页查询、批量操作等常用方法
- 对查询结果自动调用 `TypeHandler.decrypt()` 进行解密
- 对查询条件中的敏感字段使用 `TypeHandler.encryptField()` 进行加密

### 5. Controller 控制器
- 添加 `@RestController` 和 `@RequestMapping` 注解
- 使用 Knife4j 注解（`@Tag`、`@Operation`）生成 API 文档
- 实现标准 RESTful API：
  - `POST /` - 新增
  - `PUT /{id}` - 修改
  - `DELETE /{id}` - 删除
  - `GET /{id}` - 根据 ID 查询
  - `GET /page` - 分页查询
  - `GET /list` - 列表查询
- 统一返回 `R<T>` 结果封装

## 代码规范要求

1. **包结构**：
   ```
   com.cqcloud.platform.{模块名}.entity
   com.cqcloud.platform.{模块名}.dto
   com.cqcloud.platform.{模块名}.vo
   com.cqcloud.platform.{模块名}.mapper
   com.cqcloud.platform.{模块名}.service
   com.cqcloud.platform.{模块名}.service.impl
   com.cqcloud.platform.{模块名}.controller
   ```

2. **加解密处理**：
   - Entity 中敏感字段使用 `@TableField(typeHandler = TypeHandler.class)`
   - Service 查询方法返回前调用 `TypeHandler.decrypt(entity)`
   - Service 查询条件中使用 `TypeHandler.encryptField(dto, Getter, Setter)`

3. **命名规范**：
   - Entity：实体类名（如 SysUser）
   - Mapper：实体类名 + Mapper（如 SysUserMapper）
   - Service：I + 实体类名 + Service（如 ISysUserService）
   - ServiceImpl：实体类名 + ServiceImpl（如 SysUserServiceImpl）
   - Controller：实体类名 + Controller（如 SysUserController）

4. **注释规范**：
   - 类级别添加 Javadoc 注释
   - 方法添加功能说明注释
   - 复杂逻辑添加行内注释

## 示例用法

```bash
# 生成用户模块 CRUD 代码
/crud sys_user SysUser system

# 生成角色模块 CRUD 代码
/crud sys_role SysRole system

# 生成部门模块 CRUD 代码
/crud sys_dept SysDept system
```

## 注意事项

1. 生成代码前先检查目标目录是否已存在同名文件，避免覆盖
2. 根据实际数据库表结构调整字段定义
3. 敏感字段（username、phone、email、cardNo 等）务必配置 TypeHandler
4. 生成后需要手动调整业务逻辑和验证规则
5. 确保数据库表已创建，字段类型与 Java 类型匹配

---

现在请根据以下参数生成完整的 CRUD 代码：

- **表名**：$1
- **实体类名**：$2
- **模块名**：$3

请按照上述规范生成所有必需的文件，并确保代码符合项目的编码规范和加解密要求。