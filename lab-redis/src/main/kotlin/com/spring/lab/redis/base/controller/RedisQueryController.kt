package com.spring.lab.redis.base.controller

import com.spring.lab.redis.base.dto.RedisPingResponse
import com.spring.lab.redis.base.dto.RedisValueResponse
import com.spring.lab.redis.base.service.RedisQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 조회 API. 쓰기 API는 RedisCommandController에 둔다
@RestController
@RequestMapping("/redis")
class RedisQueryController(
    private val redisQueryService: RedisQueryService,
) {

    @GetMapping("/values/{key}")
    fun find(@PathVariable key: String): ResponseEntity<RedisValueResponse> =
        redisQueryService.find(key)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    // Redis 연결을 확인한다. 연결되지 않으면 예외가 나서 500으로 응답한다
    @GetMapping("/ping")
    fun ping(): RedisPingResponse = redisQueryService.ping()
}
