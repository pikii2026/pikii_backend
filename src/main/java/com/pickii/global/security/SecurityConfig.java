package com.pickii.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반 Stateless 서버이므로 세션/CSRF/폼로그인 비활성화
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 인증 불필요 API (API_SPEC 1-1~1-3, 1-4, 1-5, 1-6, 1-8, 1-10)
                        // 1-6/1-10은 Access Token이 만료된 상태로 들어오는 것이 정상이라
                        // Spring Security가 아니라 AuthService에서 직접 토큰을 검증한다.
                        .requestMatchers(
                                "/auth/email/send", "/auth/email/verify",
                                "/auth/nickname/check",
                                "/auth/signup", "/auth/login",
                                "/auth/token/refresh",
                                "/auth/password/reset",
                                "/auth/social/*/login"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/recruits", "/recruits/*", "/recruits/*/comments").permitAll()
                        // Master Data (API_SPEC 5-1~5-8) — 인증 불필요
                        .requestMatchers(HttpMethod.GET,
                                "/categories", "/topics", "/tech-stacks", "/licenses",
                                "/link-categories", "/keywords", "/apply-keywords", "/universities"
                        ).permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 채팅 이미지 등 로컬 업로드 파일(8-4)은 URL만 있으면 누구나 접근 가능해야 한다(예: <img> 태그)
                        .requestMatchers(HttpMethod.GET, "/static-uploads/**").permitAll()
                        // 그 외 전부 인증 필요 (1-7, 1-9, 1-11~1-13 등)
                        .anyRequest().authenticated())

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** DB_SCHEMA 공통 설계 원칙: 비밀번호는 BCrypt 암호화 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
