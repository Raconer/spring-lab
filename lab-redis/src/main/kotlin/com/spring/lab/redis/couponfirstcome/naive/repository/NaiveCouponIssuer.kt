package com.spring.lab.redis.couponfirstcome.naive.repository

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import com.spring.lab.redis.couponfirstcome.common.repository.CouponRedisKeys
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

// 일부러 원자성 없이 구현한 비교 기준선이다
// 확인(SISMEMBER, SCARD)과 발급(SADD) 사이에 다른 요청이 끼어들어 수량보다 많이 발급된다
@Repository
class NaiveCouponIssuer(
    private val redisTemplate: StringRedisTemplate,
) : CouponIssuer {

    override val method = CouponIssueMethod.NAIVE

    override fun issue(couponId: String, userId: String): CouponIssueResult {
        val quantity = redisTemplate.opsForValue().get(CouponRedisKeys.quantity(couponId))?.toLong()
            ?: return CouponIssueResult.NOT_FOUND
        val issuedUsers = CouponRedisKeys.issuedUsers(couponId)

        if (redisTemplate.opsForSet().isMember(issuedUsers, userId) == true) {
            return CouponIssueResult.ALREADY_ISSUED
        }
        if ((redisTemplate.opsForSet().size(issuedUsers) ?: 0) >= quantity) {
            return CouponIssueResult.SOLD_OUT
        }
        redisTemplate.opsForSet().add(issuedUsers, userId)
        return CouponIssueResult.ISSUED
    }
}
