package com.spring.lab.redis.base.controller

import com.spring.lab.redis.base.dto.RedisValueSaveRequest
import com.spring.lab.redis.base.service.RedisCommandService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 쓰기 API. 조회 API는 RedisQueryController에 둔다
@RestController
@RequestMapping("/redis")
class RedisCommandController(
    private val redisCommandService: RedisCommandService,
) {

    @PutMapping("/values/{key}")
    fun save(@PathVariable key: String, @RequestBody request: RedisValueSaveRequest): ResponseEntity<Void> {
        redisCommandService.save(key, request)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/values/{key}")
    fun delete(@PathVariable key: String): ResponseEntity<Void> =
        if (redisCommandService.delete(key)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
}
