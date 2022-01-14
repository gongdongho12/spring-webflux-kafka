package com.dongholab.kafka.service

import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaSender
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.function.Consumer;



@Service
class KafkaService {
    private val log = KotlinLogging.logger {}

    @Autowired
    private lateinit var kafkaSender: KafkaSender<String, Any>

    @Autowired
    private lateinit var receiverOptions: ReceiverOptions<String, Any>

    @Autowired
    lateinit var sinksMany: Many<Any>

    private var disposable: Disposable? = null

    @PostConstruct
    fun init() {    // Consumer를 열어놓음
        disposable = KafkaReceiver.create(receiverOptions).receive()
            .doOnNext(processReceivedData())
            .doOnError { e: Throwable? ->
                log.error { "Kafka read error" }
                init() // 에러 발생 시, consumer가 종료되고 재시작할 방법이 없기 때문에 error시 재시작
            }
            .subscribe()
    }

    @PreDestroy
    fun destroy() {
        if (disposable != null) {
            disposable!!.dispose()
        }
        kafkaSender!!.close()
    }

    fun send(topic: String?, key: String, value: Any): Mono<Boolean> {
        val record: Mono<ProducerRecord<String, Any>> = Mono.just(ProducerRecord(topic, key, value))
        return kafkaSender.createOutbound()
            .send(record) // 해당 topic으로 message 전송
            .then()
            .then(Mono.just(true))
            .onErrorResume { e: Throwable? ->
                println("Kafka send error")
                Mono.just(false)
            }
    }

    private fun processReceivedData(): Consumer<ReceiverRecord<String, Any>> {
        return Consumer<ReceiverRecord<String, Any>> { r ->
            println("Kafka Consuming")
            val receivedData: Any = r.value()
            if (receivedData != null) {
                sinksMany.emitNext(r.value(), Sinks.EmitFailureHandler.FAIL_FAST) // data를 consuming할때마다 sink로 전송
            }
            r.receiverOffset().acknowledge()
        }
    }
}