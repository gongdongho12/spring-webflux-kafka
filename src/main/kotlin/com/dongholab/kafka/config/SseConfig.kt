package com.dongholab.kafka.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many


@Configuration
class SseConfig {
    @Bean
    fun sinksMany(): Many<Any> {
        return Sinks.many().multicast().directBestEffort()
    }
}