package com.pickii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class PickiiApplication {

    public static void main(String[] args) {
        // API_SPEC 0.3: 모든 날짜/시간은 KST(UTC+9) 기준. 코드 전반에서 LocalDateTime.now()를
        // 타임존 명시 없이 쓰고 있어(마감시각/생성일시 등), JVM 기본 타임존이 KST가 아닌 환경에
        // 배포되면 전부 어긋난다. 배포 환경과 무관하게 항상 KST로 계산되도록 앱 시작 시점에 고정한다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(PickiiApplication.class, args);
    }

}
