package com.example.taxassistant.auth;

import com.example.taxassistant.auth.dto.OAuthAuthorizationResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OAuthAuthorizationService {

    private final Map<String, ProviderConfig> providers;

    public OAuthAuthorizationService(
            @Value("${app.security.oauth.google.client-id:}") String googleClientId,
            @Value("${app.security.oauth.kakao.client-id:}") String kakaoClientId
    ) {
        this.providers = Map.of(
                "google", new ProviderConfig(
                        "google",
                        googleClientId,
                        "https://accounts.google.com/o/oauth2/v2/auth",
                        "openid email profile"
                ),
                "kakao", new ProviderConfig(
                        "kakao",
                        kakaoClientId,
                        "https://kauth.kakao.com/oauth/authorize",
                        "profile_nickname account_email"
                )
        );
    }

    public OAuthAuthorizationResponse authorizationUrl(String providerName, String redirectUri) {
        ProviderConfig provider = providers.get(providerName.toLowerCase(Locale.ROOT));
        if (provider == null) {
            return new OAuthAuthorizationResponse(providerName, false, null, "지원하지 않는 소셜 로그인입니다.");
        }
        if (provider.clientId().isBlank()) {
            return new OAuthAuthorizationResponse(
                    provider.name(),
                    false,
                    null,
                    "소셜 로그인을 사용하려면 운영 환경 변수에 client id를 설정해주세요."
            );
        }

        String authorizationUrl = provider.authorizationEndpoint()
                + "?response_type=code"
                + "&client_id=" + encode(provider.clientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(provider.scope())
                + "&state=" + encode(provider.name() + "-login");

        return new OAuthAuthorizationResponse(provider.name(), true, authorizationUrl, "소셜 인증 화면으로 이동합니다.");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record ProviderConfig(
            String name,
            String clientId,
            String authorizationEndpoint,
            String scope
    ) {
    }
}
