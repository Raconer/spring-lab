package com.spring.lab.redis.couponfirstcome.naive.repository

import com.spring.lab.redis.RedisTestContainerConfig
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuerContractTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import
import kotlin.test.assertTrue

// 순차 요청 규칙은 지키지만, 동시 요청에서는 초과 발급되는 것을 보여준다
@DataRedisTest
@Import(RedisTestContainerConfig::class, NaiveCouponIssuer::class)
class NaiveCouponIssuerTest(
    @Autowired override val issuer: NaiveCouponIssuer,
) : CouponIssuerContractTest() {

    // 경쟁 상태를 재현하는 테스트라 타이밍에 따라 드물게 초과 발급이 안 나올 수 있다
    @Test
    fun `서로 다른 사용자 100명이 동시에 요청하면 수량보다 많이 발급된다`() {
        createCoupon(quantity = 10)

        val results = issueConcurrently((1..100).map { "user-$it" })

        val issued = results.count { it == CouponIssueResult.ISSUED }
        assertTrue(issued > 10, "수량 10개보다 많이 발급돼야 한다. 실제 발급: $issued")
    }
}
