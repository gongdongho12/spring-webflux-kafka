package com.dongholab.kafka.config

import com.dongholab.kafka.constants.KafkaConstants
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import java.time.Duration


@Configuration
class KafkaConfig() {
    @Bean("kafkaSender")
    fun kafkaSender(): KafkaSender<String, Any> {
        val senderOptions: SenderOptions<String, Any> = SenderOptions.create(
            producerProps
        )
        senderOptions.scheduler(Schedulers.parallel())
        senderOptions.closeTimeout(Duration.ofSeconds(20))
        return KafkaSender.create(senderOptions)
    }

    @Bean(name=arrayOf("api"))
    fun apiReceiverOptions(): ReceiverOptions<String, Any> {
        return ReceiverOptions.create<String, Any>(consumerProps)
            .subscription(KafkaConstants.transactionTopics)
    }

    @Bean(name=arrayOf("data"))
    fun dataReceiverOptions(): ReceiverOptions<String, Any> {
        return ReceiverOptions.create<String, Any>(consumerProps)
            .subscription(KafkaConstants.receiveTopics)
    }

    // 프로듀서 설정
    private val producerProps: Map<String, Any> = mutableMapOf<String, Any>().run {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.host)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000)
        this
    }

    // 컨슈머 설정
    private val consumerProps: Map<String, Any> = mutableMapOf<String, Any>().run {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.host)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        put(ConsumerConfig.GROUP_ID_CONFIG, KafkaConstants.groupId)
        this
    }
}