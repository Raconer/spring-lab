package com.spring.lab.redis.couponfirstcome.common.controller

import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueMethod
import com.spring.lab.redis.couponfirstcome.common.domain.CouponIssueResult
import com.spring.lab.redis.couponfirstcome.common.dto.CouponCreateRequest
import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueRequest
import com.spring.lab.redis.couponfirstcome.common.service.CouponCommandService
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 쓰기 API. 조회 API는 CouponQueryController에 둔다
@RestController
@RequestMapping("/redis/coupons")
class CouponCommandController(
    private val couponCommandService: CouponCommandService,
) {

    @PutMapping("/{couponId}")
    fun create(@PathVariable couponId: String, @RequestBody request: CouponCreateRequest): ResponseEntity<Void> {
        couponCommandService.create(couponId, request)
        return ResponseEntity.noContent().build()
    }

    // method로 발급 방식을 고른다 (naive, lua, incr, lock). 실패 이유는 ProblemDetail(RFC 9457) 본문으로 알려준다
    @PostMapping("/{couponId}/issues")
    fun issue(
        @PathVariable couponId: String,
        @RequestParam(defaultValue = "lua") method: String,
        @RequestBody request: CouponIssueRequest,
    ): ResponseEntity<ProblemDetail> {
        val issueMethod = CouponIssueMethod.from(method)
            ?: return problem(HttpStatus.BAD_REQUEST, "지원하지 않는 발급 방식이다: $method")

        return when (couponCommandService.issue(couponId, issueMethod, request)) {
            CouponIssueResult.ISSUED -> ResponseEntity.status(HttpStatus.CREATED).build()
            CouponIssueResult.ALREADY_ISSUED -> problem(HttpStatus.CONFLICT, "이미 발급받은 사용자다")
            CouponIssueResult.SOLD_OUT -> problem(HttpStatus.CONFLICT, "쿠폰 수량이 모두 소진됐다")
            CouponIssueResult.NOT_FOUND -> problem(HttpStatus.NOT_FOUND, "등록되지 않은 쿠폰이다")
        }
    }

    private fun problem(status: HttpStatus, detail: String): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail))
}
