package com.spring.lab.redis.couponfirstcome.common.dto

data class CouponIssueStatusResponse(
    val couponId: String,
    val userId: String,
    val issued: Boolean,
)
