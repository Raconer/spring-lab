package com.spring.lab.redis.couponfirstcome

import com.spring.lab.redis.RedisTestContainerConfig
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

// 전체 컨텍스트와 실제 Redis로 등록 → 발급 → 중복·소진 거절 → 조회 흐름을 발급 방식마다 검증한다
@SpringBootTest
@AutoConfigureMockMvc
@Import(RedisTestContainerConfig::class)
class CouponFirstComeFlowTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @ParameterizedTest
    @ValueSource(strings = ["naive", "lua", "incr", "lock"])
    fun `수량 1개 쿠폰은 첫 사용자에게만 발급된다`(method: String) {
        val couponId = "flow-$method"
        mockMvc.put("/redis/coupons/$couponId") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"quantity":1}"""
        }.andExpect {
            status { isNoContent() }
        }

        issue(couponId, method, "user-1").andExpect { status { isCreated() } }
        issue(couponId, method, "user-1").andExpect { status { isConflict() } }
        issue(couponId, method, "user-2").andExpect { status { isConflict() } }

        mockMvc.get("/redis/coupons/$couponId").andExpect {
            status { isOk() }
            jsonPath("$.issuedCount") { value(1) }
            jsonPath("$.remaining") { value(0) }
        }
        mockMvc.get("/redis/coupons/$couponId/issues/user-1").andExpect {
            jsonPath("$.issued") { value(true) }
        }
        mockMvc.get("/redis/coupons/$couponId/issues/user-2").andExpect {
            jsonPath("$.issued") { value(false) }
        }
    }

    private fun issue(couponId: String, method: String, userId: String) =
        mockMvc.post("/redis/coupons/$couponId/issues") {
            param("method", method)
            contentType = MediaType.APPLICATION_JSON
            content = """{"userId":"$userId"}"""
        }
}
