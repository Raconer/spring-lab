package com.spring.lab.redis.couponfirstcome.common.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

// 읽기 전용. 등록·발급은 CouponCommandRepository에 둔다
@Repository
class CouponQueryRepository(
    private val redisTemplate: StringRedisTemplate,
) {

    // 등록되지 않은 쿠폰이면 null
    fun getQuantity(couponId: String): Long? =
        redisTemplate.opsForValue().get(CouponRedisKeys.quantity(couponId))?.toLong()

    fun countIssued(couponId: String): Long =
        redisTemplate.opsForSet().size(CouponRedisKeys.issuedUsers(couponId)) ?: 0

    fun isIssued(couponId: String, userId: String): Boolean =
        redisTemplate.opsForSet().isMember(CouponRedisKeys.issuedUsers(couponId), userId) == true
}
