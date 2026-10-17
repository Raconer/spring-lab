package com.spring.lab.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

// lab-core.properties는 모든 모듈의 기본 설정이다. 모듈의 application.properties가 같은 키를 덮어쓴다
@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:lab-core.properties")
public class LabCoreConfiguration {

    // Swagger 문서 제목을 모듈의 spring.application.name으로 맞춘다
    @Bean
    OpenAPI labOpenApi(@Value("${spring.application.name:lab}") String applicationName) {
        return new OpenAPI().info(new Info().title(applicationName).version("v1"));
    }

}
