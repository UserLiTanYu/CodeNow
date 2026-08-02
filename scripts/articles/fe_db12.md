# 数据库安全与 SQL 注入防护

数据库安全是系统安全的核心环节。SQL 注入是最古老也是最危险的攻击方式之一，至今仍频繁出现在 OWASP Top 10 中。本文讲解 SQL 注入的原理和攻击手法、防护措施、数据库权限管理、审计日志、数据脱敏和备份加密。

## SQL 注入原理

SQL 注入的本质是将用户输入的数据拼接到 SQL 语句中，使攻击者能够控制 SQL 的执行逻辑。

### 最简单的注入

```java
// 危险代码：字符串拼接
String sql = "SELECT * FROM users WHERE username = '" + username + "'";

// 正常输入
username = "zhangsan"
// SQL: SELECT * FROM users WHERE username = 'zhangsan'

// 恶意输入
username = "' OR '1'='1"
// SQL: SELECT * FROM users WHERE username = '' OR '1'='1'
// 返回所有用户
```

### 联合注入（UNION Injection）

当查询结果会显示在页面上时，攻击者可以用 `UNION` 将恶意查询结果拼接出来。

```sql
-- 步骤一：判断列数
' ORDER BY 1-- 
' ORDER BY 2-- 
' ORDER BY 3--   ← 报错，说明有 2 列

-- 步骤二：获取显示位
' UNION SELECT NULL,NULL-- 
' UNION SELECT 1,2-- 

-- 步骤三：获取数据库信息
' UNION SELECT database(),version()-- 

-- 步骤四：获取表名
' UNION SELECT table_name,NULL FROM information_schema.tables WHERE table_schema=database()-- 

-- 步骤五：获取列名
' UNION SELECT column_name,NULL FROM information_schema.columns WHERE table_name='users'-- 

-- 步骤六：获取数据
' UNION SELECT username,password FROM users-- 
```

### 盲注（Blind Injection）

当页面不显示查询结果时，通过布尔条件或时间延迟推断数据。

**布尔盲注**：

```sql
-- 判断数据库名第一个字符
' AND SUBSTRING(database(),1,1)='a'--    ← 页面正常则为 'a'
' AND SUBSTRING(database(),1,1)='b'--    ← 页面正常则为 'b'

-- 逐字符推断
' AND ASCII(SUBSTRING((SELECT password FROM users LIMIT 1),1,1))>96-- 
```

**时间盲注**：

```sql
-- 条件为真时延迟 5 秒
' AND IF(SUBSTRING(database(),1,1)='a',SLEEP(5),0)-- 

-- 通过响应时间判断
' AND (SELECT CASE WHEN (SUBSTRING(database(),1,1)='a') THEN SLEEP(5) ELSE 0 END)-- 
```

### 报错注入（Error-based Injection）

利用数据库的报错信息提取数据。

```sql
-- MySQL 报错注入
' AND (SELECT 1 FROM (SELECT COUNT(*),CONCAT((SELECT database()),FLOOR(RAND(0)*2))x FROM information_schema.tables GROUP BY x)a)-- 

-- EXTRACTVALUE 报错
' AND EXTRACTVALUE(1,CONCAT(0x7e,(SELECT database()),0x7e))-- 

-- UPDATEXML 报错
' AND UPDATEXML(1,CONCAT(0x7e,(SELECT version()),0x7e),1)-- 
```

### 堆叠注入（Stacked Queries）

在支持多语句执行的数据库中（如 MySQL 的 `mysqli_multi_query`），攻击者可以执行多条 SQL。

```sql
'; DROP TABLE users;-- 
'; INSERT INTO users (username, password) VALUES ('hacker', 'hacked');-- 
'; UPDATE users SET password='hacked' WHERE username='admin';-- 
```

## 参数化查询

参数化查询（Prepared Statement）是防止 SQL 注入的最有效手段。数据库将 SQL 结构和参数分开处理，参数永远被视为数据而非 SQL 的一部分。

### JDBC PreparedStatement

```java
// 安全：使用参数化查询
String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
PreparedStatement ps = connection.prepareStatement(sql);
ps.setString(1, username);
ps.setString(2, password);
ResultSet rs = ps.executeQuery();

// 危险：字符串拼接
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
```

### MyBatis 参数化

```xml
<!-- 安全：使用 #{}（参数化） -->
<select id="findUser" resultType="User">
    SELECT * FROM users WHERE username = #{username}
</select>

<!-- 危险：使用 ${}（字符串替换） -->
<select id="findUser" resultType="User">
    SELECT * FROM users WHERE username = '${username}'
</select>
```

`#{}` 会被替换为 `?`，由 PreparedStatement 设置参数值。`${}` 是直接字符串替换，存在注入风险。

**只能在以下场景使用 `${}`**：
- 动态表名或列名（需白名单校验）
- ORDER BY 子句（需枚举校验）

```xml
<!-- 动态排序（白名单校验） -->
<select id="findAll" resultType="Article">
    SELECT * FROM articles
    ORDER BY
    <choose>
        <when test="sortBy == 'views'">views</when>
        <when test="sortBy == 'created_at'">created_at</when>
        <otherwise>id</otherwise>
    </choose>
    ${order}
</select>
```

### JPA 参数化

```java
// 安全：使用 @Query 参数绑定
@Query("SELECT u FROM User u WHERE u.username = :username")
User findByUsername(@Param("username") String username);

// 安全：方法名推导（自动参数化）
User findByUsernameAndPassword(String username, String password);

// 危险：原生 SQL 中的字符串拼接
@Query(value = "SELECT * FROM users WHERE username = '" + ":username" + "'", nativeQuery = true)
```

## ORM 层防注入

### MyBatis 防注入最佳实践

```java
// Mapper 接口
@Select("SELECT * FROM users WHERE id = #{id}")
User findById(@Param("id") Long id);

// 动态 SQL 防注入
@Select("<script>" +
    "SELECT * FROM users" +
    "<where>" +
    "  <if test='name != null'>AND name = #{name}</if>" +
    "  <if test='email != null'>AND email = #{email}</if>" +
    "</where>" +
    "</script>")
List<User> findByCondition(UserQuery query);
```

### JPA 防注入最佳实践

```java
// 使用 Specification 动态查询
public class UserSpecs {
    public static Specification<User> nameLike(String name) {
        return (root, query, cb) -> 
            cb.like(root.get("name"), "%" + name + "%");
    }
    
    public static Specification<User> statusEquals(Integer status) {
        return (root, query, cb) -> 
            cb.equal(root.get("status"), status);
    }
}

// 使用
Specification<User> spec = UserSpecs.nameLike("张")
    .and(UserSpecs.statusEquals(1));
List<User> users = userRepository.findAll(spec);
```

## 数据库权限最小化原则

### 应用用户权限

```sql
-- 创建专用应用用户（不使用 root）
CREATE USER 'codenow_app'@'%' IDENTIFIED BY 'strong_password';

-- 只授予必要的权限
GRANT SELECT, INSERT, UPDATE, DELETE ON codenow.* TO 'codenow_app'@'%';

-- 不授予危险权限
-- 不授予 DROP, ALTER, CREATE, GRANT
-- 不授予 FILE（防止读写文件）
-- 不授予 PROCESS, SUPER（防止查看所有连接和杀死进程）

-- 刷新权限
FLUSH PRIVILEGES;
```

### 只读用户

```sql
-- 只读用户（用于报表查询）
CREATE USER 'codenow_readonly'@'%' IDENTIFIED BY 'strong_password';
GRANT SELECT ON codenow.* TO 'codenow_readonly'@'%';
```

### 备份用户

```sql
-- 备份用户（最小权限）
CREATE USER 'codenow_backup'@'localhost' IDENTIFIED BY 'strong_password';
GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER ON codenow.* TO 'codenow_backup'@'localhost';
```

### 权限审计

```sql
-- 查看所有用户权限
SELECT user, host, Select_priv, Insert_priv, Update_priv, Delete_priv,
       Create_priv, Drop_priv, Grant_priv
FROM mysql.user;

-- 查看特定用户权限
SHOW GRANTS FOR 'codenow_app'@'%';

-- MySQL 8.0 使用角色
CREATE ROLE 'app_read', 'app_write';
GRANT SELECT ON codenow.* TO 'app_read';
GRANT INSERT, UPDATE, DELETE ON codenow.* TO 'app_write';
GRANT 'app_read', 'app_write' TO 'codenow_app'@'%';
```

## 审计日志

### MySQL Enterprise Audit

MySQL Enterprise 版本内置审计插件。社区版可以使用第三方插件或通用查询日志。

### 通用查询日志

```sql
-- 开启通用查询日志（记录所有 SQL）
SET GLOBAL general_log = ON;
SET GLOBAL general_log_file = '/var/log/mysql/general.log';
```

生产环境不建议开启通用查询日志，会严重影响性能。

### 使用应用层拦截器记录

```java
// MyBatis 拦截器
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SqlAuditInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql();
        long start = System.currentTimeMillis();
        
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {  // 记录慢 SQL
                log.warn("Slow SQL [{}ms]: {}", cost, sql);
            }
            auditLog.info("SQL: {}", sql);
        }
    }
}
```

### binlog 审计

binlog 记录了所有数据变更操作，可用于安全审计和事后追溯。

```bash
# 解析 binlog 查看数据变更
mysqlbinlog --base64-output=DECODE-ROWS -v mysql-bin.000010 | grep -A 5 "DELETE FROM users"
```

## 数据脱敏

### 静态脱敏

在数据导入测试环境时进行脱敏：

```sql
-- 手机号脱敏
UPDATE users SET phone = CONCAT(LEFT(phone, 3), '****', RIGHT(phone, 4));

-- 邮箱脱敏
UPDATE users SET email = CONCAT(LEFT(email, 2), '***@', SUBSTRING_INDEX(email, '@', -1));

-- 身份证脱敏
UPDATE users SET id_card = CONCAT(LEFT(id_card, 6), '********', RIGHT(id_card, 4));

-- 姓名脱敏
UPDATE users SET real_name = CONCAT(LEFT(real_name, 1), '**');
```

### 动态脱敏

在查询返回时动态脱敏，不修改原始数据：

```java
// 自定义 TypeHandler 或 ResultHandler
public class PhoneDesensitizeHandler {
    
    public static String desensitize(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

// Jackson 序列化脱敏
@JsonSerialize(using = PhoneSerializer.class)
private String phone;

public class PhoneSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider sp) 
            throws IOException {
        gen.writeString(PhoneDesensitizeHandler.desensitize(value));
    }
}
```

### 视图脱敏

```sql
-- 创建脱敏视图
CREATE VIEW v_users_safe AS
SELECT
    id,
    username,
    CONCAT(LEFT(email, 2), '***@', SUBSTRING_INDEX(email, '@', -1)) AS email,
    CONCAT(LEFT(phone, 3), '****', RIGHT(phone, 4)) AS phone,
    status,
    created_at
FROM users;

-- 应用查询视图而非原表
GRANT SELECT ON codenow.v_users_safe TO 'codenow_app'@'%';
```

## 备份加密

### mysqldump + GPG 加密

```bash
# 加密备份
mysqldump --single-transaction --all-databases | \
    gpg --encrypt --recipient admin@example.com > backup.sql.gpg

# 解密恢复
gpg --decrypt backup.sql.gpg | mysql -u root -p
```

### XtraBackup 加密

```bash
# 备份时加密
xtrabackup --backup --target-dir=/backup/encrypted \
    --encrypt=AES256 \
    --encrypt-key="your-encryption-key-here"

# 解密
xtrabackup --decrypt=AES256 \
    --encrypt-key="your-encryption-key-here" \
    --target-dir=/backup/encrypted

# 准备和恢复
xtrabackup --prepare --target-dir=/backup/encrypted
```

### 传输层加密（SSL/TLS）

```sql
-- 强制用户使用 SSL 连接
ALTER USER 'codenow_app'@'%' REQUIRE SSL;

-- 配置 SSL
[mysqld]
ssl-ca = /etc/mysql/ssl/ca.pem
ssl-cert = /etc/mysql/ssl/server-cert.pem
ssl-key = /etc/mysql/ssl/server-key.pem
require_secure_transport = ON
```

```yaml
# 应用端 SSL 连接
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/codenow?useSSL=true&requireSSL=true&verifyServerCertificate=true&trustCertificateKeyStoreUrl=file:truststore.jks
```

## 安全检查清单

| 检查项 | 说明 |
|---|---|
| 禁止 root 远程登录 | `skip-networking` 或限制 root 只能 localhost |
| 修改默认端口 | 避免使用 3306 |
| 密码策略 | `validate_password` 插件强制密码复杂度 |
| 连接限制 | `max_connections` 限制最大连接数 |
| 禁用 LOCAL INFILE | `local_infile = OFF` 防止读取服务器文件 |
| 删除测试数据库 | `DROP DATABASE test;` |
| 禁用符号链接 | `symbolic-links = 0` |
| 定期更新 | 及时应用安全补丁 |
| 最小权限 | 应用用户不使用 root |
| 备份加密 | 备份文件加密存储 |

```sql
-- 安全加固脚本
-- 1. 删除匿名用户
DELETE FROM mysql.user WHERE User='';

-- 2. 删除测试数据库
DROP DATABASE IF EXISTS test;
DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';

-- 3. 设置密码策略
INSTALL COMPONENT 'file://component_validate_password';
SET GLOBAL validate_password.length = 12;
SET GLOBAL validate_password.mixed_case_count = 1;
SET GLOBAL validate_password.number_count = 1;
SET GLOBAL validate_password.special_char_count = 1;

-- 4. 限制连接数
SET GLOBAL max_connections = 500;
SET GLOBAL max_connect_errors = 100;

FLUSH PRIVILEGES;
```
