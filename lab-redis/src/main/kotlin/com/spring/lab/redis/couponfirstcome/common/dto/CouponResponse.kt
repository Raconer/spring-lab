package com.spring.lab.redis.couponfirstcome.common.dto

data class CouponResponse(
    val couponId: String,
    val quantity: Long,
    val issuedCount: Long,
    val remaining: Long,
)
