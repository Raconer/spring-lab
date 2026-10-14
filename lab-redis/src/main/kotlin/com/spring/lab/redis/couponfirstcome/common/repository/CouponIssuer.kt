package com.spring.lab.redis.couponfirstcome.common.repository

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult

// 발급 방식마다 하위 패키지(naive, lua, incr, lock)에 구현체를 하나씩 둔다
// 발급에 성공하면 CouponRedisKeys.issuedUsers에 사용자를 기록해야 한다. 조회는 이 Set을 기준으로 한다
interface CouponIssuer {

    val method: CouponIssueMethod

    fun issue(couponId: String, userId: String): CouponIssueResult
}
