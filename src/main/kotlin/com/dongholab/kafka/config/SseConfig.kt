package com.dongholab.kafka.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many

@Configuration
class SseConfig {
    @Bean(name=["transaction"])
    @Primary
    fun transactionSinksMany(): Many<Any> {
        return Sinks.many().multicast().directBestEffort()
    }

    @Bean(name=["receive"])
    fun receiveSinksMany(): Many<Any> {
        return Sinks.many().multicast().directBestEffort()
    }
}