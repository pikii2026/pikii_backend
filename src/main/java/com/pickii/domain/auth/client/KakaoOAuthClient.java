package com.pickii.domain.auth.client;

import com.pickii.global.exception.BusinessException;
import com.pickii.global.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 소셜 Access Token 검증 (API_SPEC 1-10)
 *
 * <p>클라이언트가 보낸 소셜 Access Token은 신뢰하지 않고, 매번 카카오 API로 재검증한 뒤
 * providerUserId만 뽑아 쓰고 토큰 자체는 저장하지 않는다.</p>
 */
@Component
public class KakaoOAuthClient {

    private static final String USER_ME_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;

    public KakaoOAuthClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /** 카카오 API로 토큰을 검증하고 providerUserId(카카오 고유 id)를 반환한다. */
    public String getProviderUserId(String socialAccessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(USER_ME_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + socialAccessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id() == null) {
                throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }
            return String.valueOf(response.id());
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }
}