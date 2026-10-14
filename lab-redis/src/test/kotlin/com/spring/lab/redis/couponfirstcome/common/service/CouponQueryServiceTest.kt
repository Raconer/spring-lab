package com.spring.lab.redis.couponfirstcome.common.service

import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueStatusResponse
import com.spring.lab.redis.couponfirstcome.common.dto.CouponResponse
import com.spring.lab.redis.couponfirstcome.common.repository.CouponQueryRepository
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Spring과 Redis 없이 repository를 mock으로 바꿔 계산·변환 로직만 검증한다
class CouponQueryServiceTest {

    private val couponQueryRepository = mock(CouponQueryRepository::class.java)
    private val couponQueryService = CouponQueryService(couponQueryRepository)

    @Test
    fun `수량에서 발급 수를 빼서 남은 수량을 계산한다`() {
        given(couponQueryRepository.getQuantity("welcome")).willReturn(100L)
        given(couponQueryRepository.countIssued("welcome")).willReturn(30L)

        assertEquals(
            CouponResponse(couponId = "welcome", quantity = 100, issuedCount = 30, remaining = 70),
            couponQueryService.find("welcome"),
        )
    }

    @Test
    fun `등록 후 수량을 줄여 발급 수가 더 많아도 남은 수량은 0이다`() {
        given(couponQueryRepository.getQuantity("welcome")).willReturn(5L)
        given(couponQueryRepository.countIssued("welcome")).willReturn(10L)

        assertEquals(0L, couponQueryService.find("welcome")?.remaining)
    }

    @Test
    fun `등록되지 않은 쿠폰이면 null을 돌려주고 발급 수는 조회하지 않는다`() {
        given(couponQueryRepository.getQuantity("missing")).willReturn(null)

        assertNull(couponQueryService.find("missing"))
        verify(couponQueryRepository, never()).countIssued("missing")
    }

    @Test
    fun `사용자의 발급 여부를 응답으로 감싼다`() {
        given(couponQueryRepository.isIssued("welcome", "user-1")).willReturn(true)

        assertEquals(
            CouponIssueStatusResponse(couponId = "welcome", userId = "user-1", issued = true),
            couponQueryService.findIssueStatus("welcome", "user-1"),
        )
    }
}
