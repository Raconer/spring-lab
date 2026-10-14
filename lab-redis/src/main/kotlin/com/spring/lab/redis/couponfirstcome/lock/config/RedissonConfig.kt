package com.spring.lab.redis.couponfirstcome.lock.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Spring Data Redis와 같은 접속 정보로 RedissonClient를 만든다
// DataRedisConnectionDetails를 쓰므로 application.properties, Docker Compose, @ServiceConnection 어느 쪽이든 그대로 따라간다
@Configuration(proxyBeanMethods = false)
class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    fun redissonClient(connectionDetails: DataRedisConnectionDetails): RedissonClient {
        val standalone = connectionDetails.standalone
            ?: throw IllegalStateException("lock 방식은 단일 Redis 서버 구성만 지원한다")
        val config = Config()
        // Redisson setter는 this를 돌려주는 체인 방식이라 Kotlin 프로퍼티 대입 대신 메서드로 호출한다
        config.useSingleServer()
            .setAddress("redis://${standalone.host}:${standalone.port}")
            .setDatabase(standalone.database)
            .setUsername(connectionDetails.username)
            .setPassword(connectionDetails.password)
        return Redisson.create(config)
    }
}
