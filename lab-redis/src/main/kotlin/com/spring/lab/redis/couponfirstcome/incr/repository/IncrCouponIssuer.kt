package com.spring.lab.redis.couponfirstcome.incr.repository

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import com.spring.lab.redis.couponfirstcome.common.repository.CouponRedisKeys
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

// 원자적 명령(SADD, INCR)의 결과만으로 판단한다. 스크립트나 락 없이도 수량을 넘기지 않는다
// 1. SADD로 사용자를 먼저 기록한다. 이미 있으면 중복이라 순번을 쓰지 않는다
// 2. INCR로 순번을 받는다. 수량을 넘으면 1에서 넣은 사용자를 SREM으로 되돌린다
// 한계: 되돌리기 전 잠깐 동안은 발급 Set에 수량보다 많은 사용자가 보인다
@Repository
class IncrCouponIssuer(
    private val redisTemplate: StringRedisTemplate,
) : CouponIssuer {

    override val method = CouponIssueMethod.INCR

    override fun issue(couponId: String, userId: String): CouponIssueResult {
        val quantity = redisTemplate.opsForValue().get(CouponRedisKeys.quantity(couponId))?.toLong()
            ?: return CouponIssueResult.NOT_FOUND
        val issuedUsers = CouponRedisKeys.issuedUsers(couponId)

        if (redisTemplate.opsForSet().add(issuedUsers, userId) == 0L) {
            return CouponIssueResult.ALREADY_ISSUED
        }

        val order = redisTemplate.opsForValue().increment(CouponRedisKeys.issueCount(couponId))
            ?: throw IllegalStateException("발급 순번을 받지 못했다")
        if (order > quantity) {
            redisTemplate.opsForSet().remove(issuedUsers, userId)
            return CouponIssueResult.SOLD_OUT
        }
        return CouponIssueResult.ISSUED
    }
}
