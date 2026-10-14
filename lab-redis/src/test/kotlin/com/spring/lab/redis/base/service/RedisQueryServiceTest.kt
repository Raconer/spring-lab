package com.spring.lab.redis.base.service

import com.spring.lab.redis.base.dto.RedisPingResponse
import com.spring.lab.redis.base.dto.RedisValueResponse
import com.spring.lab.redis.base.repository.RedisQueryRepository
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Spring과 Redis 없이 repository를 mock으로 바꿔 변환 로직만 검증한다
class RedisQueryServiceTest {

    private val redisQueryRepository = mock(RedisQueryRepository::class.java)
    private val redisQueryService = RedisQueryService(redisQueryRepository)

    @Test
    fun `값과 남은 TTL을 응답으로 돌려준다`() {
        given(redisQueryRepository.get("key")).willReturn("value")
        given(redisQueryRepository.getExpireSeconds("key")).willReturn(30L)

        assertEquals(RedisValueResponse(key = "key", value = "value", ttlSeconds = 30), redisQueryService.find("key"))
    }

    @Test
    fun `만료가 없으면(-1) ttlSeconds는 null이다`() {
        given(redisQueryRepository.get("key")).willReturn("value")
        given(redisQueryRepository.getExpireSeconds("key")).willReturn(-1L)

        assertNull(redisQueryService.find("key")?.ttlSeconds)
    }

    @Test
    fun `값이 없으면 null을 돌려주고 TTL은 조회하지 않는다`() {
        given(redisQueryRepository.get("missing")).willReturn(null)

        assertNull(redisQueryService.find("missing"))
        verify(redisQueryRepository, never()).getExpireSeconds("missing")
    }

    @Test
    fun `PING 응답을 감싸서 돌려준다`() {
        given(redisQueryRepository.ping()).willReturn("PONG")

        assertEquals(RedisPingResponse(redis = "PONG"), redisQueryService.ping())
    }
}
