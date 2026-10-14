package com.spring.lab.redis

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

// Redis가 필요한 테스트가 @Import로 공유한다. 같은 설정의 테스트끼리는 컨텍스트와 컨테이너를 재사용한다
@TestConfiguration(proxyBeanMethods = false)
class RedisTestContainerConfig {

    // compose.yaml과 같은 이미지를 띄우고, 접속 정보는 @ServiceConnection이 주입한다
    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> =
        GenericContainer(DockerImageName.parse("redis:latest")).withExposedPorts(6379)
}
