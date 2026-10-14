package com.spring.lab.redis.couponfirstcome.common.service

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.dto.CouponCreateRequest
import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueRequest
import com.spring.lab.redis.couponfirstcome.common.repository.CouponCommandRepository
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import org.springframework.stereotype.Service

@Service
class CouponCommandService(
    private val couponCommandRepository: CouponCommandRepository,
    issuers: List<CouponIssuer>,
) {

    // 빈으로 등록된 발급 방식 구현체를 method로 찾는다
    private val issuers: Map<CouponIssueMethod, CouponIssuer> = issuers.associateBy { it.method }

    fun create(couponId: String, request: CouponCreateRequest) {
        couponCommandRepository.create(couponId, request.quantity)
    }

    fun issue(couponId: String, method: CouponIssueMethod, request: CouponIssueRequest): CouponIssueResult {
        val issuer = issuers[method] ?: throw IllegalStateException("${method.value} 발급 방식의 구현체가 없다")
        return issuer.issue(couponId, request.userId)
    }
}
