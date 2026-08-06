package com.pickii.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 파일시스템에 저장된 업로드 파일(채팅 이미지 등)을 정적 리소스로 노출한다.
 * TODO: 실제 Object Storage(S3) 연동 전까지의 목업 설정.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static-uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
