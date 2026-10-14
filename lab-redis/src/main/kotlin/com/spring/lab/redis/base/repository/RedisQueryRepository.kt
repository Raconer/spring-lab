package com.spring.lab.redis.base.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

// 읽기 전용. 저장·삭제는 RedisCommandRepository에 둔다
@Repository
class RedisQueryRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    fun get(key: String): String? = redisTemplate.opsForValue().get(key)

    // Redis TTL 규칙을 그대로 돌려준다: 만료 없음 -1, 키 없음 -2
    fun getExpireSeconds(key: String): Long? = redisTemplate.getExpire(key)

    // Redis에 PING을 보내고 응답(PONG)을 그대로 돌려준다. 연결되지 않으면 예외가 난다
    fun ping(): String? = redisTemplate.requiredConnectionFactory.connection.use { it.ping() }
}
