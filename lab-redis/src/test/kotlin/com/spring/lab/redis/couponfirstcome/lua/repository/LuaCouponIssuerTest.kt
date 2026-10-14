package com.spring.lab.redis.couponfirstcome.lua.repository

import com.spring.lab.redis.RedisTestContainerConfig
import com.spring.lab.redis.couponfirstcome.common.repository.ConcurrentCouponIssuerContractTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest
import org.springframework.context.annotation.Import

// 순차·동시 요청 규칙을 모두 지키는지 계약 테스트로 검증한다
@DataRedisTest
@Import(RedisTestContainerConfig::class, LuaCouponIssuer::class)
class LuaCouponIssuerTest(
    @Autowired override val issuer: LuaCouponIssuer,
) : ConcurrentCouponIssuerContractTest()
