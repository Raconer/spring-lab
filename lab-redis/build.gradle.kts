plugins {
    kotlin("jvm")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":lab-core"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // couponfirstcome.lock의 분산 락. Boot BOM에 없어서 버전을 직접 지정한다 (2026-10 기준 최신)
    implementation("org.redisson:redisson:4.8.0")
    // bootRun 시 이 모듈의 compose.yaml을 자동으로 띄운다
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation(testFixtures(project(":lab-core")))
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
}
