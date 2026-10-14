package com.spring.lab.redis.couponfirstcome.common.repository

// 모든 발급 방식과 Command/Query repository가 같은 키를 쓰도록 키 규칙을 한 곳에 둔다
// {couponId} 해시 태그로 키를 같은 슬롯에 둬서 Redis Cluster에서도 Lua 스크립트를 실행할 수 있게 한다
object CouponRedisKeys {

    // 쿠폰 총 수량 (String)
    fun quantity(couponId: String): String = "coupon:{$couponId}:quantity"

    // 발급받은 사용자 ID 목록 (Set). 모든 방식이 발급 결과를 여기에 기록한다
    fun issuedUsers(couponId: String): String = "coupon:{$couponId}:issued-users"

    // incr 방식의 발급 순번 카운터 (String)
    fun issueCount(couponId: String): String = "coupon:{$couponId}:issue-count"

    // lock 방식의 분산 락
    fun issueLock(couponId: String): String = "coupon:{$couponId}:issue-lock"
}
