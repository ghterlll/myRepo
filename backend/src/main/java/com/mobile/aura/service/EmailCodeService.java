package com.mobile.aura.service;

import com.mobile.aura.constant.EmailCodePurpose;

public interface EmailCodeService {
    void sendResetPasswordCode(Long userId, String email);
    void sendRegistrationCode(Long userId, String email);
    void verifyOrThrow(Long userId, String email, String code, EmailCodePurpose purpose);
    void verifyRegistrationCode(String email, String code);
}
