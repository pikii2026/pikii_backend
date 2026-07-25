package com.pickii.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickii.domain.auth.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Verification Token(auth:verify:{UUID}) 발급 공통 로직 (REDIS_POLICY.md 7)
 *
 * <p>이메일 인증(1-2)과 닉네임 확인(1-3) 모두 "검증 통과 → UUID 발급 → Redis 저장" 과정이 동일해서
 * 별도 컴포넌트로 뽑아냈다. payload 객체를 JSON으로 직렬화해 저장하고, 발급된 토큰 문자열을 반환한다.</p>
 */
@Component
@RequiredArgsConstructor
public class VerificationTokenIssuer {

    private static final Duration TOKEN_TTL = Duration.ofSeconds(900);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public String issue(Object payload) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(RedisKey.verificationToken(token), writeJson(payload), TOKEN_TTL);
        return token;
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Verification Token 직렬화에 실패했습니다.", e);
        }
    }
}
