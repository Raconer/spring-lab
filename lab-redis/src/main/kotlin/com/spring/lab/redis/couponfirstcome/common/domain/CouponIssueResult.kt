package com.spring.lab.redis.couponfirstcome.common.domain

// 발급 요청 결과. 모든 발급 방식이 같은 결과를 돌려준다
enum class CouponIssueResult {
    ISSUED,
    ALREADY_ISSUED,
    SOLD_OUT,
    NOT_FOUND,
}
