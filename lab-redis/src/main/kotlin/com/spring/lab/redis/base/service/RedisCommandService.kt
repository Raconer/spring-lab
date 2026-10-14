package com.spring.lab.redis.base.service

import com.spring.lab.redis.base.dto.RedisValueSaveRequest
import com.spring.lab.redis.base.repository.RedisCommandRepository
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisCommandService(
    private val redisCommandRepository: RedisCommandRepository,
) {

    fun save(key: String, request: RedisValueSaveRequest) {
        redisCommandRepository.set(key, request.value, request.ttlSeconds?.let(Duration::ofSeconds))
    }

    fun delete(key: String): Boolean = redisCommandRepository.delete(key)
}
