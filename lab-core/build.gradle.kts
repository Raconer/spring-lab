plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    // 기능 모듈에 그대로 노출되는 공통 Spring Boot 의존성
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-webmvc")
    // Boot BOM에 없어서 버전을 직접 지정한다 (3.1.1은 Boot 4.1.0 기준 빌드)
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    // 기능 모듈이 testImplementation(testFixtures(project(":lab-core")))로 가져가는 공통 테스트 의존성
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-webmvc-test")
    testFixturesApi("org.springframework.boot:spring-boot-testcontainers")
    testFixturesApi("org.testcontainers:testcontainers-junit-jupiter")
}
