package com.dongholab.kafka.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.time.Duration


@Configuration
class KafkaConfig(val env: Environment) {
    private val host: String = env.getProperty("kafka.host", "localhost:9092")
    private val topic: String = env.getProperty("kafka.topic", "test")
    private val groupId: String = env.getProperty("kafka.groupId", "dongholab")

    @Bean("kafkaSender")
    fun kafkaSender(): KafkaSender<String, Any> {
        val senderOptions: SenderOptions<String, Any> = SenderOptions.create(
            producerProps
        )
        senderOptions.scheduler(Schedulers.parallel())
        senderOptions.closeTimeout(Duration.ofSeconds(5))
        return KafkaSender.create(senderOptions)
    }

    @Bean
    fun receiverOptions(): ReceiverOptions<String, Any> {
        return ReceiverOptions.create<String, Any>(consumerProps)
            .subscription(setOf(topic))
    }

    // 프로듀서 설정
    private val producerProps: Map<String, Any> = mutableMapOf<String, Any>().run {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, host)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000)
        this
    }

    // 컨슈머 설정
    private val consumerProps: Map<String, Any> = mutableMapOf<String, Any>().run {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        this
    }
}