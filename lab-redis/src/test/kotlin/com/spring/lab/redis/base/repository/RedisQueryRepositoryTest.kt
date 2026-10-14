package com.spring.lab.redis.base.repository

import com.spring.lab.redis.RedisTestContainerConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 테스트 데이터는 repository를 거치지 않고 StringRedisTemplate으로 직접 넣는다
@DataRedisTest
@Import(RedisTestContainerConfig::class, RedisQueryRepository::class)
class RedisQueryRepositoryTest(
    @Autowired private val redisQueryRepository: RedisQueryRepository,
    @Autowired private val redisTemplate: StringRedisTemplate,
) {

    @BeforeEach
    fun flushDb() {
        redisTemplate.requiredConnectionFactory.connection.use { it.serverCommands().flushDb() }
    }

    @Test
    fun `있는 키를 조회하면 값을 돌려준다`() {
        redisTemplate.opsForValue().set("key", "value")

        assertEquals("value", redisQueryRepository.get("key"))
    }

    @Test
    fun `없는 키를 조회하면 null을 돌려준다`() {
        assertNull(redisQueryRepository.get("missing"))
    }

    @Test
    fun `만료가 있는 키는 남은 초를 돌려준다`() {
        redisTemplate.opsForValue().set("key", "value", Duration.ofSeconds(60))

        assertTrue(redisQueryRepository.getExpireSeconds("key")!! in 1L..60L)
    }

    @Test
    fun `만료가 없는 키는 -1, 없는 키는 -2를 돌려준다`() {
        redisTemplate.opsForValue().set("key", "value")

        assertEquals(-1L, redisQueryRepository.getExpireSeconds("key"))
        assertEquals(-2L, redisQueryRepository.getExpireSeconds("missing"))
    }

    @Test
    fun `PING을 보내면 PONG이 온다`() {
        assertEquals("PONG", redisQueryRepository.ping())
    }
}
