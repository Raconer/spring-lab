package com.spring.lab.redis.base.controller

import com.spring.lab.redis.base.dto.RedisPingResponse
import com.spring.lab.redis.base.dto.RedisValueResponse
import com.spring.lab.redis.base.service.RedisQueryService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

// Redis 없이 웹 계층만 띄워 요청 매핑, JSON 변환, 상태 코드를 검증한다
@WebMvcTest(RedisQueryController::class)
class RedisQueryControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var redisQueryService: RedisQueryService

    @Test
    fun `있는 키를 GET하면 200과 값을 응답한다`() {
        given(redisQueryService.find("key")).willReturn(RedisValueResponse(key = "key", value = "value", ttlSeconds = 30))

        mockMvc.get("/redis/values/key").andExpect {
            status { isOk() }
            jsonPath("$.key") { value("key") }
            jsonPath("$.value") { value("value") }
            jsonPath("$.ttlSeconds") { value(30) }
        }
    }

    @Test
    fun `없는 키를 GET하면 404로 응답한다`() {
        given(redisQueryService.find("missing")).willReturn(null)

        mockMvc.get("/redis/values/missing").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `ping은 Redis 응답을 JSON으로 돌려준다`() {
        given(redisQueryService.ping()).willReturn(RedisPingResponse(redis = "PONG"))

        mockMvc.get("/redis/ping").andExpect {
            status { isOk() }
            jsonPath("$.redis") { value("PONG") }
        }
    }
}
