package com.spring.lab.redis.base.dto

// ttlSeconds는 남은 만료 시간(초). 만료가 없으면 null
data class RedisValueResponse(
    val key: String,
    val value: String,
    val ttlSeconds: Long?,
)
