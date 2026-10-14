package com.spring.lab.redis

import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/redis")
class RedisPingController(
    private val connectionFactory: RedisConnectionFactory,
) {

    // Redis에 PING을 보내 연결을 확인한다. 연결되지 않으면 예외가 나서 500으로 응답한다
    @GetMapping("/ping")
    fun ping(): Map<String, String?> =
        connectionFactory.connection.use { mapOf("redis" to it.ping()) }
}
