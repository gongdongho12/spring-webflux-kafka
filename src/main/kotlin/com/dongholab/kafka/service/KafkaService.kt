package com.dongholab.kafka.service

import com.dongholab.kafka.constants.KafkaConstants
import com.dongholab.kafka.model.ApiMessage
import com.dongholab.kafka.model.DataMessage
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaSender
import java.io.StringReader
import java.lang.Exception
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.function.Consumer;

@Service
class KafkaService {
    private val log = KotlinLogging.logger {}

    @Autowired
    private lateinit var kafkaSender: KafkaSender<String, Any>

    @Autowired
    @Qualifier("api")
    private lateinit var apiReceiverOptions: ReceiverOptions<String, Any>

    @Autowired
    @Qualifier("data")
    private lateinit var dataReceiverOptions: ReceiverOptions<String, Any>

    @Autowired
    @Qualifier("transaction")
    lateinit var transactionSinksMany: Many<Any>

    @Autowired
    @Qualifier("receive")
    lateinit var receiveSinksMany: Many<Any>
    
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var apiClient: WebClient

    @Autowired
    private lateinit var gson: Gson

    private var apiDisposable: Disposable? = null
    private var dataDisposable: Disposable? = null

    @PostConstruct
    fun initAPI() {    // Consumer를 열어놓음
        apiDisposable = KafkaReceiver.create(apiReceiverOptions).receive()
            .doOnNext(apiProcessReceivedData())
            .doOnError { e: Throwable? ->
                log.error { "Kafka api read error" }
                initAPI() // 에러 발생 시, consumer가 종료되고 재시작할 방법이 없기 때문에 error시 재시작
            }
            .subscribe()
    }

    @PostConstruct
    fun initData() {    // Consumer를 열어놓음
        dataDisposable = KafkaReceiver.create(dataReceiverOptions).receive()
            .doOnNext(dataProcessReceivedData())
            .doOnError { e: Throwable? ->
                log.error { "Kafka data read error" }
                initData() // 에러 발생 시, consumer가 종료되고 재시작할 방법이 없기 때문에 error시 재시작
            }
            .subscribe()
    }

    @PreDestroy
    fun destroy() {
        if (apiDisposable != null) {
            apiDisposable!!.dispose()
        }
        if (dataDisposable != null) {
            dataDisposable!!.dispose()
        }
        kafkaSender!!.close()
    }

    fun objectToString(value: Any) = objectMapper.writeValueAsString(value)

    fun stringToObjectByKey(key: String, value: String): Any {
//        val assignValue = value.replace("\\\"", "\"")
//        while (assignValue.contains("\\\"")) {
//            assignValue = assignValue.replace("\\\"", "\'")
//        }
//        val reader: JsonReader = JsonReader(StringReader(assignValue))
//        reader.setLenient(true);
        try {
            return when (key) {
                KafkaConstants.API_KEY -> objectMapper.readValue(value, ApiMessage::class.java)
                KafkaConstants.DATA_KEY -> objectMapper.readValue(value, DataMessage::class.java)
                else -> value
            }
        } catch (e: Exception) {
            throw Exception()
        }
    }

    fun send(topic: String?, key: String, value: Any): Mono<Boolean> {
        val record: Mono<ProducerRecord<String, Any>> = Mono.just(ProducerRecord(topic, key, value))
        return kafkaSender.createOutbound()
            .send(record) // 해당 topic으로 message 전송
            .then()
            .then(Mono.just(true))
            .onErrorResume { e: Throwable? ->
                log.error("Kafka send error")
                Mono.just(false)
            }
    }

    fun defaultQuery(
        init: (MultiValueMap<String, Any>.() -> Unit)? = null
    ): MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
        LinkedMultiValueMap<String, Any>()
            .apply {
                init?.let {
                    it()
                }
            }.map {
                val key = it.key
                it.value.map {
                    add(key, it?.toString() ?: "")
                }
            }
    }

    fun sendAPI(path: String, method: HttpMethod = HttpMethod.GET, defaultQueryValue: (MultiValueMap<String, Any>.() -> Unit)? = null): WebClient.RequestBodySpec {
        val url = UriComponentsBuilder
            .fromUriString(path)
            .let { builder ->
                defaultQueryValue?.let {
                    builder.queryParams(
                        defaultQuery {
                            defaultQueryValue()
                        }
                    )
                }?: builder
            }
            .build().toString()
        return apiClient.method(method).uri(url)
    }

    fun consume(topic: String, key: String, value: Any): Mono<Any> {
        return when (topic) {
            "error" -> {
                log.error("error: ${value}")
                Mono.just(true)
            }
            else -> {
                when (value) {
                    is DataMessage -> {
                        val (api, data) = value
                        log.info { "api: $api" }
                        log.info { "save function: $data" }
                        Mono.just(true)
                    }
                    is ApiMessage -> {
                        val (host, method) = value
                        sendAPI(host, HttpMethod.valueOf(method)).retrieve().bodyToMono(String::class.java).publishOn(Schedulers.boundedElastic()).map {
                            val assignData = it.substring(0, 150).trim()
                            log.info { "host: ${host} / data: ${assignData}" }
                            assignData
                        }.map {
                            send(KafkaConstants.receiveTopics.first(), KafkaConstants.DATA_KEY, objectToString(DataMessage(
                                value,
                                it
                            ))).subscribeOn(Schedulers.single()).log().subscribe()
                            true
                        }
                    }
                    else -> {
                        Mono.just(true)
                    }
                }
            }
        }
    }

    private fun apiProcessReceivedData(): Consumer<ReceiverRecord<String, Any>> {
        return Consumer<ReceiverRecord<String, Any>> { r ->
            println("Kafka Consuming Transaction")
            val receivedData: Any = r.value()
            if (receivedData != null) {
                val topic = r.topic()
                try {
                    val key = r.key()
                    val value: Any = stringToObjectByKey(key, receivedData as String)
                    log.info { "Kafka topic: ${topic} data: ${receivedData}" }

                    consume(topic, key, value).onErrorResume {
                        when (topic) {
                            "default" -> {
                                send("retry1", key, value)
                            }
                            "retry1" -> {
                                send("retry2", key, value)
                            }
                            "retry2" -> {
                                send("retry3", key, value)
                            }
                            "retry3" -> {
                                send("error", key, value)
                            }
                            else -> { Mono.empty() }
                        }
                    }.subscribe()
                    transactionSinksMany.emitNext("topic: $topic / data: $receivedData", Sinks.EmitFailureHandler.FAIL_FAST)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // data를 consuming할때마다 sink로 전송
            }
            r.receiverOffset().acknowledge()
        }
    }

    private fun dataProcessReceivedData(): Consumer<ReceiverRecord<String, Any>> {
        return Consumer<ReceiverRecord<String, Any>> { r ->
            println("Kafka Consuming Receive")
            val receivedData: Any = r.value()
            if (receivedData != null) {
                val topic = r.topic()
                try {
                    val key = r.key()
                    val value: Any = stringToObjectByKey(key, receivedData as String)
                    log.info { "Kafka topic: ${topic} data: ${receivedData}" }

                    consume(topic, key, value).subscribe()
                    receiveSinksMany.emitNext("topic: $topic / data: $receivedData", Sinks.EmitFailureHandler.FAIL_FAST)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            r.receiverOffset().acknowledge()
        }
    }
}