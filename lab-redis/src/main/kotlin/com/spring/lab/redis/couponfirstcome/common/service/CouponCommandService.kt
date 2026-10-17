package com.spring.lab.redis.couponfirstcome.common.service

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.dto.CouponCreateRequest
import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueRequest
import com.spring.lab.redis.couponfirstcome.common.repository.CouponCommandRepository
import com.spring.lab.redis.couponfirstcome.common.repository.CouponIssuer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service

@Service
class CouponCommandService(
    private val couponCommandRepository: CouponCommandRepository,
    issuers: List<CouponIssuer>,
    private val meterRegistry: MeterRegistry,
) {

    // 빈으로 등록된 발급 방식 구현체를 method로 찾는다
    private val issuers: Map<CouponIssueMethod, CouponIssuer> = issuers.associateBy { it.method }

    fun create(couponId: String, request: CouponCreateRequest) {
        couponCommandRepository.create(couponId, request.quantity)
    }

    fun issue(couponId: String, method: CouponIssueMethod, request: CouponIssueRequest): CouponIssueResult {
        val issuer = issuers[method] ?: throw IllegalStateException("${method.value} 발급 방식의 구현체가 없다")
        val sample = Timer.start(meterRegistry)
        val result = issuer.issue(couponId, request.userId)
        sample.stop(issueTimer(method, result))
        return result
    }

    // 방식·결과별 발급 수와 처리 시간. Prometheus에서는 coupon_issue_seconds_* 로 보인다
    private fun issueTimer(method: CouponIssueMethod, result: CouponIssueResult): Timer =
        Timer.builder("coupon.issue")
            .description("쿠폰 선착순 발급 요청 처리 시간")
            .tag("method", method.value)
            .tag("result", result.name)
            .publishPercentileHistogram()
            .register(meterRegistry)
}
