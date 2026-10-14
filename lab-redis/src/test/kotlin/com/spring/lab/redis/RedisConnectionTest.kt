package com.spring.lab.redis

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@SpringBootTest
@Testcontainers
class RedisConnectionTest(
    @Autowired private val connectionFactory: RedisConnectionFactory,
) {

    @Test
    fun `Redis에 PING을 보내면 PONG이 온다`() {
        val reply = connectionFactory.connection.use { it.ping() }

        assertEquals("PONG", reply)
    }

    companion object {
        // compose.yaml과 같은 이미지를 띄우고, 접속 정보는 @ServiceConnection이 주입한다
        @Container
        @ServiceConnection(name = "redis")
        @JvmStatic
        val redis = GenericContainer<Nothing>("redis:latest").apply { withExposedPorts(6379) }
    }
}
