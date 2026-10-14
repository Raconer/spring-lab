package com.spring.lab.redis.couponfirstcome.common.controller

import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueStatusResponse
import com.spring.lab.redis.couponfirstcome.common.dto.CouponResponse
import com.spring.lab.redis.couponfirstcome.common.service.CouponQueryService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

// Redis 없이 웹 계층만 띄워 요청 매핑, JSON 변환, 상태 코드를 검증한다
@WebMvcTest(CouponQueryController::class)
class CouponQueryControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var couponQueryService: CouponQueryService

    @Test
    fun `등록된 쿠폰을 GET하면 200과 수량 정보를 응답한다`() {
        given(couponQueryService.find("welcome"))
            .willReturn(CouponResponse(couponId = "welcome", quantity = 100, issuedCount = 30, remaining = 70))

        mockMvc.get("/redis/coupons/welcome").andExpect {
            status { isOk() }
            jsonPath("$.quantity") { value(100) }
            jsonPath("$.issuedCount") { value(30) }
            jsonPath("$.remaining") { value(70) }
        }
    }

    @Test
    fun `등록되지 않은 쿠폰을 GET하면 404로 응답한다`() {
        given(couponQueryService.find("missing")).willReturn(null)

        mockMvc.get("/redis/coupons/missing").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `사용자의 발급 여부를 응답한다`() {
        given(couponQueryService.findIssueStatus("welcome", "user-1"))
            .willReturn(CouponIssueStatusResponse(couponId = "welcome", userId = "user-1", issued = true))

        mockMvc.get("/redis/coupons/welcome/issues/user-1").andExpect {
            status { isOk() }
            jsonPath("$.issued") { value(true) }
        }
    }
}
