package com.spring.lab.redis.couponfirstcome.common.repository

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// 동시 요청에서도 수량과 중복 규칙을 지켜야 하는 발급 방식이 상속한다 (naive 제외)
abstract class ConcurrentCouponIssuerContractTest : CouponIssuerContractTest() {

    @Test
    fun `서로 다른 사용자 100명이 동시에 요청해도 수량 10개만 발급된다`() {
        createCoupon(quantity = 10)

        val results = issueConcurrently((1..100).map { "user-$it" })

        assertEquals(10, results.count { it == CouponIssueResult.ISSUED })
        assertEquals(90, results.count { it == CouponIssueResult.SOLD_OUT })
        assertEquals(10L, issuedUserCount())
    }

    @Test
    fun `같은 사용자가 동시에 50번 요청해도 한 번만 발급된다`() {
        createCoupon(quantity = 10)

        val results = issueConcurrently(List(50) { "user-1" })

        assertEquals(1, results.count { it == CouponIssueResult.ISSUED })
        assertEquals(49, results.count { it == CouponIssueResult.ALREADY_ISSUED })
        assertEquals(1L, issuedUserCount())
    }
}
