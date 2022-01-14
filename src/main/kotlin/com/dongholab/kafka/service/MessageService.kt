package com.dongholab.kafka.service

import com.dongholab.kafka.model.exception.KafkaException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks.Many
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Service
class MessageService(env: Environment) {
    private val log = KotlinLogging.logger {}

    @Autowired
    private lateinit var sinksMany: Many<Any>
    @Autowired
    private lateinit var kafkaService: KafkaService
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val topic: String = env.getProperty("kafka.topic", "test")

    fun send(key: String, value: Any): Mono<String> {
//        return Mono.just("test")
        try {
            return kafkaService.send(topic, key, objectMapper.writeValueAsString(value))
                .map {
                    if (it) {
                        "suceess send message"
                    } else {
                        "fail send message"
                    }
                }.map {

                    it
                }
        } catch (e: JsonProcessingException) {
            return Mono.error(KafkaException.SEND_ERROR)
        }
    }

    fun receive(): Flux<ServerSentEvent<Any>> {
        return sinksMany
            .asFlux()
            .publishOn(Schedulers.parallel())
            .map { message: Any ->
                ServerSentEvent.builder(
                    message
                ).build()
            } // Sink로 전송되는 message를 ServerSentEvent로 전송
            .mergeWith(ping())
            .onErrorResume { e: Throwable? -> Flux.empty() }
            .doOnCancel { println("disconnected by client") } // client 종료 시, ping으로 인지하고 cancel signal을 받음
    }

    private fun ping(): Flux<ServerSentEvent<Any>> {
        return Flux.interval(Duration.ofMillis(500))
            .map { i: Long? ->
                ServerSentEvent.builder<Any>().build()
            }
    }
}