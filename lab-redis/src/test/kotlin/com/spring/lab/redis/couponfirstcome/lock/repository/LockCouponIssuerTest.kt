package com.spring.lab.redis.couponfirstcome.lock.repository

import com.spring.lab.redis.RedisTestContainerConfig
import com.spring.lab.redis.couponfirstcome.common.repository.ConcurrentCouponIssuerContractTest
import com.spring.lab.redis.couponfirstcome.common.repository.CouponRedisKeys
import com.spring.lab.redis.couponfirstcome.lock.config.RedissonConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import kotlin.test.assertFalse

// 계약 테스트에 더해, 발급이 끝나면 락을 풀어 두는지 검증한다
@DataRedisTest
@Import(RedisTestContainerConfig::class, RedissonConfig::class, LockCouponIssuer::class)
class LockCouponIssuerTest(
    @Autowired override val issuer: LockCouponIssuer,
) : ConcurrentCouponIssuerContractTest() {

    @Test
    fun `발급이 끝나면 락 키가 남지 않는다`() {
        createCoupon(quantity = 1)

        issuer.issue(couponId, "user-1")

        assertFalse(redisTemplate.hasKey(CouponRedisKeys.issueLock(couponId)) == true)
    }
}
