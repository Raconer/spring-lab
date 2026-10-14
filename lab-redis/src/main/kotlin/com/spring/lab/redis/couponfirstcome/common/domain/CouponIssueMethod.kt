package com.spring.lab.redis.couponfirstcome.common.domain

// 발급 방식. value는 API의 method 파라미터 값이고, 같은 이름의 하위 패키지에 구현이 있다
enum class CouponIssueMethod(val value: String) {
    // 확인 후 발급. 원자성이 없어 동시 요청에서 초과 발급된다 (비교 기준선)
    NAIVE("naive"),

    // 확인과 발급을 Lua 스크립트 하나로 원자적으로 실행한다
    LUA("lua"),

    // INCR로 순번을 받아 수량과 비교한다
    INCR("incr"),

    // Redisson 분산 락 안에서 확인 후 발급한다
    LOCK("lock"),
    ;

    companion object {
        fun from(value: String): CouponIssueMethod? = entries.find { it.value == value }
    }
}
