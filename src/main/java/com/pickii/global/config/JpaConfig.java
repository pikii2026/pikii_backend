package com.pickii.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity의 @CreatedDate / @LastModifiedDate 자동 기록 활성화
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
