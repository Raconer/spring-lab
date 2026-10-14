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
import kotlin.test.assertNull
import kotlin.test.assertTrue

// 테스트 데이터는 repository를 거치지 않고 StringRedisTemplate으로 직접 넣는다
@DataRedisTest
@Import(RedisTestContainerConfig::class, CouponQueryRepository::class)
class CouponQueryRepositoryTest(
    @Autowired private val couponQueryRepository: CouponQueryRepository,
    @Autowired private val redisTemplate: StringRedisTemplate,
) {

    private val couponId = "welcome"

    @BeforeEach
    fun flushDb() {
        redisTemplate.requiredConnectionFactory.connection.use { it.serverCommands().flushDb() }
    }

    @Test
    fun `등록된 쿠폰의 수량을 돌려준다`() {
        redisTemplate.opsForValue().set(CouponRedisKeys.quantity(couponId), "100")

        assertEquals(100L, couponQueryRepository.getQuantity(couponId))
    }

    @Test
    fun `등록되지 않은 쿠폰의 수량은 null이다`() {
        assertNull(couponQueryRepository.getQuantity("missing"))
    }

    @Test
    fun `발급 수를 돌려주고 발급 기록이 없으면 0이다`() {
        redisTemplate.opsForSet().add(CouponRedisKeys.issuedUsers(couponId), "user-1", "user-2")

        assertEquals(2L, couponQueryRepository.countIssued(couponId))
        assertEquals(0L, couponQueryRepository.countIssued("missing"))
    }

    @Test
    fun `사용자의 발급 여부를 돌려준다`() {
        redisTemplate.opsForSet().add(CouponRedisKeys.issuedUsers(couponId), "user-1")

        assertTrue(couponQueryRepository.isIssued(couponId, "user-1"))
        assertFalse(couponQueryRepository.isIssued(couponId, "user-2"))
    }
}
