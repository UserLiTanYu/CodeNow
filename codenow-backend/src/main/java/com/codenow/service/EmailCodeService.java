package com.codenow.service;

/**
 * 邮箱验证码服务接口
 */
public interface EmailCodeService {

    /**
     * 发送注册验证码
     *
     * @param email 目标邮箱
     */
    void sendRegisterCode(String email);

    /**
     * 发送重置密码验证码
     *
     * @param email 目标邮箱
     */
    void sendResetCode(String email);

    /**
     * 验证注册验证码
     *
     * @param email 目标邮箱
     * @param code  验证码
     */
    void verifyRegisterCode(String email, String code);

    /**
     * 验证重置密码验证码
     *
     * @param email 目标邮箱
     * @param code  验证码
     */
    void verifyResetCode(String email, String code);

    /**
     * 发送更换邮箱验证码
     *
     * @param email 目标新邮箱
     */
    void sendChangeEmailCode(String email);

    /**
     * 验证更换邮箱验证码
     *
     * @param email 目标新邮箱
     * @param code  验证码
     */
    void verifyChangeEmailCode(String email, String code);
}
