package com.pickii.domain.auth.service;

import com.pickii.domain.auth.EmailVerificationPayload;
import com.pickii.domain.auth.RedisKey;
import com.pickii.domain.auth.VerificationPurpose;
import com.pickii.domain.auth.VerificationType;
import com.pickii.domain.auth.dto.EmailVerifyResponse;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import com.pickii.global.mail.MailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 이메일 인증코드 발송/검증 (API_SPEC 1-1, 1-2 / REDIS_POLICY.md 6)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofSeconds(180);

    /** 동일 IP에서 이 시간 동안 보낼 수 있는 최대 요청 수 */
    private static final int IP_REQUEST_LIMIT = 5;
    private static final Duration IP_WINDOW = Duration.ofMinutes(10);

    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;
    private final MailClient mailClient;
    private final VerificationTokenIssuer verificationTokenIssuer;

    public void sendCode(String email, VerificationPurpose purpose, String clientIp) {
        validateMembership(email, purpose);

        String codeKey = RedisKey.emailCode(purpose, email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(codeKey))) {
            // 이전에 발송한 인증코드가 아직 유효(TTL 180초 이내)한 경우 재발송을 막는다
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "이미 발송된 인증코드가 있습니다. 잠시 후 다시 시도해주세요.");
        }
        checkIpRateLimit(clientIp);

        String code = generateCode();
        // 발송에 성공한 코드만 Redis에 저장한다. 순서가 바뀌면 메일 발송 실패 시에도
        // 쿨다운 Key가 남아 사용자가 재요청조차 못 하는 상태가 된다.
        sendMail(email, code);
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL);
    }

    /** 1-2 이메일 인증번호 확인 */
    public EmailVerifyResponse verifyCode(String email, String code, VerificationPurpose purpose) {
        String codeKey = RedisKey.emailCode(purpose, email);
        String savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null) {
            throw new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND);
        }
        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        // 재사용 방지: 인증에 성공한 코드는 즉시 삭제한다
        redisTemplate.delete(codeKey);

        EmailVerificationPayload payload = new EmailVerificationPayload(VerificationType.EMAIL, purpose, email);
        String token = verificationTokenIssuer.issue(payload);

        return new EmailVerifyResponse(true, token);
    }

    private void validateMembership(String email, VerificationPurpose purpose) {
        boolean exists = memberRepository.existsByEmail(email);
        if (purpose == VerificationPurpose.SIGNUP && exists) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (purpose != VerificationPurpose.SIGNUP && !exists) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void checkIpRateLimit(String clientIp) {
        String ipKey = RedisKey.emailCodeIpLimit(clientIp);
        Long count = redisTemplate.opsForValue().increment(ipKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(ipKey, IP_WINDOW);
        }
        if (count != null && count > IP_REQUEST_LIMIT) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private String generateCode() {
        int number = RANDOM.nextInt(1_000_000);
        return "%06d".formatted(number);
    }

    private void sendMail(String email, String code) {
        mailClient.send(email, "[Pickii] 이메일 인증번호",
                "인증번호는 [%s] 입니다. 180초 이내에 입력해주세요.".formatted(code));
    }
}
