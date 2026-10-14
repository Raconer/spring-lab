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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 쓰기 결과는 repository를 거치지 않고 StringRedisTemplate으로 직접 확인한다
@DataRedisTest
@Import(RedisTestContainerConfig::class, RedisCommandRepository::class)
class RedisCommandRepositoryTest(
    @Autowired private val redisCommandRepository: RedisCommandRepository,
    @Autowired private val redisTemplate: StringRedisTemplate,
) {

    @BeforeEach
    fun flushDb() {
        redisTemplate.requiredConnectionFactory.connection.use { it.serverCommands().flushDb() }
    }

    @Test
    fun `ttl 없이 저장하면 만료 없이 저장된다`() {
        redisCommandRepository.set("key", "value", null)

        assertEquals("value", redisTemplate.opsForValue().get("key"))
        assertEquals(-1L, redisTemplate.getExpire("key"))
    }

    @Test
    fun `ttl을 주면 만료 시간이 설정된다`() {
        redisCommandRepository.set("key", "value", Duration.ofSeconds(60))

        assertEquals("value", redisTemplate.opsForValue().get("key"))
        assertTrue(redisTemplate.getExpire("key")!! in 1L..60L)
    }

    @Test
    fun `같은 키에 다시 저장하면 값을 덮어쓴다`() {
        redisCommandRepository.set("key", "old", null)

        redisCommandRepository.set("key", "new", null)

        assertEquals("new", redisTemplate.opsForValue().get("key"))
    }

    @Test
    fun `있는 키를 지우면 true를 돌려주고 키가 사라진다`() {
        redisTemplate.opsForValue().set("key", "value")

        assertTrue(redisCommandRepository.delete("key"))
        assertNull(redisTemplate.opsForValue().get("key"))
    }

    @Test
    fun `없는 키를 지우면 false를 돌려준다`() {
        assertFalse(redisCommandRepository.delete("missing"))
    }
}
