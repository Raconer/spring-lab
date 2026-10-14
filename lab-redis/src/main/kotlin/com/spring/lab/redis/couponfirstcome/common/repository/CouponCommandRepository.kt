package com.spring.lab.redis.couponfirstcome.common.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

// 쿠폰 등록 전용. 발급은 방식별 CouponIssuer 구현체가, 조회는 CouponQueryRepository가 한다
@Repository
class CouponCommandRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    // 수량을 정하고 기존 발급 기록과 순번을 지운다. 같은 쿠폰을 다시 등록하면 처음부터 다시 발급한다
    fun create(couponId: String, quantity: Long) {
        redisTemplate.delete(listOf(CouponRedisKeys.issuedUsers(couponId), CouponRedisKeys.issueCount(couponId)))
        redisTemplate.opsForValue().set(CouponRedisKeys.quantity(couponId), quantity.toString())
    }
}
