package com.spring.lab.redis.couponfirstcome.common.service

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.dto.CouponCreateRequest
import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueRequest
import com.spring.lab.redis.couponfirstcome.common.repository.CouponCommandRepository
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.assertEquals

// Spring과 Redis 없이 repository와 issuer를 mock으로 바꿔 요청 전달과 방식 선택만 검증한다
class CouponCommandServiceTest {

    private val couponCommandRepository = mock(CouponCommandRepository::class.java)
    private val luaIssuer = issuerOf(CouponIssueMethod.LUA)
    private val lockIssuer = issuerOf(CouponIssueMethod.LOCK)
    private val couponCommandService = CouponCommandService(couponCommandRepository, listOf(luaIssuer, lockIssuer))

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
    fun `구현체가 없는 방식이면 예외가 난다`() {
        assertThrows<IllegalStateException> {
            couponCommandService.issue("welcome", CouponIssueMethod.INCR, CouponIssueRequest(userId = "user-1"))
        }
    }

    private fun issuerOf(method: CouponIssueMethod): CouponIssuer =
        mock(CouponIssuer::class.java).also { given(it.method).willReturn(method) }
}
