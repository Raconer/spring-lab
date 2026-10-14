package com.spring.lab.redis.couponfirstcome.common.repository

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 모든 발급 방식이 지켜야 할 순차 요청 규칙이다. 방식별 테스트가 상속해서 같은 시나리오로 검증한다
// 데이터 준비와 결과 확인은 issuer를 거치지 않고 StringRedisTemplate으로 직접 한다
abstract class CouponIssuerContractTest {

    @Autowired
    protected lateinit var redisTemplate: StringRedisTemplate

    protected abstract val issuer: CouponIssuer

    protected val couponId = "welcome"

    @BeforeEach
    fun flushDb() {
        redisTemplate.requiredConnectionFactory.connection.use { it.serverCommands().flushDb() }
    }

    @Test
    fun `수량이 남아 있으면 발급하고 사용자를 기록한다`() {
        createCoupon(quantity = 1)

        assertEquals(CouponIssueResult.ISSUED, issuer.issue(couponId, "user-1"))
        assertTrue(redisTemplate.opsForSet().isMember(CouponRedisKeys.issuedUsers(couponId), "user-1") == true)
    }

    @Test
    fun `이미 발급받은 사용자는 다시 발급하지 않는다`() {
        createCoupon(quantity = 10)
        issuer.issue(couponId, "user-1")

        assertEquals(CouponIssueResult.ALREADY_ISSUED, issuer.issue(couponId, "user-1"))
        assertEquals(1L, issuedUserCount())
    }

    @Test
    fun `수량이 소진되면 발급하지 않는다`() {
        createCoupon(quantity = 1)
        issuer.issue(couponId, "user-1")

        assertEquals(CouponIssueResult.SOLD_OUT, issuer.issue(couponId, "user-2"))
        assertEquals(1L, issuedUserCount())
    }

    @Test
    fun `등록되지 않은 쿠폰은 발급하지 않는다`() {
        assertEquals(CouponIssueResult.NOT_FOUND, issuer.issue("missing", "user-1"))
    }

    protected fun createCoupon(quantity: Long) {
        redisTemplate.opsForValue().set(CouponRedisKeys.quantity(couponId), quantity.toString())
    }

    protected fun issuedUserCount(): Long = redisTemplate.opsForSet().size(CouponRedisKeys.issuedUsers(couponId)) ?: 0

    // 모든 요청을 준비시킨 뒤 latch로 한 번에 출발시킨다
    protected fun issueConcurrently(userIds: List<String>): List<CouponIssueResult> {
        val start = CountDownLatch(1)
        return Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures = userIds.map { userId ->
                executor.submit(Callable {
                    start.await()
                    issuer.issue(couponId, userId)
                })
            }
            start.countDown()
            futures.map { it.get() }
        }
    }
}
