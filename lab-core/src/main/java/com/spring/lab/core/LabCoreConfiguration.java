package com.spring.lab.core;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LabCoreConfiguration {

    // Swagger 문서 제목을 모듈의 spring.application.name으로 맞춘다
    @Bean
    OpenAPI labOpenApi(@Value("${spring.application.name:lab}") String applicationName) {
        return new OpenAPI().info(new Info().title(applicationName).version("v1"));
    }

}
