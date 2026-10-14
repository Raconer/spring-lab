package com.spring.lab.redis.base

import com.spring.lab.redis.RedisTestContainerConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

// 전체 컨텍스트와 실제 Redis로 쓰기 → 읽기 → 삭제 흐름을 한 번에 검증한다
@SpringBootTest
@AutoConfigureMockMvc
@Import(RedisTestContainerConfig::class)
class RedisBaseFlowTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @Test
    fun `저장한 값을 조회하고 삭제하면 더 이상 조회되지 않는다`() {
        mockMvc.put("/redis/values/flow") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"value":"value","ttlSeconds":60}"""
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/redis/values/flow").andExpect {
            status { isOk() }
            jsonPath("$.value") { value("value") }
        }

        mockMvc.delete("/redis/values/flow").andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/redis/values/flow").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `ping은 실제 Redis의 PONG을 돌려준다`() {
        mockMvc.get("/redis/ping").andExpect {
            status { isOk() }
            jsonPath("$.redis") { value("PONG") }
        }
    }
}
