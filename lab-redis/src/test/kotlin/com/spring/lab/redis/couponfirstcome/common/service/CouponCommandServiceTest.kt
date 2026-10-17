package com.spring.lab.redis.couponfirstcome.common.service

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.dto.CouponCreateRequest
import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueRequest
import com.spring.lab.redis.couponfirstcome.common.repository.CouponCommandRepository
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.assertEquals

// Spring과 Redis 없이 repository와 issuer를 mock으로 바꿔 요청 전달, 방식 선택, 메트릭 기록만 검증한다
class CouponCommandServiceTest {

    private val couponCommandRepository = mock(CouponCommandRepository::class.java)
    private val luaIssuer = issuerOf(CouponIssueMethod.LUA)
    private val lockIssuer = issuerOf(CouponIssueMethod.LOCK)
    private val meterRegistry = SimpleMeterRegistry()
    private val couponCommandService = CouponCommandService(couponCommandRepository, listOf(luaIssuer, lockIssuer), meterRegistry)

    @Test
    fun `등록 요청의 수량으로 쿠폰을 등록한다`() {
        couponCommandService.create("welcome", CouponCreateRequest(quantity = 100))

        verify(couponCommandRepository).create("welcome", 100)
    }

    @Test
    fun `요청한 방식의 issuer로 발급하고 결과를 그대로 돌려준다`() {
        given(lockIssuer.issue("welcome", "user-1")).willReturn(CouponIssueResult.SOLD_OUT)

        val result = couponCommandService.issue("welcome", CouponIssueMethod.LOCK, CouponIssueRequest(userId = "user-1"))

        assertEquals(CouponIssueResult.SOLD_OUT, result)
        verify(luaIssuer, never()).issue("welcome", "user-1")
    }

    @Test
    fun `발급 결과를 방식·결과 태그로 coupon.issue 메트릭에 기록한다`() {
        given(lockIssuer.issue("welcome", "user-1")).willReturn(CouponIssueResult.ISSUED)

        couponCommandService.issue("welcome", CouponIssueMethod.LOCK, CouponIssueRequest(userId = "user-1"))

        val timer = meterRegistry.get("coupon.issue").tag("method", "lock").tag("result", "ISSUED").timer()
        assertEquals(1L, timer.count())
    }

    @Test
    fun `구현체가 없는 방식이면 예외가 난다`() {
        assertThrows<IllegalStateException> {
            couponCommandService.issue("welcome", CouponIssueMethod.INCR, CouponIssueRequest(userId = "user-1"))
        }
    }

    private fun issuerOf(method: CouponIssueMethod): CouponIssuer =
        mock(CouponIssuer::class.java).also { given(it.method).willReturn(method) }
}
