# 多表关联查询与嵌套结果映射

单表查询只能满足最基础的业务场景。实际项目中，文章需要关联分类和标签、订单需要关联商品和用户、部门需要关联员工——这些都离不开多表关联查询。MyBatis 提供了 `resultMap` 中的 `association` 和 `collection` 元素，能够将 SQL JOIN 的扁平结果优雅地映射到嵌套的 Java 对象结构中。

## 多表关联查询的需求场景

典型的多表关联场景：

- **一对一**：文章 → 分类（每篇文章属于一个分类）
- **一对多**：文章 → 标签（一篇文章有多个标签）
- **多对多**：用户 → 角色（通过中间表关联）

在 MyBatis 中，多表关联查询的核心挑战不在于 SQL 本身，而在于**如何将 JOIN 后的扁平结果集映射到具有层级结构的 Java 对象**。这就是 `resultMap` 的用武之地。

## resultMap 详解

`resultMap` 是 MyBatis 最强大的映射机制，它定义了数据库列与 Java 对象属性之间的对应关系。

```xml
<resultMap id="articleMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <result property="content" column="content"/>
    <result property="createdAt" column="created_at"/>
</resultMap>
```

`resultMap` 包含以下子元素：

| 元素 | 用途 | 说明 |
|------|------|------|
| `id` | 主键映射 | 与 `result` 的区别在于 MyBatis 会用 `id` 元素判断结果是否为同一条记录 |
| `result` | 普通字段映射 | 列名 → 属性名的对应关系 |
| `association` | 一对一关联 | 将结果映射到一个嵌套对象 |
| `collection` | 一对多关联 | 将结果映射到一个嵌套集合 |
| `discriminator` | 鉴别器 | 根据列值走不同的映射逻辑 |

`id` 和 `result` 最基本的属性：

- `column` — 数据库列名（或列别名）
- `property` — Java 对象属性名
- `javaType` — Java 类型（通常可自动推断）
- `jdbcType` — JDBC 类型（仅在列可能为 null 时需要显式指定）
- `typeHandler` — 自定义类型处理器

## association 一对一关联

`association` 用于将查询结果中的部分列映射到一个嵌套对象。例如文章关联分类：

```java
public class Article {
    private Long id;
    private String title;
    private Category category; // 一对一
}
```

### 方式一：嵌套结果映射（Nested ResultMap）

通过一条 JOIN SQL 完成所有查询，再用 `association` 将不同列映射到嵌套对象：

```xml
<select id="selectArticleWithCategory" resultMap="articleWithCategoryMap">
    SELECT a.id, a.title, a.content, a.created_at,
           c.id AS cat_id, c.name AS cat_name
    FROM article a
    LEFT JOIN category c ON a.category_id = c.id
    WHERE a.id = #{id}
</select>

<resultMap id="articleWithCategoryMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <result property="content" column="content"/>
    <result property="createdAt" column="created_at"/>
    <association property="category" javaType="com.example.entity.Category">
        <id property="id" column="cat_id"/>
        <result property="name" column="cat_name"/>
    </association>
</resultMap>
```

关键点：嵌套对象的列需要使用**列别名**（如 `cat_id`、`cat_name`）避免与主表列名冲突。

也可以用 `association` 的 `resultMap` 属性引用一个独立的 resultMap：

```xml
<resultMap id="categoryMap" type="com.example.entity.Category">
    <id property="id" column="cat_id"/>
    <result property="name" column="cat_name"/>
</resultMap>

<resultMap id="articleWithCategoryMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <association property="category" resultMap="categoryMap"/>
</resultMap>
```

### 方式二：嵌套查询（Nested Select）

`association` 的 `select` 属性可以指定另一条 SQL，由 MyBatis 在需要时调用：

```xml
<resultMap id="articleWithCategoryLazyMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <association property="category"
                 column="category_id"
                 select="com.example.mapper.CategoryMapper.selectById"/>
</resultMap>
```

这里 `column="category_id"` 表示将当前记录的 `category_id` 列值作为参数传递给 `selectById`。

## collection 一对多关联

`collection` 用于将多行结果聚合到一个列表中。例如文章关联多个标签：

```java
public class Article {
    private Long id;
    private String title;
    private List<Tag> tags; // 一对多
}
```

### 方式一：嵌套结果映射

```xml
<select id="selectArticleWithTags" resultMap="articleWithTagsMap">
    SELECT a.id, a.title,
           t.id AS tag_id, t.name AS tag_name
    FROM article a
    LEFT JOIN article_tag at ON a.id = at.article_id
    LEFT JOIN tag t ON at.tag_id = t.id
    WHERE a.id = #{id}
</select>

<resultMap id="articleWithTagsMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <collection property="tags" ofType="com.example.entity.Tag">
        <id property="id" column="tag_id"/>
        <result property="name" column="tag_name"/>
    </collection>
</resultMap>
```

注意：`collection` 使用 `ofType` 指定集合元素的类型，而非 `javaType`。

当查询多篇文章时，JOIN 会产生多行结果（一篇文章对应多条标签记录），MyBatis 会根据 `<id>` 元素判断哪些行属于同一篇文章，自动将标签聚合到 `tags` 列表中。

### 方式二：嵌套查询

```xml
<resultMap id="articleWithTagsLazyMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <collection property="tags"
                column="id"
                select="com.example.mapper.TagMapper.selectTagsByArticleId"/>
</resultMap>
```

## 嵌套查询的 N+1 问题及解决方案

嵌套查询（select 方式）有一个严重的性能隐患——**N+1 问题**：

1. 先执行 1 条 SQL 查询出 N 篇文章
2. 对每篇文章再分别执行 1 条 SQL 查询其标签
3. 总共执行 1 + N 条 SQL

当 N 很大时，数据库连接被大量消耗，性能急剧下降。

**解决方案一：改用嵌套结果映射**

用一条 JOIN SQL 获取所有数据，避免多次查询：

```xml
<select id="selectAllArticlesWithTags" resultMap="articleWithTagsMap">
    SELECT a.id, a.title,
           t.id AS tag_id, t.name AS tag_name
    FROM article a
    LEFT JOIN article_tag at ON a.id = at.article_id
    LEFT JOIN tag t ON at.tag_id = t.id
    ORDER BY a.id
</select>
```

**解决方案二：批量查询 + 手动组装**

先查出所有文章 ID，再用 `IN` 子句批量查询标签：

```xml
<select id="selectTagsByArticleIds" resultType="com.example.entity.Tag">
    SELECT at.article_id, t.id, t.name
    FROM article_tag at
    JOIN tag t ON at.tag_id = t.id
    WHERE at.article_id IN
    <foreach collection="articleIds" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

然后在 Service 层手动组装：

```java
List<Article> articles = articleMapper.selectAll();
List<Long> articleIds = articles.stream().map(Article::getId).toList();
Map<Long, List<Tag>> tagMap = tagMapper.selectTagsByArticleIds(articleIds)
        .stream().collect(Collectors.groupingBy(Tag::getArticleId));
articles.forEach(a -> a.setTags(tagMap.getOrDefault(a.getId(), Collections.emptyList())));
```

## discriminator 鉴别器

`discriminator` 类似于 Java 的 `switch` 语句，根据某列的值选择不同的映射策略。例如根据文章类型映射不同的字段：

```xml
<resultMap id="articleMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <discriminator javaType="int" column="article_type">
        <case value="1" resultType="com.example.entity.Article">
            <result property="content" column="text_content"/>
        </case>
        <case value="2" resultType="com.example.entity.VideoArticle">
            <result property="videoUrl" column="video_url"/>
            <result property="duration" column="duration"/>
        </case>
    </discriminator>
</resultMap>
```

`discriminator` 在实际项目中使用频率不高，但在处理**多态数据**（如同一张表存储不同类型记录）时非常有用。

## resultMap 继承：extends 属性

当多个 `resultMap` 存在重复映射时，可以用 `extends` 属性实现继承：

```xml
<resultMap id="baseArticleMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <result property="content" column="content"/>
    <result property="createdAt" column="created_at"/>
</resultMap>

<resultMap id="articleWithCategoryMap" type="com.example.entity.Article" extends="baseArticleMap">
    <association property="category" javaType="com.example.entity.Category">
        <id property="id" column="cat_id"/>
        <result property="name" column="cat_name"/>
    </association>
</resultMap>

<resultMap id="articleWithTagsMap" type="com.example.entity.Article" extends="baseArticleMap">
    <collection property="tags" ofType="com.example.entity.Tag">
        <id property="id" column="tag_id"/>
        <result property="name" column="tag_name"/>
    </collection>
</resultMap>
```

子 `resultMap` 继承父 `resultMap` 的所有映射规则，并可以添加自己的扩展。这在字段较多时能显著减少重复配置。

## MyBatis-Plus 多表关联

MyBatis-Plus 专注于单表 CRUD，不直接支持多表关联查询，但可以通过以下方式实现。

### 自定义 XML Mapper

在 MyBatis-Plus 项目中，Mapper 接口可以同时继承 `BaseMapper` 和自定义方法：

```java
public interface ArticleMapper extends BaseMapper<Article> {

    // 自定义多表关联查询
    List<ArticleVO> selectArticlesWithCategoryAndTags(@Param("categoryId") Long categoryId);
}
```

XML 中编写关联 SQL：

```xml
<select id="selectArticlesWithCategoryAndTags" resultType="com.example.vo.ArticleVO">
    SELECT a.id, a.title, a.created_at,
           c.name AS categoryName,
           GROUP_CONCAT(t.name) AS tagNames
    FROM article a
    LEFT JOIN category c ON a.category_id = c.id
    LEFT JOIN article_tag at ON a.id = at.article_id
    LEFT JOIN tag t ON at.tag_id = t.id
    <where>
        <if test="categoryId != null">
            AND a.category_id = #{categoryId}
        </if>
    </where>
    GROUP BY a.id
</select>
```

### Wrapper 嵌套

对于简单的子查询场景，可以用 `AbstractWrapper` 的嵌套：

```java
// 查询有标签的文章
QueryWrapper<Article> wrapper = new QueryWrapper<>();
wrapper.inSql("id",
    "SELECT article_id FROM article_tag WHERE tag_id = 1");
List<Article> articles = articleMapper.selectList(wrapper);
```

## 实战：文章-分类-标签三表关联查询

以"码上记"博客系统为例，实现一个完整的三表关联查询：查询文章列表，同时携带分类信息和标签列表。

**数据库表结构：**

```sql
CREATE TABLE article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    category_id BIGINT,
    created_at DATETIME
);

CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id)
);
```

**实体类：**

```java
@Data
public class Article {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Category category;
    private List<Tag> tags;
}

@Data
public class Category {
    private Long id;
    private String name;
}

@Data
public class Tag {
    private Long id;
    private String name;
}
```

**Mapper XML：**

```xml
<resultMap id="baseArticleMap" type="com.example.entity.Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <result property="content" column="content"/>
    <result property="createdAt" column="created_at"/>
</resultMap>

<resultMap id="articleDetailMap" type="com.example.entity.Article" extends="baseArticleMap">
    <association property="category" javaType="com.example.entity.Category">
        <id property="id" column="cat_id"/>
        <result property="name" column="cat_name"/>
    </association>
    <collection property="tags" ofType="com.example.entity.Tag">
        <id property="id" column="tag_id"/>
        <result property="name" column="tag_name"/>
    </collection>
</resultMap>

<select id="selectArticleDetailList" resultMap="articleDetailMap">
    SELECT a.id, a.title, a.content, a.created_at,
           c.id AS cat_id, c.name AS cat_name,
           t.id AS tag_id, t.name AS tag_name
    FROM article a
    LEFT JOIN category c ON a.category_id = c.id
    LEFT JOIN article_tag at ON a.id = at.article_id
    LEFT JOIN tag t ON at.tag_id = t.id
    ORDER BY a.created_at DESC
</select>
```

由于 JOIN 会产生多行结果（一篇文章对应多个标签），MyBatis 根据 `<id property="id" column="id"/>` 判断同属一篇文章的记录，将标签自动聚合到 `tags` 列表中。

## 关联查询性能优化

### 延迟加载 vs 立即加载

| 加载策略 | 时机 | 配置方式 | 适用场景 |
|---------|------|---------|---------|
| 立即加载（Eager） | 执行主查询时一并加载 | 默认行为 | 关联数据一定会被使用 |
| 延迟加载（Lazy） | 访问关联属性时才触发查询 | `fetchType="lazy"` | 关联数据可能不被使用 |

全局开启延迟加载：

```yaml
mybatis:
  configuration:
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
```

单个 association/collection 指定延迟：

```xml
<association property="category" column="category_id"
             select="com.example.mapper.CategoryMapper.selectById"
             fetchType="lazy"/>
```

### 其他优化建议

**合理使用 `fetchType`**：如果列表页不需要标签详情，可以用 `fetchType="lazy"` 延迟加载，或在 SQL 中不 JOIN 标签表。

**分页时避免 collection**：JOIN + 分页会导致实际返回的行数不等于分页条数（一篇文章可能对应多行标签）。解决方案是先分页查文章 ID，再批量查询关联数据：

```java
// 第一步：分页查询文章 ID
Page<Article> page = articleMapper.selectPage(new Page<>(1, 10), queryWrapper);
List<Long> articleIds = page.getRecords().stream().map(Article::getId).toList();

// 第二步：批量查询分类和标签
if (!articleIds.isEmpty()) {
    List<ArticleDetail> details = articleMapper.selectDetailsByIds(articleIds);
    // 组装到分页结果中
}
```

**使用 `@ManyToOne` / `@OneToMany` 替代方案**：如果项目使用 JPA + Hibernate，可以用注解声明关联关系，框架自动处理 SQL 生成和结果映射。但在 MyBatis 生态中，XML 配置仍然是最灵活的方式。

**避免过度关联**：超过 3 张表的 JOIN 通常意味着需要重新审视表设计。可以考虑冗余关键字段、使用宽表或物化视图来减少关联层数。
