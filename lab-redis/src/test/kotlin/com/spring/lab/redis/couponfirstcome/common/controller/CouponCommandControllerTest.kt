package com.spring.lab.redis.couponfirstcome.common.controller

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.dto.CouponCreateRequest
import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueRequest
import com.spring.lab.redis.couponfirstcome.common.service.CouponCommandService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

// Redis 없이 웹 계층만 띄워 요청 매핑, 발급 방식 선택, 발급 결과별 상태 코드를 검증한다
@WebMvcTest(CouponCommandController::class)
class CouponCommandControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var couponCommandService: CouponCommandService

    private val request = CouponIssueRequest(userId = "user-1")

    @Test
    fun `쿠폰을 등록하면 204로 응답한다`() {
        mockMvc.put("/redis/coupons/welcome") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"quantity":100}"""
        }.andExpect {
            status { isNoContent() }
        }

        verify(couponCommandService).create("welcome", CouponCreateRequest(quantity = 100))
    }

    @Test
    fun `method를 주면 그 방식으로 발급한다`() {
        given(couponCommandService.issue("welcome", CouponIssueMethod.LOCK, request)).willReturn(CouponIssueResult.ISSUED)

        issue(methodParam = "lock").andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `method가 없으면 lua 방식으로 발급한다`() {
        given(couponCommandService.issue("welcome", CouponIssueMethod.LUA, request)).willReturn(CouponIssueResult.ISSUED)

        issue(methodParam = null).andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `지원하지 않는 method면 400으로 응답하고 발급하지 않는다`() {
        issue(methodParam = "unknown").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("지원하지 않는 발급 방식이다: unknown") }
        }

        verifyNoInteractions(couponCommandService)
    }

    @Test
    fun `이미 발급받았으면 409와 이유를 응답한다`() {
        given(couponCommandService.issue("welcome", CouponIssueMethod.LUA, request)).willReturn(CouponIssueResult.ALREADY_ISSUED)

        issue(methodParam = "lua").andExpect {
            status { isConflict() }
            jsonPath("$.detail") { value("이미 발급받은 사용자다") }
        }
    }

    @Test
    fun `수량이 소진됐으면 409와 이유를 응답한다`() {
        given(couponCommandService.issue("welcome", CouponIssueMethod.LUA, request)).willReturn(CouponIssueResult.SOLD_OUT)

        issue(methodParam = "lua").andExpect {
            status { isConflict() }
            jsonPath("$.detail") { value("쿠폰 수량이 모두 소진됐다") }
        }
    }

    @Test
    fun `등록되지 않은 쿠폰이면 404로 응답한다`() {
        given(couponCommandService.issue("welcome", CouponIssueMethod.LUA, request)).willReturn(CouponIssueResult.NOT_FOUND)

        issue(methodParam = "lua").andExpect {
            status { isNotFound() }
        }
    }

    private fun issue(methodParam: String?) =
        mockMvc.post("/redis/coupons/welcome/issues") {
            methodParam?.let { param("method", it) }
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId":"user-1"}"""
        }
}
