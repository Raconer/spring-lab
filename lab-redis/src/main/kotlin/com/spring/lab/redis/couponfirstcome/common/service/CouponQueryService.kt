package com.spring.lab.redis.couponfirstcome.common.service

import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueStatusResponse
import com.spring.lab.redis.couponfirstcome.common.dto.CouponResponse
import com.spring.lab.redis.couponfirstcome.common.repository.CouponQueryRepository
import org.springframework.stereotype.Service

@Service
class CouponQueryService(
    private val couponQueryRepository: CouponQueryRepository,
) {

    // 등록되지 않은 쿠폰이면 null
    fun find(couponId: String): CouponResponse? {
        val quantity = couponQueryRepository.getQuantity(couponId) ?: return null
        val issuedCount = couponQueryRepository.countIssued(couponId)
        return CouponResponse(
            couponId = couponId,
            quantity = quantity,
            issuedCount = issuedCount,
            remaining = (quantity - issuedCount).coerceAtLeast(0),
        )
    }

    fun findIssueStatus(couponId: String, userId: String): CouponIssueStatusResponse =
        CouponIssueStatusResponse(
            couponId = couponId,
            userId = userId,
            issued = couponQueryRepository.isIssued(couponId, userId),
        )
}
