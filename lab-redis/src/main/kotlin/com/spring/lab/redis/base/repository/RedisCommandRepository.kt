package com.spring.lab.redis.base.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

// 쓰기 전용. 조회는 RedisQueryRepository에 둔다
@Repository
class RedisCommandRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    // ttl이 null이면 만료 없이 저장한다
    fun set(key: String, value: String, ttl: Duration?) {
        if (ttl == null) {
            redisTemplate.opsForValue().set(key, value)
        } else {
            redisTemplate.opsForValue().set(key, value, ttl)
        }
    }

    // 키가 있어서 지웠으면 true
    fun delete(key: String): Boolean = redisTemplate.delete(key) == true
}
