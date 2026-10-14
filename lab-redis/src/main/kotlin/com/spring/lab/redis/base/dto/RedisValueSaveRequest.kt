package com.spring.lab.redis.base.dto

// ttlSeconds가 없으면 만료 없이 저장한다
data class RedisValueSaveRequest(
    val value: String,
    val ttlSeconds: Long? = null,
)
