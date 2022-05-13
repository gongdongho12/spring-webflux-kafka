package com.dongholab.kafka.config

import com.google.gson.GsonBuilder
import com.dongholab.kafka.config.gson.DateDeserializer
import com.dongholab.kafka.config.gson.DateSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class GsonConfiguration {
    @Bean("gson")
    fun gson() = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateSerializer())
        .registerTypeAdapter(Date::class.java, DateDeserializer())
        .create()
}