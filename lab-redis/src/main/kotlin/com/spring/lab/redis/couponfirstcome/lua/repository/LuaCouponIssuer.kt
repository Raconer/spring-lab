package com.spring.lab.redis.couponfirstcome.lua.repository

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import com.spring.lab.redis.couponfirstcome.common.repository.CouponRedisKeys
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository

// 수량 확인, 중복 확인, 발급을 Lua 스크립트 하나로 실행한다
// Redis는 스크립트를 실행하는 동안 다른 명령을 처리하지 않으므로 동시 요청에도 수량을 넘기지 않는다
@Repository
class LuaCouponIssuer(
    private val redisTemplate: StringRedisTemplate,
) : CouponIssuer {

    override val method = CouponIssueMethod.LUA

    private val issueScript: RedisScript<Long> =
        RedisScript.of(ClassPathResource("redis/couponfirstcome/lua/issue.lua"), Long::class.javaObjectType)

    override fun issue(couponId: String, userId: String): CouponIssueResult {
        val keys = listOf(CouponRedisKeys.quantity(couponId), CouponRedisKeys.issuedUsers(couponId))
        val code = redisTemplate.execute(issueScript, keys, userId)
            ?: throw IllegalStateException("발급 스크립트가 결과를 돌려주지 않았다")
        return toResult(code)
    }

    // issue.lua의 반환 코드를 결과로 바꾼다
    private fun toResult(code: Long): CouponIssueResult =
        when (code) {
            0L -> CouponIssueResult.ISSUED
            1L -> CouponIssueResult.ALREADY_ISSUED
            2L -> CouponIssueResult.SOLD_OUT
            3L -> CouponIssueResult.NOT_FOUND
            else -> throw IllegalStateException("알 수 없는 발급 결과 코드: $code")
        }
}
