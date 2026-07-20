package com.codenow.common;

/**
 * 评论状态与树结构常量类，定义评论的审核状态及树形结构相关常量。
 * <p>
 * 用于标识评论的审核流程状态，以及评论树的根节点标识。
 * </p>
 */
public final class CommentStatus {

    /** 根评论的父级ID，表示该评论为顶层评论（无父评论） */
    public static final long ROOT_PARENT_ID = 0L;

    /** 待审核状态，评论已提交但尚未通过审核 */
    public static final int PENDING = 0;

    /** 已通过状态，评论已通过审核并对外可见 */
    public static final int APPROVED = 1;

    /**
     * 私有构造方法，防止实例化常量类。
     */
    private CommentStatus() {
    }
}
