package com.pickii.domain.auth.service;

import com.pickii.domain.auth.NicknameVerificationPayload;
import com.pickii.domain.auth.VerificationType;
import com.pickii.domain.auth.dto.NicknameCheckResponse;
import com.pickii.domain.member.repository.MemberRepository;
import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 닉네임 중복 확인 (API_SPEC 1-3)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NicknameVerificationService {

    /** API_SPEC 0.7 공통 Validation 규칙: 닉네임 1자 이상 ~ 10자 이하 */
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 10;

    private final MemberRepository memberRepository;
    private final VerificationTokenIssuer verificationTokenIssuer;

    public NicknameCheckResponse check(String nickname) {
        validateLength(nickname);
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        NicknameVerificationPayload payload = new NicknameVerificationPayload(VerificationType.NICKNAME, nickname);
        String token = verificationTokenIssuer.issue(payload);

        return new NicknameCheckResponse(true, token);
    }

    private void validateLength(String nickname) {
        if (nickname == null || nickname.isBlank()
                || nickname.length() < MIN_LENGTH || nickname.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "닉네임은 1자 이상 10자 이하로 입력해주세요.");
        }
    }
}
