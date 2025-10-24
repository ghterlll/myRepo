package com.mobile.aura.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.constant.EmailCodePurpose;
import com.mobile.aura.domain.auth.EmailCode;
import com.mobile.aura.mapper.EmailCodeMapper;
import com.mobile.aura.service.EmailCodeService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private final EmailCodeMapper codeMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUser;

    @Override
    @Transactional
    public void sendResetPasswordCode(Long userId, String email) {
        String rawCode = EmailCode.generateRawCode();
        EmailCode emailCode = EmailCode.createForPasswordReset(userId, email, rawCode);

        findExistingPasswordResetCode(userId)
                .ifPresentOrElse(
                        existing -> {
                            emailCode.updateWith(existing);
                            codeMapper.updateById(emailCode);
                        },
                        () -> codeMapper.insert(emailCode)
                );

        sendEmail(emailCode.toPasswordResetEmail(rawCode));
    }

    private Optional<EmailCode> findExistingPasswordResetCode(Long userId) {
        return Optional.ofNullable(codeMapper.selectOne(new LambdaQueryWrapper<EmailCode>()
                .eq(EmailCode::getUserId, userId)
                .eq(EmailCode::getPurpose, EmailCodePurpose.RESET_PASSWORD.name())));
    }

    @Override
    @Transactional
    public void sendRegistrationCode(Long userId, String email) {
        String rawCode = EmailCode.generateRawCode();
        EmailCode emailCode = EmailCode.createForRegistration(userId, email, rawCode);

        findExistingRegistrationCode(email)
                .ifPresentOrElse(
                        existing -> {
                            emailCode.updateWith(existing);
                            codeMapper.updateById(emailCode);
                        },
                        () -> codeMapper.insert(emailCode)
                );

        sendEmail(emailCode.toRegistrationEmail(rawCode));
    }

    private Optional<EmailCode> findExistingRegistrationCode(String email) {
        return Optional.ofNullable(codeMapper.selectOne(new LambdaQueryWrapper<EmailCode>()
                .eq(EmailCode::getEmail, email)
                .eq(EmailCode::getPurpose, EmailCodePurpose.REGISTER.name())
                .isNull(EmailCode::getUsedAt)));
    }

    @Override
    @Transactional
    public void verifyRegistrationCode(String email, String code) {
        EmailCode emailCode = Optional.ofNullable(codeMapper.selectOne(new LambdaQueryWrapper<EmailCode>()
                        .eq(EmailCode::getEmail, email)
                        .eq(EmailCode::getPurpose, EmailCodePurpose.REGISTER.name())))
                .orElseThrow(() -> new BizException(CommonStatusEnum.EMAIL_CODE_INVALID));

        emailCode.verifyAndThrow(email, code);
        emailCode.markAsUsed();
        codeMapper.updateById(emailCode);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyOrThrow(Long userId, String email, String code, EmailCodePurpose purpose) {
        EmailCode emailCode = Optional.ofNullable(codeMapper.selectOne(new LambdaQueryWrapper<EmailCode>()
                        .eq(EmailCode::getUserId, userId)
                        .eq(EmailCode::getPurpose, purpose.name())))
                .orElseThrow(() -> new BizException(CommonStatusEnum.EMAIL_CODE_INVALID));

        emailCode.verifyAndThrow(email, code);
    }

    private void sendEmail(EmailCode.EmailMessage emailMessage) {
        mailSender.send(emailMessage.toSimpleMailMessage(mailUser));
    }
}
