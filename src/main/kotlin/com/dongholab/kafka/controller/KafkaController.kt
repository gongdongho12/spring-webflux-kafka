package com.dongholab.kafka.controller

import com.dongholab.kafka.model.AssignMessage
import com.dongholab.kafka.model.Message;
import com.dongholab.kafka.service.MessageService
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/kafka")
class KafkaController(val messageService: MessageService) {
    @PostMapping
    fun produceMessage(@RequestBody message: Mono<Message>): Mono<String> {
        return message
            .flatMap { msg: Message ->
                messageService.send(
                    msg.name,
                    msg
                )
            }
    }

    @PostMapping("/api")
    fun produceMessageApi(@RequestBody assignMessage: Mono<AssignMessage>): Mono<String> {
        return assignMessage
            .flatMap { (topic, key, data): AssignMessage ->
                messageService.sendData(
                    topic,
                    key,
                    data
                )
            }
    }

    @GetMapping
    fun consumeTransactionMessage(): Flux<ServerSentEvent<Any>> {
        return messageService.transactionReceive()
    }

    @GetMapping("/data")
    fun consumeDataMessage(): Flux<ServerSentEvent<Any>> {
        return messageService.dataReceive()
    }
}