package com.spring.lab.redis.couponfirstcome.common.repository

import com.spring.lab.redis.RedisTestContainerConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// 등록 결과는 repository를 거치지 않고 StringRedisTemplate으로 직접 확인한다
@DataRedisTest
@Import(RedisTestContainerConfig::class, CouponCommandRepository::class)
class CouponCommandRepositoryTest(
    @Autowired private val couponCommandRepository: CouponCommandRepository,
    @Autowired private val redisTemplate: StringRedisTemplate,
) {

    private val couponId = "welcome"

    @BeforeEach
    fun flushDb() {
        redisTemplate.requiredConnectionFactory.connection.use { it.serverCommands().flushDb() }
    }

    @Test
    fun `쿠폰을 등록하면 수량이 저장된다`() {
        couponCommandRepository.create(couponId, 100)

        assertEquals("100", redisTemplate.opsForValue().get(CouponRedisKeys.quantity(couponId)))
    }

    @Test
    fun `다시 등록하면 기존 발급 기록과 순번을 지운다`() {
        redisTemplate.opsForSet().add(CouponRedisKeys.issuedUsers(couponId), "user-1")
        redisTemplate.opsForValue().set(CouponRedisKeys.issueCount(couponId), "1")

        couponCommandRepository.create(couponId, 100)

        assertFalse(redisTemplate.hasKey(CouponRedisKeys.issuedUsers(couponId)) == true)
        assertFalse(redisTemplate.hasKey(CouponRedisKeys.issueCount(couponId)) == true)
    }
}
