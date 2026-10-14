package com.spring.lab.redis.couponfirstcome.lock.repository

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import com.spring.lab.redis.couponfirstcome.common.repository.CouponRedisKeys
import org.redisson.api.RedissonClient
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

// naive와 같은 확인 후 발급 로직을 쿠폰별 분산 락 안에서 실행한다
// 한 번에 한 요청만 확인·발급하므로 수량을 넘기지 않지만, 요청이 줄을 서서 다른 방식보다 느리다
@Repository
class LockCouponIssuer(
    private val redisTemplate: StringRedisTemplate,
    private val redissonClient: RedissonClient,
) : CouponIssuer {

    override val method = CouponIssueMethod.LOCK

    override fun issue(couponId: String, userId: String): CouponIssueResult {
        val lock = redissonClient.getLock(CouponRedisKeys.issueLock(couponId))
        // leaseTime 없이 잡아서 Redisson watchdog이 작업이 끝날 때까지 락을 연장하게 한다
        if (!lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
            throw IllegalStateException("${LOCK_WAIT_SECONDS}초 안에 발급 락을 얻지 못했다")
        }
        try {
            return issueWithoutLock(couponId, userId)
        } finally {
            lock.unlock()
        }
    }

    private fun issueWithoutLock(couponId: String, userId: String): CouponIssueResult {
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

    companion object {
        private const val LOCK_WAIT_SECONDS = 10L
    }
}
