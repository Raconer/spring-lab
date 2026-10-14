package com.spring.lab.redis.base.service

import com.spring.lab.redis.base.dto.RedisPingResponse
import com.spring.lab.redis.base.dto.RedisValueResponse
import com.spring.lab.redis.base.repository.RedisQueryRepository
import org.springframework.stereotype.Service

@Service
class RedisQueryService(
    private val redisQueryRepository: RedisQueryRepository,
) {

    // 키가 없으면 null
    fun find(key: String): RedisValueResponse? {
        val value = redisQueryRepository.get(key) ?: return null
        // 만료 없음(-1)은 응답에서 null로 보낸다
        val ttlSeconds = redisQueryRepository.getExpireSeconds(key)?.takeIf { it >= 0 }
        return RedisValueResponse(key = key, value = value, ttlSeconds = ttlSeconds)
    }

    fun ping(): RedisPingResponse = RedisPingResponse(redis = redisQueryRepository.ping())
}
