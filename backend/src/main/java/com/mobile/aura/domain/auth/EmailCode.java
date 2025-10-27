package com.mobile.aura.domain.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.constant.EmailCodePurpose;
import com.mobile.aura.support.BizException;
import lombok.Data;
import org.springframework.mail.SimpleMailMessage;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Data
@TableName("email_code")
public class EmailCode {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String email;
    private String purpose;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    public static EmailCode createForRegistration(String email, String rawCode) {
        EmailCode code = new EmailCode();
        code.userId = null;
        code.email = email;
        code.purpose = EmailCodePurpose.REGISTER.name();
        code.codeHash = hashCode(rawCode);
        code.expiresAt = LocalDateTime.now().plusMinutes(10);
        return code;
    }

    public static EmailCode createForPasswordReset(Long userId, String email, String rawCode) {
        EmailCode code = new EmailCode();
        code.userId = userId;
        code.email = email;
        code.purpose = EmailCodePurpose.RESET_PASSWORD.name();
        code.codeHash = hashCode(rawCode);
        code.expiresAt = LocalDateTime.now().plusMinutes(10);
        return code;
    }

    public static String generateRawCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private static String hashCode(String rawCode) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(rawCode.getBytes());
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void verifyAndThrow(String email, String rawCode) {
        verifyNotUsed();
        verifyEmailMatches(email);
        verifyNotExpired();
        verifyCodeMatches(rawCode);
    }

    private void verifyNotUsed() {
        Optional.ofNullable(usedAt)
                .ifPresent(used -> {
                    throw new BizException(CommonStatusEnum.EMAIL_CODE_USED);
                });
    }

    private void verifyEmailMatches(String emailToVerify) {
        Optional.of(email.equalsIgnoreCase(emailToVerify))
                .filter(matches -> !matches)
                .ifPresent(mismatch -> {
                    throw new BizException(CommonStatusEnum.EMAIL_MISMATCH);
                });
    }

    private void verifyNotExpired() {
        Optional.of(expiresAt.isBefore(LocalDateTime.now()))
                .filter(expired -> expired)
                .ifPresent(expired -> {
                    throw new BizException(CommonStatusEnum.EMAIL_CODE_EXPIRED);
                });
    }

    private void verifyCodeMatches(String rawCode) {
        String inputHash = hashCode(rawCode);
        Optional.of(codeHash.equals(inputHash))
                .filter(matches -> !matches)
                .ifPresent(mismatch -> {
                    throw new BizException(CommonStatusEnum.EMAIL_CODE_INVALID);
                });
    }

    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
    }

    public void updateWith(EmailCode existingRecord) {
        this.id = existingRecord.id;
    }

    public EmailMessage toRegistrationEmail(String rawCode) {
        return new EmailMessage(
                email,
                "Registration Verification Code",
                String.format("Your verification code is: %s, valid for 10 minutes.", rawCode)
        );
    }

    public EmailMessage toPasswordResetEmail(String rawCode) {
        return new EmailMessage(
                email,
                "Reset Password Verification Code",
                String.format("Your verification code is: %s, valid for 10 minutes.", rawCode)
        );
    }

    public record EmailMessage(String to, String subject, String body) {

    public SimpleMailMessage toSimpleMailMessage(String fromAddress) {
            org.springframework.mail.SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            return msg;
        }
    }
}
