package com.codenow.common;

/**
 * 作者申请状态常量类，定义作者申请流程中的各个状态。
 * <p>
 * 用户申请成为作者时，申请会经历待审核、已通过、已拒绝或已取消等状态。
 * </p>
 */
public final class AuthorApplicationStatus {

    /** 待审核状态，申请已提交，等待管理员审核 */
    public static final String PENDING = "PENDING";

    /** 已通过状态，申请已获批准，用户已升级为作者 */
    public static final String APPROVED = "APPROVED";

    /** 已拒绝状态，申请未通过审核 */
    public static final String REJECTED = "REJECTED";

    /** 已取消状态，申请人主动撤回了申请 */
    public static final String CANCELED = "CANCELED";

    /**
     * 私有构造方法，防止实例化常量类。
     */
    private AuthorApplicationStatus() {
    }
}
