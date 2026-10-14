package com.spring.lab.redis.base.dto

// 기존 응답 형태 {"redis": "PONG"}을 유지한다
data class RedisPingResponse(
    val redis: String?,
)
