plugins {
    kotlin("jvm")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":lab-core"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    // bootRun 시 이 모듈의 compose.yaml을 자동으로 띄운다
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation(testFixtures(project(":lab-core")))
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
}
