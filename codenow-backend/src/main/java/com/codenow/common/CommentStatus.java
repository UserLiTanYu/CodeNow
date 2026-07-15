package com.codenow.common;

/**
 * 评论状态与树结构常量。
 */
public final class CommentStatus {

    public static final long ROOT_PARENT_ID = 0L;
    public static final int PENDING = 0;
    public static final int APPROVED = 1;

    private CommentStatus() {
    }
}
