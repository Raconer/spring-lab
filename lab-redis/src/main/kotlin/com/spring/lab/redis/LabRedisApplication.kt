package com.spring.lab.redis

import com.spring.lab.core.config.LabCoreConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(LabCoreConfiguration::class)
class LabRedisApplication

fun main(args: Array<String>) {
    runApplication<LabRedisApplication>(*args)
}
