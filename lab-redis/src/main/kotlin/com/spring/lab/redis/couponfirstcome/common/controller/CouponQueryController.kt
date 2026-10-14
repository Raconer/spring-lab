package com.spring.lab.redis.couponfirstcome.common.controller

import com.spring.lab.redis.couponfirstcome.common.dto.CouponIssueStatusResponse
import com.spring.lab.redis.couponfirstcome.common.dto.CouponResponse
import com.spring.lab.redis.couponfirstcome.common.service.CouponQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 조회 API. 쓰기 API는 CouponCommandController에 둔다
@RestController
@RequestMapping("/redis/coupons")
class CouponQueryController(
    private val couponQueryService: CouponQueryService,
) {

    @GetMapping("/{couponId}")
    fun find(@PathVariable couponId: String): ResponseEntity<CouponResponse> =
        couponQueryService.find(couponId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @GetMapping("/{couponId}/issues/{userId}")
    fun findIssueStatus(@PathVariable couponId: String, @PathVariable userId: String): CouponIssueStatusResponse =
        couponQueryService.findIssueStatus(couponId, userId)
}
