package com.codenow.service;

public interface EmailCodeService {
    void sendRegisterCode(String email);

    void sendResetCode(String email);

    void verifyRegisterCode(String email, String code);

    void verifyResetCode(String email, String code);

    void sendChangeEmailCode(String email);

    void verifyChangeEmailCode(String email, String code);
}
