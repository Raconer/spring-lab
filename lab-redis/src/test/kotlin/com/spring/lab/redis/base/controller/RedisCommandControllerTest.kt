package com.spring.lab.redis.base.controller

import com.spring.lab.redis.base.dto.RedisValueSaveRequest
import com.spring.lab.redis.base.service.RedisCommandService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.put

// Redis 없이 웹 계층만 띄워 요청 매핑, JSON 변환, 상태 코드를 검증한다
@WebMvcTest(RedisCommandController::class)
class RedisCommandControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    private lateinit var redisCommandService: RedisCommandService

    @Test
    fun `PUT 요청 본문을 그대로 넘기고 204로 응답한다`() {
        mockMvc.put("/redis/values/key") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"value":"value","ttlSeconds":60}"""
        }.andExpect {
            status { isNoContent() }
        }

        verify(redisCommandService).save("key", RedisValueSaveRequest(value = "value", ttlSeconds = 60))
    }

    @Test
    fun `PUT 본문에 ttlSeconds가 없으면 null로 넘긴다`() {
        mockMvc.put("/redis/values/key") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"value":"value"}"""
        }.andExpect {
            status { isNoContent() }
        }

        verify(redisCommandService).save("key", RedisValueSaveRequest(value = "value", ttlSeconds = null))
    }

    @Test
    fun `있는 키를 DELETE하면 204로 응답한다`() {
        given(redisCommandService.delete("key")).willReturn(true)

        mockMvc.delete("/redis/values/key").andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `없는 키를 DELETE하면 404로 응답한다`() {
        given(redisCommandService.delete("missing")).willReturn(false)

        mockMvc.delete("/redis/values/missing").andExpect {
            status { isNotFound() }
        }
    }
}
