package com.spring.lab.redis.base.service

import com.spring.lab.redis.base.dto.RedisValueSaveRequest
import com.spring.lab.redis.base.repository.RedisCommandRepository
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Duration
import kotlin.test.assertTrue

// Spring과 Redis 없이 repository를 mock으로 바꿔 변환 로직만 검증한다
class RedisCommandServiceTest {

    private val redisCommandRepository = mock(RedisCommandRepository::class.java)
    private val redisCommandService = RedisCommandService(redisCommandRepository)

    @Test
    fun `ttlSeconds가 있으면 Duration으로 바꿔 저장한다`() {
        redisCommandService.save("key", RedisValueSaveRequest(value = "value", ttlSeconds = 60))

        verify(redisCommandRepository).set("key", "value", Duration.ofSeconds(60))
    }

    @Test
    fun `ttlSeconds가 없으면 만료 없이 저장한다`() {
        redisCommandService.save("key", RedisValueSaveRequest(value = "value"))

        verify(redisCommandRepository).set("key", "value", null)
    }

    @Test
    fun `삭제 결과를 그대로 돌려준다`() {
        given(redisCommandRepository.delete("key")).willReturn(true)

        assertTrue(redisCommandService.delete("key"))
    }
}
