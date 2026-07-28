package com.codenow.common;

/**
 * 文章状态常量类，定义文章的发布状态枚举值。
 * <p>
 * 用于标识文章当前所处的生命周期阶段，如草稿、已发布等。
 * </p>
 */
public final class ArticleStatus {

    /** 草稿状态，文章尚未发布 */
    public static final int DRAFT = 0;

    /** 已发布状态，文章已对外公开 */
    public static final int PUBLISHED = 1;

    /**
     * 私有构造方法，防止实例化常量类。
     */
    private ArticleStatus() {
    }
}
