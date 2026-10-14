package com.spring.lab.redis.couponfirstcome.incr.repository

import com.spring.lab.redis.RedisTestContainerConfig
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.repository.ConcurrentCouponIssuerContractTest
import com.spring.lab.redis.couponfirstcome.common.repository.CouponRedisKeys
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// 계약 테스트에 더해, 수량을 넘은 요청의 기록을 되돌리는지 검증한다
@DataRedisTest
@Import(RedisTestContainerConfig::class, IncrCouponIssuer::class)
class IncrCouponIssuerTest(
    @Autowired override val issuer: IncrCouponIssuer,
) : ConcurrentCouponIssuerContractTest() {

    @Test
    fun `순번이 수량을 넘으면 발급 기록을 되돌리고 순번은 그대로 남긴다`() {
        createCoupon(quantity = 1)
        issuer.issue(couponId, "user-1")

        assertEquals(CouponIssueResult.SOLD_OUT, issuer.issue(couponId, "user-2"))
        assertFalse(redisTemplate.opsForSet().isMember(CouponRedisKeys.issuedUsers(couponId), "user-2") == true)
        assertEquals("2", redisTemplate.opsForValue().get(CouponRedisKeys.issueCount(couponId)))
    }

    @Test
    fun `중복 요청은 순번을 쓰지 않는다`() {
        createCoupon(quantity = 10)
        issuer.issue(couponId, "user-1")

        issuer.issue(couponId, "user-1")

        assertEquals("1", redisTemplate.opsForValue().get(CouponRedisKeys.issueCount(couponId)))
    }
}
